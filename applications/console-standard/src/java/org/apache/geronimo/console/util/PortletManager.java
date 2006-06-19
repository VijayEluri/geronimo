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
package org.apache.geronimo.console.util;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderResponse;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.deployment.plugin.factories.DeploymentFactoryImpl;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelRegistry;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.proxy.GeronimoManagedBean;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Repository;
import org.apache.geronimo.management.J2EEDeployedObject;
import org.apache.geronimo.management.geronimo.J2EEDomain;
import org.apache.geronimo.management.geronimo.J2EEServer;
import org.apache.geronimo.management.geronimo.JCAAdminObject;
import org.apache.geronimo.management.geronimo.JCAManagedConnectionFactory;
import org.apache.geronimo.management.geronimo.JCAResource;
import org.apache.geronimo.management.geronimo.JMSBroker;
import org.apache.geronimo.management.geronimo.JMSConnector;
import org.apache.geronimo.management.geronimo.JMSManager;
import org.apache.geronimo.management.geronimo.JVM;
import org.apache.geronimo.management.geronimo.ResourceAdapter;
import org.apache.geronimo.management.geronimo.ResourceAdapterModule;
import org.apache.geronimo.management.geronimo.WebAccessLog;
import org.apache.geronimo.management.geronimo.WebConnector;
import org.apache.geronimo.management.geronimo.WebContainer;
import org.apache.geronimo.management.geronimo.WebManager;
import org.apache.geronimo.system.logging.SystemLog;

/**
 * @version $Rev$ $Date$
 */
public class PortletManager {
    private final static Log log = LogFactory.getLog(PortletManager.class);
    // The following are currently static due to having only one server/JVM/etc. per Geronimo
    private final static String HELPER_KEY = "org.apache.geronimo.console.ManagementHelper";
    private final static String DOMAIN_KEY = "org.apache.geronimo.console.J2EEDomain";
    private final static String SERVER_KEY = "org.apache.geronimo.console.J2EEServer";
    private final static String JVM_KEY = "org.apache.geronimo.console.JVM";
    private final static String SYSTEM_LOG_KEY = "org.apache.geronimo.console.SystemLog";
    // The following may change based on the user's selections
    // nothing yet

    private static ManagementHelper createHelper() {
        //todo: consider making this configurable; we could easily connect to a remote kernel if we wanted to
        Kernel kernel = null;
        try {
            kernel = (Kernel) new InitialContext().lookup("java:comp/GeronimoKernel");
        } catch (NamingException e) {
//            log.error("Unable to look up kernel in JNDI", e);
        }
        if (kernel == null) {
            log.debug("Unable to find kernel in JNDI; using KernelRegistry instead");
            kernel = KernelRegistry.getSingleKernel();
        }
        return new KernelManagementHelper(kernel);
    }

