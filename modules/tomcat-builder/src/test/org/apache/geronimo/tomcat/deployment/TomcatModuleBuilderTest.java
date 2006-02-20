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
package org.apache.geronimo.tomcat.deployment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Reference;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.geronimo.axis.builder.AxisBuilder;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTrackingCoordinatorGBean;
import org.apache.geronimo.deployment.DeploymentContext;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.deployment.util.UnpackedJarFile;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.EJBReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.NamingContext;
import org.apache.geronimo.j2ee.deployment.RefContext;
import org.apache.geronimo.j2ee.deployment.ResourceReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.ServiceReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.WebServiceBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.J2eeContext;
import org.apache.geronimo.j2ee.j2eeobjectnames.J2eeContextImpl;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.j2ee.management.impl.J2EEServerImpl;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelFactory;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.DefaultArtifactManager;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationManagerImpl;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.config.ManageableAttributeStore;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.apache.geronimo.kernel.management.State;
import org.apache.geronimo.security.SecurityServiceImpl;
import org.apache.geronimo.security.jacc.ApplicationPolicyConfigurationManager;
import org.apache.geronimo.security.jacc.ComponentPermissions;
import org.apache.geronimo.system.serverinfo.BasicServerInfo;
import org.apache.geronimo.system.configuration.ExecutableConfigurationUtil;
import org.apache.geronimo.tomcat.ConnectorGBean;
import org.apache.geronimo.tomcat.EngineGBean;
import org.apache.geronimo.tomcat.HostGBean;
import org.apache.geronimo.tomcat.RealmGBean;
import org.apache.geronimo.tomcat.TomcatContainer;
import org.apache.geronimo.transaction.context.TransactionContextManagerGBean;
import org.apache.geronimo.transaction.manager.TransactionManagerImplGBean;

/**
 * @version $Rev$ $Date$
 */
public class TomcatModuleBuilderTest extends TestCase {
    private String DOMAIN_NAME = "geronimo.test";
    private String SERVER_NAME = "geronimo";
    private String BASE_NAME = DOMAIN_NAME + ":J2EEServer=" + SERVER_NAME;

    protected Kernel kernel;

    private GBeanData container;

    private ObjectName containerName;

    private ObjectName connectorName;

    private GBeanData connector;

    private ObjectName engineName;

    private GBeanData engine;

    private ObjectName hostName;

    private GBeanData host;

    private ObjectName realmName;

    private GBeanData realm;

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

    private J2eeContext moduleContext = new J2eeContextImpl(DOMAIN_NAME,
            SERVER_NAME, "null", NameFactory.WEB_MODULE, "Test", null, null);

    private TomcatModuleBuilder builder;

    private File basedir = new File(System.getProperty("basedir", "."));

    private List parentId = Arrays.asList(new Artifact[] {Artifact.create("geronimo/Foo/1/car")});
    private Environment defaultEnvironment = new Environment();

    public void testDeployWar4() throws Exception {
        deployWar("war4", "foo/bar/1/car");
    }

    public void testDeployWar5() throws Exception {
        deployWar("war5", "test/foo/1/car");
    }

