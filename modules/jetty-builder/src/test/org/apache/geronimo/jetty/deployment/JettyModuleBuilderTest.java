/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.apache.geronimo.jetty.deployment;

import junit.framework.TestCase;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTrackingCoordinatorGBean;
import org.apache.geronimo.deployment.DeploymentContext;
import org.apache.geronimo.deployment.util.UnpackedJarFile;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.EJBReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.RefContext;
import org.apache.geronimo.j2ee.deployment.ResourceReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.ServiceReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.UnavailableWebServiceBuilder;
import org.apache.geronimo.j2ee.deployment.WebServiceBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.J2eeContext;
import org.apache.geronimo.j2ee.j2eeobjectnames.J2eeContextImpl;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.j2ee.management.impl.J2EEServerImpl;
import org.apache.geronimo.jetty.JettyContainerImpl;
import org.apache.geronimo.jetty.connector.HTTPConnector;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelFactory;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationManagerImpl;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.apache.geronimo.kernel.management.State;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.DefaultArtifactManager;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.ImportType;
import org.apache.geronimo.security.SecurityServiceImpl;
import org.apache.geronimo.system.serverinfo.BasicServerInfo;
import org.apache.geronimo.transaction.context.TransactionContextManagerGBean;
import org.apache.geronimo.transaction.manager.TransactionManagerImplGBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Reference;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @version $Rev:385232 $ $Date$
 */
public class JettyModuleBuilderTest extends TestCase {
    private String DOMAIN_NAME = "geronimo.test";
    private String SERVER_NAME = "geronimo";
    private String BASE_NAME = DOMAIN_NAME + ":J2EEServer=" + SERVER_NAME;

    protected Kernel kernel;
    private GBeanData container;
    private ObjectName containerName;
    private ObjectName connectorName;
    private GBeanData connector;
    private GBeanData securityServiceGBean;
    private ObjectName securityServiceName;
    private ObjectName serverInfoName;
    private GBeanData serverInfoGBean;
    private ObjectName tmName;
    private ObjectName ctcName;
    private GBeanData tm;
    private GBeanData ctc;
    private ObjectName tcmName;
    private GBeanData tcm;
    private ClassLoader cl;
    private J2eeContext moduleContext = new J2eeContextImpl(DOMAIN_NAME, SERVER_NAME, "null", NameFactory.WEB_MODULE, "jettyTest", null, null);
    private JettyModuleBuilder builder;
    private File basedir = new File(System.getProperty("basedir", "."));
    private String PARENT_ARTIFACT_ID = "geronimo/Foo/1/car";
    private String ARTIFACT_ID = "foo/bar/1/car";
    private Artifact parentId = Artifact.create(PARENT_ARTIFACT_ID);
    private Environment defaultEnvironment = new Environment();
    private ConfigurationManager configurationManager;

    public void testDeployWar4() throws Exception {
        File outputPath = new File(basedir, "target/test-resources/deployables/war4");
        recursiveDelete(outputPath);
        outputPath.mkdirs();
        new File(outputPath, "war").mkdir();
        File path = new File(basedir, "src/test-resources/deployables/war4");
        UnpackedJarFile jarFile = new UnpackedJarFile(path);
        Module module = builder.createModule(null, jarFile);
        EARContext earContext = createEARContext(outputPath, defaultEnvironment);
        module.setEarContext(earContext);
        ObjectName serverName = earContext.getServerName();
        GBeanData server = new GBeanData(serverName, J2EEServerImpl.GBEAN_INFO);
        start(server);
        builder.initContext(earContext, module, cl);
        builder.addGBeans(earContext, module, cl, null);
        earContext.close();
        module.close();

        ConfigurationStore configurationStore = new MockConfigStore(outputPath.toURL());
        Configuration configuration = configurationManager.loadConfiguration(earContext.getConfigurationData(), configurationStore);
        configurationManager.startConfiguration(configuration);

        assertEquals(State.RUNNING_INDEX, kernel.getGBeanState(ObjectName.getInstance(BASE_NAME + ",J2EEApplication=null,j2eeType=WebModule,name=" + ARTIFACT_ID)));
        Set names = kernel.listGBeans(ObjectName.getInstance(DOMAIN_NAME + ":J2EEApplication=null,WebModule=" + ARTIFACT_ID + ",*"));
        System.out.println("Object names: " + names);
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            assertEquals(State.RUNNING_INDEX, kernel.getGBeanState(objectName));
        }
        GBeanData filterMapping2Data = kernel.getGBeanData(ObjectName.getInstance(BASE_NAME + ",J2EEApplication=null,Servlet=Servlet1,WebFilter=Filter2,WebModule=" + ARTIFACT_ID + ",j2eeType=WebFilterMapping"));