    public static DeploymentManager getDeploymentManager(PortletRequest request) {
        DeploymentFactoryImpl factory = new DeploymentFactoryImpl();
        try {
            return factory.getDeploymentManager("deployer:geronimo:inVM", null, null);
        } catch (DeploymentManagerCreationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ManagementHelper getManagementHelper(PortletRequest request) {
        ManagementHelper helper = (ManagementHelper) request.getPortletSession(true).getAttribute(HELPER_KEY, PortletSession.APPLICATION_SCOPE);
        if (helper == null) {
            helper = createHelper();
            request.getPortletSession().setAttribute(HELPER_KEY, helper, PortletSession.APPLICATION_SCOPE);
        }
        return helper;
    }

    public static ManagementHelper getManagementHelper(HttpSession session) {
        ManagementHelper helper = (ManagementHelper) session.getAttribute(HELPER_KEY);
        if (helper == null) {
            helper = createHelper();
            session.setAttribute(HELPER_KEY, helper);
        }
        return helper;
    }

    public static J2EEDomain getCurrentDomain(PortletRequest request) {
        J2EEDomain domain = (J2EEDomain) request.getPortletSession(true).getAttribute(DOMAIN_KEY, PortletSession.APPLICATION_SCOPE);
        if (domain == null) {
            domain = getManagementHelper(request).getDomains()[0]; //todo: some day, select a domain
            request.getPortletSession().setAttribute(DOMAIN_KEY, domain, PortletSession.APPLICATION_SCOPE);
        }
        return domain;

    }

    public static J2EEServer getCurrentServer(PortletRequest request) {
        J2EEServer server = (J2EEServer) request.getPortletSession(true).getAttribute(SERVER_KEY, PortletSession.APPLICATION_SCOPE);
        if (server == null) {
            server = getCurrentDomain(request).getServerInstances()[0]; //todo: some day, select a server from the domain
            request.getPortletSession().setAttribute(SERVER_KEY, server, PortletSession.APPLICATION_SCOPE);
        } else {
            // to do     handle "should not occur" error   - message?
        }
        return server;
    }

    public static JVM getCurrentJVM(PortletRequest request) {
        JVM jvm = (JVM) request.getPortletSession(true).getAttribute(JVM_KEY, PortletSession.APPLICATION_SCOPE);
        if (jvm == null) {
            ManagementHelper helper = getManagementHelper(request);
            jvm = helper.getJavaVMs(getCurrentServer(request))[0]; //todo: some day, select a JVM from the server
            request.getPortletSession().setAttribute(JVM_KEY, jvm, PortletSession.APPLICATION_SCOPE);
        }
        return jvm;
    }

    public static void testLoginModule(PortletRequest request, LoginModule module, Map options) {
        ManagementHelper helper = getManagementHelper(request);
        helper.testLoginModule(getCurrentServer(request), module, options);
    }

    public static Subject testLoginModule(PortletRequest request, LoginModule module, Map options, String username, String password) throws LoginException {
        ManagementHelper helper = getManagementHelper(request);
        return helper.testLoginModule(getCurrentServer(request), module, options, username, password);
    }

    public static ResourceAdapterModule[] getOutboundRAModules(PortletRequest request, String iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundRAModules(getCurrentServer(request), iface);
    }

    public static ResourceAdapterModule[] getOutboundRAModules(PortletRequest request, String[] iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundRAModules(getCurrentServer(request), iface);
    }

    public static ResourceAdapterModule[] getAdminObjectModules(PortletRequest request, String[] ifaces) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getAdminObjectModules(getCurrentServer(request), ifaces);
    }

    public static JCAManagedConnectionFactory[] getOutboundFactoriesOfType(PortletRequest request, String iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundFactories(getCurrentServer(request), iface);
    }

    public static JCAManagedConnectionFactory[] getOutboundFactoriesForRA(PortletRequest request, AbstractName resourceAdapterModuleName) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundFactories((ResourceAdapterModule) helper.getObject(resourceAdapterModuleName));
    }

    public static JCAManagedConnectionFactory[] getOutboundFactoriesForRA(PortletRequest request, AbstractName resourceAdapterModuleName, String iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundFactories((ResourceAdapterModule) helper.getObject(resourceAdapterModuleName), iface);
    }

    public static JCAManagedConnectionFactory[] getOutboundFactoriesForRA(PortletRequest request, ResourceAdapterModule module) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundFactories(module);
    }

    public static JCAManagedConnectionFactory[] getOutboundFactoriesForRA(PortletRequest request, ResourceAdapterModule module, String iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundFactories(module, iface);
    }

    public static JCAManagedConnectionFactory[] getOutboundFactoriesForRA(PortletRequest request, ResourceAdapterModule module, String[] iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getOutboundFactories(module, iface);
    }

    //todo: Create an interface for admin objects
    public static JCAAdminObject[] getAdminObjectsForRA(PortletRequest request, ResourceAdapterModule module, String[] ifaces) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getAdminObjects(module, ifaces);
    }

    public static WebManager[] getWebManagers(PortletRequest request) {
        return getCurrentServer(request).getWebManagers();
    }

    public static WebManager getWebManager(PortletRequest request, AbstractName managerName) {
        ManagementHelper helper = getManagementHelper(request);
        return (WebManager) helper.getObject(managerName);
    }

