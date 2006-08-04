/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.security.network.protocol;

//  Removed to allow building Geronimo on non-Sun JDK.  When this test case is updated and enabled
//  again it should be updated so that it is not specific to the Sun JDK.
// import com.sun.security.auth.login.ConfigFile;
import org.activeio.AcceptListener;
import org.activeio.AsyncChannelServer;
import org.activeio.Channel;
import org.activeio.Packet;
import org.activeio.RequestChannel;
import org.activeio.RequestListener;
import org.activeio.adapter.AsyncChannelToClientRequestChannel;
import org.activeio.adapter.AsyncChannelToServerRequestChannel;
import org.activeio.adapter.AsyncToSyncChannel;
import org.activeio.adapter.SyncToAsyncChannel;
import org.activeio.adapter.SyncToAsyncChannelServer;
import org.activeio.filter.PacketAggregatingAsyncChannel;
import org.activeio.net.SocketSyncChannelFactory;
import org.activeio.packet.ByteArrayPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.security.AbstractTest;
import org.apache.geronimo.security.jaas.JaasLoginModuleUse;
import org.apache.geronimo.security.jaas.LoginModuleGBean;
import org.apache.geronimo.security.realm.GenericSecurityRealm;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;


/**
 * @version $Rev$ $Date$
 */
public class SubjectCarryingProtocolTest extends AbstractTest implements RequestListener {
    
    final static private Log log = LogFactory.getLog(SubjectCarryingProtocolTest.class);
    
    private File basedir = new File(System.getProperty("basedir"));
    
    protected AbstractName testCE;
    protected AbstractName testRealm;

    private Subject clientSubject;
    private Subject serverSubject;
    private URI serverURI;
    private AsyncChannelServer server;

    public void testNothing() throws Exception {
    }

    /*
     * Enable this test again once its working.
     */
    public void disabledtest() throws Exception {

        SocketSyncChannelFactory factory = new SocketSyncChannelFactory();
        final RequestChannel channel =
            new AsyncChannelToClientRequestChannel(
                AsyncToSyncChannel.adapt(
                    new SubjectCarryingChannel(
                        new PacketAggregatingAsyncChannel(
                            SyncToAsyncChannel.adapt(
                                 factory.openSyncChannel(serverURI))))));
        try {
            channel.start();
	        Subject.doAs(clientSubject, new PrivilegedExceptionAction() {
	            public Object run() throws Exception {

	                Subject subject = Subject.getSubject(AccessController.getContext());
	                String p = subject.getPrincipals().iterator().next().toString();
	                log.info("Sending request as: "+p);

                    Packet request = new ByteArrayPacket("whoami".getBytes());
                    Packet response = channel.request(request, 1000*5*1000);

                    assertNotNull(response);
                    assertEquals( p, new String(response.sliceAsBytes()) );
                    return null;
	            }
	        });
        } finally {
            channel.dispose();
        }
    }


    public void setUp() throws Exception {
        needServerInfo = true;
        super.setUp();

        GBeanData gbean;

        gbean = buildGBeanData    ("name", "PropertiesLoginModule", LoginModuleGBean.getGBeanInfo());
        testCE = gbean.getAbstractName();
        gbean.setAttribute("loginModuleClass", "org.apache.geronimo.security.realm.providers.PropertiesFileLoginModule");
        gbean.setAttribute("serverSide", new Boolean(true));
        Properties props = new Properties();
        props.put("usersURI", new File(basedir, "src/test-data/data/users.properties").toURI().toString());
        props.put("groupsURI", new File(basedir, "src/test-data/data/groups.properties").toURI().toString());
        gbean.setAttribute("options", props);
        gbean.setAttribute("loginDomainName", "PropertiesDomain");
        kernel.loadGBean(gbean, LoginModuleGBean.class.getClassLoader());

        gbean = buildGBeanData("name", "PropertiesLoginModuleUse", JaasLoginModuleUse.getGBeanInfo());
        AbstractName testUseName = gbean.getAbstractName();
        gbean.setAttribute("controlFlag", "REQUIRED");
        gbean.setReferencePattern("LoginModule", testCE);
        kernel.loadGBean(gbean, JaasLoginModuleUse.class.getClassLoader());

        gbean = buildGBeanData("name", "PropertiesSecurityRealm", GenericSecurityRealm.getGBeanInfo());
        testRealm = gbean.getAbstractName();
        gbean.setAttribute("realmName", "properties-realm");
        gbean.setReferencePattern("LoginModuleConfiguration", testUseName);
        gbean.setReferencePattern("ServerInfo", serverInfo);
        kernel.loadGBean(gbean, GenericSecurityRealm.class.getClassLoader());

        kernel.startGBean(testCE);
        kernel.startGBean(testUseName);
        kernel.startGBean(testRealm);

        LoginContext context = new LoginContext("properties", new AbstractTest.UsernamePasswordCallback("alan", "starcraft"));
        context.login();
        clientSubject = context.getSubject();

        context = new LoginContext("properties", new AbstractTest.UsernamePasswordCallback("izumi", "violin"));
        context.login();
        serverSubject = context.getSubject();

        SocketSyncChannelFactory factory = new SocketSyncChannelFactory();
        server = new SyncToAsyncChannelServer(
                factory.bindSyncChannel(new URI("tcp://localhost:0")));

        server.setAcceptListener(new AcceptListener() {
            public void onAccept(Channel channel) {
                RequestChannel requestChannel=null;
                try {

                    requestChannel =
                        new AsyncChannelToServerRequestChannel(
	                        new SubjectCarryingChannel(
	                            new PacketAggregatingAsyncChannel(
	                                SyncToAsyncChannel.adapt(channel))));

                    requestChannel.setRequestListener(SubjectCarryingProtocolTest.this);
                    requestChannel.start();

                } catch (IOException e) {
                    log.info("Failed to accept connection.", e);
                    if( requestChannel!=null )
                        requestChannel.dispose();
                    else
                        channel.dispose();
                }
            }
            public void onAcceptError(IOException error) {
                log.info("Accept Failed: "+error);
            }
        });

        server.start();
        serverURI = server.getConnectURI();

    }

    public void tearDown() throws Exception {
        server.dispose();

        kernel.stopGBean(testRealm);
        kernel.stopGBean(testCE);
        kernel.stopGBean(serverInfo);
        kernel.unloadGBean(testCE);
        kernel.unloadGBean(testRealm);
        kernel.unloadGBean(serverInfo);
        super.tearDown();
//        Configuration.setConfiguration(new ConfigFile());
    }

    public Packet onRequest(Packet packet) {

        String p="";
        try {
            SubjectContext ctx = (SubjectContext)packet.narrow(SubjectContext.class);
	        Subject subject = ctx.getSubject();
	        p = subject.getPrincipals().iterator().next().toString();
	        log.info("Received request as: "+p);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return new ByteArrayPacket(p.getBytes());
    }

    public void onRquestError(IOException arg) {
    }


}