    public void deployWar(String warName, String name) throws Exception {

        File outputPath = new File(basedir,
                "target/test-resources/deployables/" + warName);
        recursiveDelete(outputPath);
        outputPath.mkdirs();
        File path = new File(basedir, "src/test-resources/deployables/" + warName);
        File dest = new File(basedir, "target/test-resources/deployables/" + warName + "/war");
        recursiveCopy(path, dest);
        UnpackedJarFile jarFile = new UnpackedJarFile(dest);
        Module module = builder.createModule(null, jarFile);

        ObjectName jaccBeanName = NameFactory.getComponentName(null, null, null, null, "foo", NameFactory.JACC_MANAGER, moduleContext);
        GBeanData jaccBeanData = new GBeanData(jaccBeanName, ApplicationPolicyConfigurationManager.GBEAN_INFO);
        PermissionCollection excludedPermissions= new Permissions();
        PermissionCollection uncheckedPermissions= new Permissions();
        ComponentPermissions componentPermissions = new ComponentPermissions(excludedPermissions, uncheckedPermissions, new HashMap());
        Map contextIDToPermissionsMap = new HashMap();
        contextIDToPermissionsMap.put("test_J2EEApplication=null_J2EEServer=bar_j2eeType=WebModule_name=org/apache/geronimo/test", componentPermissions);
        jaccBeanData.setAttribute("contextIdToPermissionsMap", contextIDToPermissionsMap);
        jaccBeanData.setAttribute("principalRoleMap", new HashMap());
        jaccBeanData.setAttribute("roleDesignates", new HashMap());
        start(jaccBeanData);

        EARContext earContext = createEARContext(outputPath, defaultEnvironment);
        earContext.setJaccManagerName(jaccBeanName);
        ObjectName serverName = earContext.getServerObjectName();
        GBeanData server = new GBeanData(serverName, J2EEServerImpl.GBEAN_INFO);
        start(server);
        builder.initContext(earContext, module, cl);
        builder.addGBeans(earContext, module, cl);
        earContext.close();
        module.close();
        GBeanData configData = ExecutableConfigurationUtil.getConfigurationGBeanData(earContext.getConfigurationData());
        configData.setAttribute("baseURL", outputPath.toURL());
        kernel.loadGBean(configData, cl);
        ObjectName configName = configData.getName();
        kernel.startGBean(configName);
        kernel.invoke(configName, "loadGBeans", new Object[] {null}, new String[] {ManageableAttributeStore.class.getName()});
        kernel.invoke(configName, "startRecursiveGBeans");
        if (kernel.getGBeanState(configName) != State.RUNNING_INDEX) {
            fail("gbean not started: " + configName);
        }

        assertEquals(State.RUNNING_INDEX, kernel.getGBeanState(ObjectName.getInstance(BASE_NAME + ",J2EEApplication=null,j2eeType=WebModule,name=" + name)));
        Set names = kernel.listGBeans(ObjectName.getInstance(DOMAIN_NAME + ":J2EEApplication=null,WebModule=" + name + ",*"));
        System.out.println("Object names: " + names);
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            System.out.println("STATE: " + kernel.getGBeanState(objectName) + " - " + objectName.getCanonicalName());
            assertEquals(State.RUNNING_INDEX, kernel.getGBeanState(objectName));
        }

        //If we got here with no errors, then Tomcat deployed the war and loaded the classes

        kernel.stopGBean(configName);
        kernel.unloadGBean(configName);