//    private static String[] namesToStrings(AbstractName[] names) {
//        String[] result = new String[names.length];
//        for (int i = 0; i < names.length; i++) {
//            AbstractName name = names[i];
//            result[i] = name.toURI().toString();
//        }
//        return result;
//    }
//

    public static WebAccessLog getWebAccessLog(PortletRequest request, AbstractName managerName, AbstractName containerName) {
        ManagementHelper helper = getManagementHelper(request);
        WebManager manager = (WebManager) helper.getObject(managerName);
        return manager.getAccessLog((WebContainer) helper.getObject(containerName));
    }

    public static WebContainer getWebContainer(PortletRequest request, AbstractName containerName) {
        ManagementHelper helper = getManagementHelper(request);
        return (WebContainer) helper.getObject(containerName);
    }

    public static WebConnector createWebConnector(PortletRequest request, AbstractName managerName, AbstractName containerName, String name, String protocol, String host, int port) {
        ManagementHelper helper = getManagementHelper(request);
        WebManager manager = (WebManager) helper.getObject(managerName);
        return manager.addConnector((WebContainer) helper.getObject(containerName), name, protocol, host, port);
    }

    public static WebConnector[] getWebConnectors(PortletRequest request, AbstractName managerName) {
        ManagementHelper helper = getManagementHelper(request);
        WebManager manager = (WebManager) helper.getObject(managerName);
        return (WebConnector[]) manager.getConnectors();
    }

    public static WebConnector[] getWebConnectors(PortletRequest request, AbstractName managerName, String protocol) {
        ManagementHelper helper = getManagementHelper(request);
        WebManager manager = (WebManager) helper.getObject(managerName);
        return (WebConnector[]) manager.getConnectors(protocol);
    }

    public static WebConnector getWebConnector(PortletRequest request, AbstractName connectorName) {
        ManagementHelper helper = getManagementHelper(request);
        return (WebConnector) helper.getObject(connectorName);
    }

    public static WebConnector[] getWebConnectorsForContainer(PortletRequest request, AbstractName managerName, AbstractName containerName, String protocol) {
        ManagementHelper helper = getManagementHelper(request);
        WebManager manager = (WebManager) helper.getObject(managerName);
        return (WebConnector[]) manager.getConnectorsForContainer(containerName, protocol);
    }

    public static JMSBroker getJMSBroker(PortletRequest request, AbstractName brokerName) {
        ManagementHelper helper = getManagementHelper(request);
        return (JMSBroker) helper.getObject(brokerName);
    }

    public static JMSConnector createJMSConnector(PortletRequest request, JMSManager manager, AbstractName containerName, String name, String protocol, String host, int port) {
        return manager.addConnector(getJMSBroker(request, containerName), name, protocol, host, port);
    }

    public static JMSConnector[] getJMSConnectors(PortletRequest request, AbstractName managerName) {
        ManagementHelper helper = getManagementHelper(request);
        JMSManager manager = (JMSManager) helper.getObject(managerName);
        return (JMSConnector[]) manager.getConnectors();
    }

    public static JMSConnector[] getJMSConnectors(PortletRequest request, AbstractName managerName, String protocol) {
        ManagementHelper helper = getManagementHelper(request);
        JMSManager manager = (JMSManager) helper.getObject(managerName);
        return (JMSConnector[]) manager.getConnectors(protocol);
    }

    public static JMSConnector[] getJMSConnectorsForContainer(PortletRequest request, AbstractName managerName, AbstractName brokerName) {
        ManagementHelper helper = getManagementHelper(request);
        JMSManager manager = (JMSManager) helper.getObject(managerName);
        return (JMSConnector[]) manager.getConnectorsForContainer(brokerName);
    }

    public static JMSConnector[] getJMSConnectorsForContainer(PortletRequest request, AbstractName managerName, AbstractName brokerName, String protocol) {
        ManagementHelper helper = getManagementHelper(request);
        JMSManager manager = (JMSManager) helper.getObject(managerName);
        return (JMSConnector[]) manager.getConnectorsForContainer(brokerName, protocol);
    }

    public static ResourceAdapter[] getResourceAdapters(PortletRequest request, ResourceAdapterModule module) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getResourceAdapters(module);
    }

    public static JCAResource[] getJCAResources(PortletRequest request, ResourceAdapter adapter) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getRAResources(adapter);
    }

    public static String getGBeanDescription(PortletRequest request, AbstractName objectName) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getGBeanDescription(objectName);
    }

    public static SystemLog getCurrentSystemLog(PortletRequest request) {
        SystemLog log = (SystemLog) request.getPortletSession(true).getAttribute(SYSTEM_LOG_KEY, PortletSession.APPLICATION_SCOPE);
        if (log == null) {
            ManagementHelper helper = getManagementHelper(request);
            log = helper.getSystemLog(getCurrentJVM(request));
            request.getPortletSession().setAttribute(SYSTEM_LOG_KEY, log, PortletSession.APPLICATION_SCOPE);
        }
        return log;
    }

    public static GeronimoManagedBean[] getManagedBeans(PortletRequest request, Class intrface) {
        ManagementHelper helper = getManagementHelper(request);
        Object[] obs = helper.findByInterface(intrface);
        GeronimoManagedBean[] results = new GeronimoManagedBean[obs.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = (GeronimoManagedBean) obs[i];
        }
        return results;
    }

    public static GeronimoManagedBean getManagedBean(PortletRequest request, AbstractName name) {
        ManagementHelper helper = getManagementHelper(request);
        return (GeronimoManagedBean) helper.getObject(name);
    }

    public static Artifact getConfigurationFor(PortletRequest request, AbstractName objectName) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getConfigurationNameFor(objectName);
    }

    public static AbstractName getNameFor(PortletRequest request, Object component) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getNameFor(component);
    }

    public static File getRepositoryEntry(PortletRequest request, String repositoryURI) {
        J2EEServer server = getCurrentServer(request);
        Repository[] repos = server.getRepositories();
        Artifact uri = Artifact.create(repositoryURI);
        if (!uri.isResolved()) {
            Artifact[] all = server.getConfigurationManager().getArtifactResolver().queryArtifacts(uri);
            if (all.length == 0) {
                return null;
            } else {
                uri = all[all.length - 1];
            }
        }
        for (int i = 0; i < repos.length; i++) {
            Repository repo = repos[i];
            if (repo.contains(uri)) {
                return repo.getLocation(uri);
            }
        }
        return null;
    }

    public static J2EEDeployedObject getModule(PortletRequest request, Artifact configuration) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getModuleForConfiguration(configuration);
    }

    public static ConfigurationData[] getConfigurations(PortletRequest request, ConfigurationModuleType type, boolean includeChildModules) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getConfigurations(type, includeChildModules);
    }

    public static Object[] getGBeansImplementing(PortletRequest request, Class iface) {
        ManagementHelper helper = getManagementHelper(request);
        return helper.getGBeansImplementing(iface);
    }

    /**
     * Looks up the context prefix used by the portal, even if the thing running
     * is in one of the portlets.  We're kind of hacking our way there, but hey,
     * it beats hardcoding.
     */
    public static String getConsoleFrameworkServletPath(HttpServletRequest request) {
        String contextPath;
        Object response = request.getAttribute("javax.portlet.response");
        if (response != null && response instanceof RenderResponse) { // request came from a portlet
            String pathInfo = request.getPathInfo();
            String portletPath = Pattern.compile("/").split(pathInfo)[1];
            contextPath = ((RenderResponse) response).createRenderURL().toString();
            contextPath = Pattern.compile("/" + portletPath).split(contextPath)[0];
        } else { // request did not come from a portlet
            contextPath = request.getContextPath();
        }
        return contextPath;
    }
}
