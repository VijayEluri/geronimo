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

package org.apache.geronimo.connector.outbound;

import junit.framework.TestCase;
import org.apache.geronimo.connector.mock.ConnectionFactoryExtension;
import org.apache.geronimo.connector.mock.MockConnection;
import org.apache.geronimo.connector.mock.MockConnectionFactory;
import org.apache.geronimo.connector.mock.MockManagedConnectionFactory;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoTransactions;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTracker;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.J2eeContext;
import org.apache.geronimo.j2ee.j2eeobjectnames.J2eeContextImpl;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelFactory;
import org.apache.geronimo.kernel.repository.Artifact;

import javax.management.MalformedObjectNameException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
public class ManagedConnectionFactoryWrapperTest extends TestCase {

    private Kernel kernel;
    private AbstractName managedConnectionFactoryName;
    private AbstractName ctcName;
    private AbstractName cmfName;
    private static final String KERNEL_NAME = "testKernel";
    private static final String TARGET_NAME = "testCFName";

    public void testProxy() throws Exception {
        Object proxy = kernel.invoke(managedConnectionFactoryName, "$getResource");
        assertNotNull(proxy);
        assertTrue(proxy instanceof ConnectionFactory);
        Connection connection = ((ConnectionFactory) proxy).getConnection();
        assertNotNull(connection);
        kernel.stopGBean(managedConnectionFactoryName);
        try {
            ((ConnectionFactory) proxy).getConnection();
            fail();
        } catch (IllegalStateException ise) {
        }
        kernel.startGBean(managedConnectionFactoryName);
        ((ConnectionFactory) proxy).getConnection();
        //check implemented interfaces
        assertTrue(proxy instanceof Serializable);
        assertTrue(proxy instanceof ConnectionFactoryExtension);
        assertEquals("SomethingElse", ((ConnectionFactoryExtension)proxy).doSomethingElse());
    }

    public void testSerialization() throws Exception {
        ConnectionFactory proxy = (ConnectionFactory) kernel.invoke(managedConnectionFactoryName, "$getResource");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(proxy);
        oos.flush();
        byte[] bytes = baos.toByteArray();
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object proxy2 = ois.readObject();
        assertNotNull(proxy2);
        assertTrue(proxy instanceof ConnectionFactory);
        Connection connection = proxy.getConnection();
        assertNotNull(connection);
        kernel.stopGBean(managedConnectionFactoryName);
        ObjectInputStream ois2 = new ObjectInputStream(new ByteArrayInputStream(bytes));
        ConnectionFactory proxy3 = (ConnectionFactory) ois2.readObject();
        try {
            proxy3.getConnection();
            fail();
        } catch (IllegalStateException ise) {
        }
        kernel.startGBean(managedConnectionFactoryName);
        proxy3.getConnection();

    }

    protected void setUp() throws Exception {
        kernel = KernelFactory.newInstance().createKernel(KERNEL_NAME);
        kernel.boot();
        ClassLoader cl = MockConnectionTrackingCoordinator.class.getClassLoader();
        J2eeContext j2eeContext = new J2eeContextImpl("test.domain", "geronimo", "testapplication", "noModuleType", "testmodule", TARGET_NAME, NameFactory.JCA_MANAGED_CONNECTION_FACTORY);

        GBeanData ctc = buildGBeanData("name", "ConnectionTrackingCoordinator", MockConnectionTrackingCoordinator.getGBeanInfo(), "ConnectionTrackingCoordinator", j2eeContext);
        ctcName = ctc.getAbstractName();
        kernel.loadGBean(ctc, cl);

        GBeanData cmf = buildGBeanData("name", "ConnectionManagerContainer", GenericConnectionManagerGBean.getGBeanInfo(), "ConnectionManagerContainer", j2eeContext);
        cmfName = cmf.getAbstractName();
        cmf.setAttribute("transactionSupport", NoTransactions.INSTANCE);
        cmf.setAttribute("pooling", new NoPool());
        cmf.setReferencePattern("ConnectionTracker", ctcName);
        kernel.loadGBean(cmf, cl);


        GBeanData mcfw = buildGBeanData("name", TARGET_NAME, ManagedConnectionFactoryWrapperGBean.getGBeanInfo(), NameFactory.JCA_RESOURCE, j2eeContext);
        managedConnectionFactoryName = mcfw.getAbstractName();
        mcfw.setAttribute("managedConnectionFactoryClass", MockManagedConnectionFactory.class.getName());
        mcfw.setAttribute("connectionFactoryInterface", ConnectionFactory.class.getName());
        mcfw.setAttribute("implementedInterfaces", new String[] {Serializable.class.getName(), ConnectionFactoryExtension.class.getName()});
        mcfw.setAttribute("connectionFactoryImplClass", MockConnectionFactory.class.getName());
        mcfw.setAttribute("connectionInterface", Connection.class.getName());
        mcfw.setAttribute("connectionImplClass", MockConnection.class.getName());
        //"ResourceAdapterWrapper",
        mcfw.setReferencePattern("ConnectionManagerContainer", cmfName);
        //"ManagedConnectionFactoryListener",
        kernel.loadGBean(mcfw, cl);

        kernel.startGBean(ctcName);
        kernel.startGBean(cmfName);
        kernel.startGBean(managedConnectionFactoryName);
    }
    private GBeanData buildGBeanData(String key, String value, GBeanInfo info, String type, J2eeContext j2eeContext) throws MalformedObjectNameException {
        AbstractName abstractName = buildAbstractName(key, value, info, type, j2eeContext);
        return new GBeanData(abstractName, info);
    }

    private AbstractName buildAbstractName(String key, String value, GBeanInfo info, String type, J2eeContext j2eeContext) throws MalformedObjectNameException {
        Map names = new HashMap();
        names.put(key, value);
        return new AbstractName(new Artifact("test", "foo", "1", "car"), names, info.getInterfaces(), NameFactory.getComponentName(null, null, null, type, null, null, value, j2eeContext));
    }


    protected void tearDown() throws Exception {
        kernel.stopGBean(managedConnectionFactoryName);
        kernel.shutdown();
    }

    public static class MockConnectionTrackingCoordinator implements ConnectionTracker {
        public void handleObtained(ConnectionTrackingInterceptor connectionTrackingInterceptor,
                                   ConnectionInfo connectionInfo) {
        }

        public void handleReleased(ConnectionTrackingInterceptor connectionTrackingInterceptor,
                                   ConnectionInfo connectionInfo) {
        }

        public void setEnvironment(ConnectionInfo connectionInfo, String key) {
        }

        static final GBeanInfo GBEAN_INFO;

        static {
            GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(MockConnectionTrackingCoordinator.class);
            infoFactory.addInterface(ConnectionTracker.class);
            GBEAN_INFO = infoFactory.getBeanInfo();
        }

        public static GBeanInfo getGBeanInfo() {
            return GBEAN_INFO;
        }
    }
}