        kernel.loadGBean(configData, cl);
        kernel.startGBean(configName);
        kernel.invoke(configName, "loadGBeans", new Object[] {null}, new String[] {ManageableAttributeStore.class.getName()});
        kernel.invoke(configName, "startRecursiveGBeans");
        kernel.stopGBean(configName);
        kernel.unloadGBean(configName);
    }

    private EARContext createEARContext(File outputPath, Environment environment)
            throws MalformedObjectNameException, DeploymentException {
        EARContext earContext = new EARContext(outputPath, environment,
                ConfigurationModuleType.WAR, kernel, moduleContext
                        .getJ2eeApplicationName(), tcmName, ctcName, null,
                null, null, new RefContext(new EJBReferenceBuilder() {

                    public Reference createEJBLocalReference(String objectName,
                                                             GBeanData gbeanData, boolean isSession, String localHome, String local)
                            throws DeploymentException {
                        return null;
                    }

                    public Reference createEJBRemoteReference(String objectName, GBeanData gbeanData, boolean isSession, String home,
                                                              String remote) throws DeploymentException {
                        return null;
                    }

                    public Reference createCORBAReference(URI corbaURL,
                                                          String objectName, ObjectName containerName,
                                                          String home) throws DeploymentException {
                        return null;
                    }

                    public Object createHandleDelegateReference() {
                        return null;
                    }

                    public Reference getImplicitEJBRemoteRef(URI module, String refName, boolean isSession, String home, String remote, NamingContext context) throws DeploymentException {
                        return null;
                    }

                    public Reference getImplicitEJBLocalRef(URI module, String refName, boolean isSession, String localHome, String local, NamingContext context) throws DeploymentException {
                        return null;
                    }
                }, new ResourceReferenceBuilder() {

                    public Reference createResourceRef(String containerId,
                                                       Class iface) throws DeploymentException {
                        return null;
                    }

                    public Reference createAdminObjectRef(String containerId,
                                                          Class iface) throws DeploymentException {
                        return null;
                    }

                    public ObjectName locateResourceName(ObjectName query)
                            throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateActivationSpecInfo(
                            GBeanData resourceAdapterModuleData,
                            String messageListenerInterface)
                            throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateResourceAdapterGBeanData(
                            GBeanData resourceAdapterModuleData)
                            throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateAdminObjectInfo(
                            GBeanData resourceAdapterModuleData,
                            String adminObjectInterfaceName)
                            throws DeploymentException {
                        return null;
                    }

                    public GBeanData locateConnectionFactoryInfo(
                            GBeanData resourceAdapterModuleData,
                            String connectionFactoryInterfaceName)
                            throws DeploymentException {
                        return null;
                    }
                }, new ServiceReferenceBuilder() {
                    //it could return a Service or a Reference, we don't care
                    public Object createService(Class serviceInterface, URI wsdlURI, URI jaxrpcMappingURI, QName serviceQName, Map portComponentRefMap, List handlerInfos, Object serviceRefType, DeploymentContext deploymentContext, Module module, ClassLoader classLoader) throws DeploymentException {
                        return null;
                    }
                }, kernel));
        return earContext;
    }

    private void recursiveDelete(File path) {
        // does not delete top level dir passed in
        File[] listing = path.listFiles();
        for (int i = 0; i < ((listing == null) ? 0 : listing.length); i++) {
            File file = listing[i];
            if (file.isDirectory()) {
                recursiveDelete(file);
            }
            file.delete();
        }
    }

    public void recursiveCopy(File src, File dest) throws IOException {
        Collection files = FileUtils.listFiles(src,null,true);
        Iterator iterator = files.iterator();
        while(iterator.hasNext()){
            File file = (File) iterator.next();
            if (file.getAbsolutePath().indexOf(".svn") < 0){
                String pathToFile = file.getPath();
                String relativePath = pathToFile.substring(src.getPath().length(), pathToFile.length() - (file.getName().length()));
                FileUtils.copyFileToDirectory(file,new File(dest.getPath() + relativePath));
            }
        }
    }

    protected void setUp() throws Exception {
        Artifact artifact = Artifact.create("test/foo/1/car");
        defaultEnvironment.setConfigId(artifact);
        Map properties = new HashMap();
        properties.put(NameFactory.JSR77_BASE_NAME_PROPERTY, BASE_NAME);
        defaultEnvironment.addProperties(properties);
        cl = this.getClass().getClassLoader();
        containerName = NameFactory.getWebComponentName(null, null, null, null,
                "tomcatContainer", "WebResource", moduleContext);
        connectorName = NameFactory.getWebComponentName(null, null, null, null,
                "tomcatConnector", "WebResource", moduleContext);
        realmName = NameFactory.getWebComponentName(null, null, null, null,
                "tomcatRealm", "WebResource", moduleContext);
        engineName = NameFactory.getWebComponentName(null, null, null, null,
                "tomcatEngine", "WebResource", moduleContext);
        hostName = NameFactory.getWebComponentName(null, null, null, null,
                "tomcatHost", "WebResource", moduleContext);

        tmName = NameFactory.getComponentName(null, null, null, null, null,
                "TransactionManager", NameFactory.TRANSACTION_MANAGER, moduleContext);
        tcmName = NameFactory.getComponentName(null, null, null, null, null,
                "TransactionContextManager", NameFactory.TRANSACTION_CONTEXT_MANAGER,
                moduleContext);
        ctcName = new ObjectName(
                "geronimo.server:role=ConnectionTrackingCoordinator");

        kernel = KernelFactory.newInstance().createKernel("foo");
        kernel.boot();

        GBeanData store = new GBeanData(JMXUtil
                .getObjectName("foo:j2eeType=ConfigurationStore,name=mock"),
                MockConfigStore.GBEAN_INFO);
        kernel.loadGBean(store, this.getClass().getClassLoader());
        kernel.startGBean(store.getName());

        ObjectName configurationManagerName = new ObjectName(":j2eeType=ConfigurationManager,name=Basic");
        GBeanData configurationManagerData = new GBeanData(configurationManagerName, ConfigurationManagerImpl.GBEAN_INFO);
        configurationManagerData.setReferencePatterns("Stores", Collections.singleton(store.getName()));
        kernel.loadGBean(configurationManagerData, getClass().getClassLoader());
        kernel.startGBean(configurationManagerName);

        GBeanData manager = new GBeanData(JMXUtil.getObjectName("foo:name=ArtifactManager"), DefaultArtifactManager.GBEAN_INFO);
        kernel.loadGBean(manager, this.getClass().getClassLoader());
        kernel.startGBean(manager.getName());

        GBeanData resolver = new GBeanData(JMXUtil.getObjectName("foo:name=ArtifactResolver"), DefaultArtifactResolver.GBEAN_INFO);
        resolver.setReferencePattern("ArtifactManager", manager.getName());
//            resolver.setReferencePattern("Repositories", repository.getName());
        kernel.loadGBean(resolver, this.getClass().getClassLoader());
        kernel.startGBean(resolver.getName());

        ConfigurationManager configurationManager = (ConfigurationManager) kernel.getProxyManager().createProxy(configurationManagerName, ConfigurationManager.class);

        configurationManager.load((Artifact) parentId.get(0));
        configurationManager.loadGBeans((Artifact) parentId.get(0));
        configurationManager.start((Artifact) parentId.get(0));

        serverInfoName = new ObjectName("geronimo.system:name=ServerInfo");
        serverInfoGBean = new GBeanData(serverInfoName, BasicServerInfo.GBEAN_INFO);
        serverInfoGBean.setAttribute("baseDirectory", ".");
        start(serverInfoGBean);

        // install the policy configuration factory
        securityServiceName = new ObjectName("foo:j2eeType=SecurityService");
        securityServiceGBean = new GBeanData(securityServiceName,
                SecurityServiceImpl.GBEAN_INFO);
        securityServiceGBean.setReferencePattern("ServerInfo", serverInfoName);
        securityServiceGBean
                .setAttribute("policyConfigurationFactory",
                        "org.apache.geronimo.security.jacc.GeronimoPolicyConfigurationFactory");
        securityServiceGBean.setAttribute("policyProvider",
                "org.apache.geronimo.security.jacc.GeronimoPolicy");
        start(securityServiceGBean);

        WebServiceBuilder webServiceBuilder = new AxisBuilder();

        builder = new TomcatModuleBuilder(defaultEnvironment, true, containerName, webServiceBuilder, null);

        // Default Realm
        Map initParams = new HashMap();

        initParams.put("userClassNames",
                        "org.apache.geronimo.security.realm.providers.GeronimoUserPrincipal");
        initParams.put("roleClassNames",
                        "org.apache.geronimo.security.realm.providers.GeronimoGroupPrincipal");
        realm = new GBeanData(realmName, RealmGBean.GBEAN_INFO);
        realm.setAttribute("className",
                "org.apache.geronimo.tomcat.realm.TomcatJAASRealm");
        realm.setAttribute("initParams", initParams);
        start(realm);

        // Default Host
        initParams.clear();
        initParams.put("workDir", "work");
        initParams.put("name", "localhost");
        initParams.put("appBase", "");
        host = new GBeanData(hostName, HostGBean.GBEAN_INFO);
        host.setAttribute("className", "org.apache.catalina.core.StandardHost");
        host.setAttribute("initParams", initParams);
        start(host);

        // Default Engine
        initParams.clear();
        initParams.put("name", "Geronimo");
        engine = new GBeanData(engineName, EngineGBean.GBEAN_INFO);
        engine.setAttribute("className", "org.apache.geronimo.tomcat.TomcatEngine");
        engine.setAttribute("initParams", initParams);
        engine.setReferencePattern("DefaultHost", hostName);
        engine.setReferencePattern("RealmGBean", realmName);
        engine.setReferencePattern("Hosts", hostName);
        start(engine);

        container = new GBeanData(containerName, TomcatContainer.GBEAN_INFO);
        container.setAttribute("classLoader", cl);
        container.setAttribute("catalinaHome", "target/var/catalina");
        container.setReferencePattern("EngineGBean", engineName);
        container.setReferencePattern("ServerInfo", serverInfoName);

        connector = new GBeanData(connectorName, ConnectorGBean.GBEAN_INFO);
        connector.setAttribute("name", "HTTP");
        connector.setAttribute("port", new Integer(8181));
        connector.setReferencePattern("TomcatContainer", containerName);

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
        private final Kernel kernel;

        public MockConfigStore(Kernel kernel) {
            this.kernel = kernel;
        }

        public Artifact install(URL source) throws IOException, InvalidConfigException {
            return null;
        }

        public void install(ConfigurationData configurationData) throws IOException, InvalidConfigException {
        }

        public void uninstall(Artifact configID) throws NoSuchConfigException, IOException {
        }

        public ObjectName loadConfiguration(Artifact configId) throws NoSuchConfigException, IOException, InvalidConfigException {
            ObjectName configurationObjectName = Configuration.getConfigurationObjectName(configId);
            GBeanData configData = new GBeanData(configurationObjectName, Configuration.GBEAN_INFO);
            Environment environment = new Environment();
            environment.setConfigId(configId);
            environment.getProperties().put(NameFactory.JSR77_BASE_NAME_PROPERTY, "geronimo.test:J2EEServer=geronimo");
            configData.setAttribute("environment", environment);
            configData.setAttribute("gBeanState", NO_OBJECTS_OS);

            try {
                kernel.loadGBean(configData, Configuration.class.getClassLoader());
            } catch (Exception e) {
                throw new InvalidConfigException("Unable to register configuration", e);
            }

            return configurationObjectName;
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
            return null;
        }

        public final static GBeanInfo GBEAN_INFO;

        private static final byte[] NO_OBJECTS_OS;

        static {
            GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(MockConfigStore.class, NameFactory.CONFIGURATION_STORE);
            infoBuilder.addInterface(ConfigurationStore.class);
            infoBuilder.addAttribute("kernel", Kernel.class, false);
            infoBuilder.setConstructor(new String[] {"kernel"});
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