        configurationManager.stopConfiguration(configuration);
        configurationManager.unloadConfiguration(configuration);


        //what is this testing?
        configuration = configurationManager.loadConfiguration(earContext.getConfigurationData(), configurationStore);
        configurationManager.startConfiguration(configuration);
        configurationManager.stopConfiguration(configuration);
        configurationManager.unloadConfiguration(configuration);
    }

    private EARContext createEARContext(File outputPath, Environment environment) throws MalformedObjectNameException, DeploymentException {
        EARContext earContext = new EARContext(outputPath,
                environment,
                ConfigurationModuleType.WAR,
                kernel,
                serverName, moduleContext.getJ2eeApplicationName(),
                tcmName,
                ctcName,
                null,
                null,
                null,
                new RefContext(new EJBReferenceBuilder() {


                    public Reference createCORBAReference(Configuration configuration, AbstractNameQuery containerNameQuery, URI nsCorbaloc, String objectName, String home) throws DeploymentException {
                        return null;
                    }

                    public Object createHandleDelegateReference() {
                        return null;
                    }

                    public Reference createEJBRemoteRef(String requiredModule, String optionalModule, String name, Artifact targetConfigId, AbstractNameQuery query, boolean isSession, String home, String remote, Configuration configuration) throws DeploymentException {
                        return null;
                    }

                    public Reference createEJBLocalRef(String requiredModule, String optionalModule, String name, Artifact targetConfigId, AbstractNameQuery query, boolean isSession, String localHome, String local, Configuration configuration) throws DeploymentException {
                        return null;
                    }

                },
                new ResourceReferenceBuilder() {

                    public Reference createResourceRef(AbstractNameQuery containerId, Class iface, Configuration configuration) throws DeploymentException {
                        return null;
                    }

                    public Reference createAdminObjectRef(AbstractNameQuery containerId, Class iface, Configuration configuration) throws DeploymentException {
                        return null;
                    }

                    public ObjectName locateResourceName(ObjectName query) throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateActivationSpecInfo(AbstractNameQuery nameQuery, String messageListenerInterface, Configuration configuration) throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateResourceAdapterGBeanData(GBeanData resourceAdapterModuleData) throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateAdminObjectInfo(GBeanData resourceAdapterModuleData, String adminObjectInterfaceName) throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateConnectionFactoryInfo(GBeanData resourceAdapterModuleData, String connectionFactoryInterfaceName) throws DeploymentException {
                        return null;
                    }
                },
                new ServiceReferenceBuilder() {
                    //it could return a Service or a Reference, we don't care
                    public Object createService(Class serviceInterface, URI wsdlURI, URI jaxrpcMappingURI, QName serviceQName, Map portComponentRefMap, List handlerInfos, Object serviceRefType, DeploymentContext deploymentContext, Module module, ClassLoader classLoader) throws DeploymentException {
                        return null;
                    }
                }));
                return earContext;
    }

    private void recursiveDelete(File path) {
        //does not delete top level dir passed in
        File[] listing = path.listFiles();
        for (int i = 0; i < ((listing == null) ? 0 : listing.length); i++) {
            File file = listing[i];
            if (file.isDirectory()) {
                recursiveDelete(file);
            }
            file.delete();
        }
    }

    protected void setUp() throws Exception {
        defaultEnvironment.getProperties().put(NameFactory.JSR77_BASE_NAME_PROPERTY, BASE_NAME);
        cl = this.getClass().getClassLoader();
        containerName = NameFactory.getWebComponentName(null, null, null, null, "jettyContainer", "WebResource", moduleContext);
        connectorName = NameFactory.getWebComponentName(null, null, null, null, "jettyConnector", "WebResource", moduleContext);
        //        webModuleName = NameFactory.getWebComponentName(null, null, null, null, NameFactory.WEB_MODULE, "WebResource", moduleContext);

        tmName = NameFactory.getComponentName(null, null, null, null, null, "TransactionManager", NameFactory.TRANSACTION_MANAGER, moduleContext);
        tcmName = NameFactory.getComponentName(null, null, null, null, null, "TransactionContextManager", NameFactory.TRANSACTION_CONTEXT_MANAGER, moduleContext);
        ctcName = new ObjectName("geronimo.test:role=ConnectionTrackingCoordinator");

        kernel = KernelFactory.newInstance().createKernel("foo");
        kernel.boot();

        GBeanData store = new GBeanData(JMXUtil.getObjectName("foo:j2eeType=ConfigurationStore,name=mock"), MockConfigStore.GBEAN_INFO);
        kernel.loadGBean(store, this.getClass().getClassLoader());
        kernel.startGBean(store.getName());

        GBeanData artifactManager = new GBeanData(JMXUtil.getObjectName("foo:name=ArtifactManager"), DefaultArtifactManager.GBEAN_INFO);
        kernel.loadGBean(artifactManager, this.getClass().getClassLoader());
        kernel.startGBean(artifactManager.getName());

        GBeanData artifactResolver = new GBeanData(JMXUtil.getObjectName("foo:name=ArtifactResolver"), DefaultArtifactResolver.GBEAN_INFO);
        artifactResolver.setReferencePattern("ArtifactManager", artifactManager.getName());
        kernel.loadGBean(artifactResolver, this.getClass().getClassLoader());
        kernel.startGBean(artifactResolver.getName());

        ObjectName configurationManagerName = new ObjectName(":j2eeType=ConfigurationManager,name=Basic");
        GBeanData configurationManagerData = new GBeanData(configurationManagerName, ConfigurationManagerImpl.GBEAN_INFO);
        configurationManagerData.setReferencePattern("Stores", store.getName());
        configurationManagerData.setReferencePattern("ArtifactManager", artifactManager.getName());
        configurationManagerData.setReferencePattern("ArtifactResolver", artifactResolver.getName());
        kernel.loadGBean(configurationManagerData, getClass().getClassLoader());
        kernel.startGBean(configurationManagerName);
        configurationManager = (ConfigurationManager) kernel.getProxyManager().createProxy(configurationManagerName, ConfigurationManager.class);

        configurationManager.loadConfiguration(parentId);
        configurationManager.startConfiguration(parentId);

        Collection defaultServlets = new HashSet();
        Collection defaultFilters = new HashSet();
        Collection defaultFilterMappings = new HashSet();
        Object pojoWebServiceTemplate = null;
        WebServiceBuilder webServiceBuilder = new UnavailableWebServiceBuilder();

        serverInfoName = new ObjectName("geronimo.system:name=ServerInfo");
        serverInfoGBean = new GBeanData(serverInfoName, BasicServerInfo.GBEAN_INFO);
        serverInfoGBean.setAttribute("baseDirectory", ".");
        start(serverInfoGBean);

        //install the policy configuration factory
        securityServiceName = new ObjectName("foo:j2eeType=SecurityService");
        securityServiceGBean = new GBeanData(securityServiceName, SecurityServiceImpl.GBEAN_INFO);
        securityServiceGBean.setReferencePattern("ServerInfo", serverInfoName);
        securityServiceGBean.setAttribute("policyConfigurationFactory", "org.apache.geronimo.security.jacc.GeronimoPolicyConfigurationFactory");
        securityServiceGBean.setAttribute("policyProvider", "org.apache.geronimo.security.jacc.GeronimoPolicy");
        start(securityServiceGBean);

        defaultEnvironment.addDependency(parentId, ImportType.ALL);
        Artifact artifact = Artifact.create("foo/bar/1/car");
        defaultEnvironment.setConfigId(artifact);
        builder = new JettyModuleBuilder(defaultEnvironment, new Integer(1800), false, Collections.EMPTY_LIST, containerName, defaultServlets, defaultFilters, defaultFilterMappings, pojoWebServiceTemplate, webServiceBuilder, kernel);

        container = new GBeanData(containerName, JettyContainerImpl.GBEAN_INFO);

        connector = new GBeanData(connectorName, HTTPConnector.GBEAN_INFO);
        connector.setAttribute("port", new Integer(5678));
        connector.setAttribute("maxThreads", new Integer(50));
        connector.setAttribute("minThreads", new Integer(10));
        connector.setReferencePattern("JettyContainer", containerName);

        start(container);
        start(connector);

        tm = new GBeanData(tmName, TransactionManagerImplGBean.GBEAN_INFO);
        Set patterns = new HashSet();
        patterns.add(ObjectName.getInstance("geronimo.server:j2eeType=JCAManagedConnectionFactory,*"));
        tm.setAttribute("defaultTransactionTimeoutSeconds", new Integer(10));
        tm.setReferencePatterns("ResourceManagers", patterns);
        start(tm);
        tcm = new GBeanData(tcmName, TransactionContextManagerGBean.GBEAN_INFO);
        tcm.setReferencePattern("TransactionManager", tmName);
        start(tcm);
        ctc = new GBeanData(ctcName, ConnectionTrackingCoordinatorGBean.GBEAN_INFO);
        start(ctc);


    }

    protected void tearDown() throws Exception {
        stop(ctcName);
        stop(tmName);
        stop(serverInfoName);
        stop(securityServiceName);
        stop(connectorName);
        stop(containerName);
        kernel.shutdown();
    }

    private void start(GBeanData gbeanData) throws Exception {
        kernel.loadGBean(gbeanData, cl);
        kernel.startGBean(gbeanData.getName());
        if (kernel.getGBeanState(gbeanData.getName()) != State.RUNNING_INDEX) {
            fail("gbean not started: " + gbeanData.getName());
        }
    }

    private void stop(ObjectName name) throws Exception {
        kernel.stopGBean(name);
        kernel.unloadGBean(name);
    }

    public static class MockConfigStore implements ConfigurationStore {

        URL baseURL;

        public MockConfigStore() {
        }

        public MockConfigStore(URL baseURL) {
            this.baseURL = baseURL;
        }

        public void install(ConfigurationData configurationData) throws IOException, InvalidConfigException {
        }

        public void uninstall(Artifact configID) throws NoSuchConfigException, IOException {
        }

        public GBeanData loadConfiguration(Artifact configId) throws NoSuchConfigException, IOException, InvalidConfigException {
            ObjectName configurationObjectName = Configuration.getConfigurationObjectName(configId);
            GBeanData configData = new GBeanData(configurationObjectName, Configuration.GBEAN_INFO);
            Environment environment = new Environment();
            environment.setConfigId(configId);
            environment.getProperties().put(NameFactory.JSR77_BASE_NAME_PROPERTY, "geronimo.test:J2EEServer=geronimo");
            configData.setAttribute("environment", environment);
            configData.setAttribute("gBeanState", NO_OBJECTS_OS);

            return configData;
        }

        public boolean containsConfiguration(Artifact configID) {
            return true;
        }

        public String getObjectName() {
            return null;
        }

        public List listConfigurations() {
            return null;
        }

        public File createNewConfigurationDir(Artifact configId) {
            return null;
        }

        public URL resolve(Artifact configId, URI uri) throws NoSuchConfigException, MalformedURLException {
            return baseURL;
        }

        public final static GBeanInfo GBEAN_INFO;

        private static final byte[] NO_OBJECTS_OS;

        static {
            GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(MockConfigStore.class, NameFactory.CONFIGURATION_STORE);
            infoBuilder.addInterface(ConfigurationStore.class);
//            infoBuilder.setConstructor(new String[] {"kernel"});
            GBEAN_INFO = infoBuilder.getBeanInfo();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.flush();
                NO_OBJECTS_OS = baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
