/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2004 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */
package org.apache.geronimo.security.remoting.jmx;

import javax.management.ObjectName;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.geronimo.gbean.jmx.GBeanMBean;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.jmx.MBeanProxyFactory;
import org.apache.geronimo.remoting.transport.TransportLoader;
import org.apache.geronimo.security.IdentificationPrincipal;
import org.apache.geronimo.security.RealmPrincipal;
import org.apache.geronimo.security.jaas.LoginServiceMBean;


/**
 * @version $Revision: 1.1 $ $Date: 2004/02/17 00:05:40 $
 */
public class RemoteLoginTest extends TestCase {
    Kernel kernel;
    ObjectName loginService;
    ObjectName kerberosRealm;
    ObjectName subsystemRouter;
    ObjectName secureSubsystemRouter;
    ObjectName asyncTransport;
    ObjectName saslTransport;
    ObjectName gssapiTransport;
    ObjectName jmxRouter;
    ObjectName secureJmxRouter;
    ObjectName serverStub;
    LoginServiceMBean asyncRemoteProxy;
    LoginServiceMBean saslRemoteProxy;
    LoginServiceMBean gssapiRemoteProxy;


    public void testLogin() throws Exception {
        LoginContext context = new LoginContext("FOO", new UsernamePasswordCallback("alan", "starcraft"));

        context.login();
        Subject subject = context.getSubject();

        assertTrue("expected non-null subject", subject != null);
        assertTrue("subject should have one remote principal", subject.getPrincipals(IdentificationPrincipal.class).size() == 1);
        IdentificationPrincipal principal = (IdentificationPrincipal) subject.getPrincipals(IdentificationPrincipal.class).iterator().next();
        assertTrue("id of principal should be non-zero", principal.getId().longValue() != 0);
        assertTrue("subject should have five principals", subject.getPrincipals().size() == 5);
        assertTrue("subject should have two realm principal", subject.getPrincipals(RealmPrincipal.class).size() == 2);

        context.logout();
    }

    public void setUp() throws Exception {
        kernel = new Kernel("test.kernel", "simple.geronimo.test");
        kernel.boot();

        GBeanMBean gbean;

        // Create all the parts
        gbean = new GBeanMBean("org.apache.geronimo.security.jaas.LoginService");
        loginService = new ObjectName("geronimo.security:type=LoginService");
        gbean.setReferencePatterns("Realms", Collections.singleton(new ObjectName("geronimo.security:type=SecurityRealm,*")));
        gbean.setAttribute("ReclaimPeriod", new Long(100));
        gbean.setAttribute("Kernel", kernel);
        gbean.setAttribute("Algorithm", "HmacSHA1");
        gbean.setAttribute("Password", "secret");
        kernel.loadGBean(loginService, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.security.realm.providers.PropertiesFileSecurityRealm");
        kerberosRealm = new ObjectName("geronimo.security:type=SecurityRealm,realm=properties-realm");
        gbean.setAttribute("RealmName", "properties-realm");
        gbean.setAttribute("MaxLoginModuleAge", new Long(1 * 1000));
        gbean.setAttribute("UsersURI", (new File(new File("."), "src/test-data/data/users.properties")).toURI());
        gbean.setAttribute("GroupsURI", (new File(new File("."), "src/test-data/data/groups.properties")).toURI());
        kernel.loadGBean(kerberosRealm, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.router.SubsystemRouter");
        subsystemRouter = new ObjectName("geronimo.remoting:router=SubsystemRouter");
        kernel.loadGBean(subsystemRouter, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.transport.TransportLoader");
        gbean.setAttribute("BindURI", new URI("async://0.0.0.0:0"));
        gbean.setReferencePatterns("Router", Collections.singleton(subsystemRouter));
        asyncTransport = new ObjectName("geronimo.remoting:transport=async");
        kernel.loadGBean(asyncTransport, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.router.JMXRouter");
        gbean.setReferencePatterns("SubsystemRouter", Collections.singleton(subsystemRouter));
        jmxRouter = new ObjectName("geronimo.remoting:router=JMXRouter");
        kernel.loadGBean(jmxRouter, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.router.SubsystemRouter");
        secureSubsystemRouter = new ObjectName("geronimo.remoting:router=SubsystemRouter,type=secure");
        kernel.loadGBean(secureSubsystemRouter, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.transport.TransportLoader");
        gbean.setAttribute("BindURI", new URI("async://0.0.0.0:4242"));
        gbean.setReferencePatterns("Router", Collections.singleton(secureSubsystemRouter));
        saslTransport = new ObjectName("geronimo.remoting:transport=async,subprotocol=sasl");
        kernel.loadGBean(saslTransport, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.transport.TransportLoader");
        gbean.setAttribute("BindURI", new URI("async://0.0.0.0:4243"));
        gbean.setReferencePatterns("Router", Collections.singleton(secureSubsystemRouter));
        gssapiTransport = new ObjectName("geronimo.remoting:transport=async,subprotocol=gssapi");
        kernel.loadGBean(gssapiTransport, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.remoting.router.JMXRouter");
        gbean.setReferencePatterns("SubsystemRouter", Collections.singleton(secureSubsystemRouter));
        secureJmxRouter = new ObjectName("geronimo.remoting:router=JMXRouter,type=secure");
        kernel.loadGBean(secureJmxRouter, gbean);

        gbean = new GBeanMBean("org.apache.geronimo.security.remoting.jmx.LoginServiceStub");
        gbean.setReferencePatterns("Router", Collections.singleton(secureJmxRouter));
        serverStub = new ObjectName("geronimo.remoting:target=LoginServiceStub");
        kernel.loadGBean(serverStub, gbean);

        kernel.startGBean(loginService);
        kernel.startGBean(kerberosRealm);
        kernel.startGBean(subsystemRouter);
        kernel.startGBean(secureSubsystemRouter);
        kernel.startGBean(asyncTransport);
        kernel.startGBean(saslTransport);
        kernel.startGBean(gssapiTransport);
        kernel.startGBean(jmxRouter);
        kernel.startGBean(secureJmxRouter);
        kernel.startGBean(serverStub);

        TransportLoader bean = (TransportLoader) MBeanProxyFactory.getProxy(TransportLoader.class, kernel.getMBeanServer(), asyncTransport);
        URI connectURI = bean.getClientConnectURI();
        asyncRemoteProxy = RemoteLoginServiceFactory.create(connectURI.getHost(), connectURI.getPort());

        bean = (TransportLoader) MBeanProxyFactory.getProxy(TransportLoader.class, kernel.getMBeanServer(), saslTransport);
        connectURI = bean.getClientConnectURI();
        saslRemoteProxy = RemoteLoginServiceFactory.create(connectURI.getHost(), connectURI.getPort());

        bean = (TransportLoader) MBeanProxyFactory.getProxy(TransportLoader.class, kernel.getMBeanServer(), gssapiTransport);
        connectURI = bean.getClientConnectURI();
        gssapiRemoteProxy = RemoteLoginServiceFactory.create(connectURI.getHost(), connectURI.getPort());
    }

    protected void tearDown() throws Exception {
        kernel.stopGBean(serverStub);
        kernel.stopGBean(secureJmxRouter);
        kernel.stopGBean(jmxRouter);
        kernel.stopGBean(gssapiTransport);
        kernel.stopGBean(saslTransport);
        kernel.stopGBean(asyncTransport);
        kernel.stopGBean(secureSubsystemRouter);
        kernel.stopGBean(subsystemRouter);
        kernel.stopGBean(kerberosRealm);
        kernel.stopGBean(loginService);

        kernel.unloadGBean(loginService);
        kernel.unloadGBean(kerberosRealm);
        kernel.unloadGBean(subsystemRouter);
        kernel.unloadGBean(secureSubsystemRouter);
        kernel.unloadGBean(asyncTransport);
        kernel.unloadGBean(saslTransport);
        kernel.unloadGBean(gssapiTransport);
        kernel.unloadGBean(jmxRouter);
        kernel.unloadGBean(secureJmxRouter);
        kernel.unloadGBean(serverStub);

        kernel.shutdown();
    }

    class UsernamePasswordCallback implements CallbackHandler {
        private final String username;
        private final String password;

        UsernamePasswordCallback(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof PasswordCallback) {
                    ((PasswordCallback) callbacks[i]).setPassword(password.toCharArray());
                } else if (callbacks[i] instanceof NameCallback) {
                    ((NameCallback) callbacks[i]).setName(username);
                }
            }
        }
    }
}
