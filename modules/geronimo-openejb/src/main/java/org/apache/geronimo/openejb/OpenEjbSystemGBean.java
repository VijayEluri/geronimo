/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.openejb;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ejb.spi.HandleDelegate;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.resource.spi.ResourceAdapter;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.ResourceAdapterWrapper;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.ReferenceCollection;
import org.apache.geronimo.gbean.ReferenceCollectionEvent;
import org.apache.geronimo.gbean.ReferenceCollectionListener;
import org.apache.geronimo.gbean.SingleElementCollection;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.openejb.Container;
import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.NoSuchApplicationException;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.UndeployException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.ClientInfo;
import org.apache.openejb.assembler.classic.ContainerInfo;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.MdbContainerInfo;
import org.apache.openejb.assembler.classic.ProxyFactoryInfo;
import org.apache.openejb.assembler.classic.SecurityServiceInfo;
import org.apache.openejb.assembler.classic.TransactionServiceInfo;
import org.apache.openejb.assembler.dynamic.PassthroughFactory;
import org.apache.openejb.config.AppModule;
import org.apache.openejb.config.ClientModule;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.EjbModule;
import org.apache.openejb.core.ServerFederation;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ApplicationServer;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.openejb.util.proxy.Jdk13ProxyFactory;

import org.omg.CORBA.ORB;

/**
 * @version $Rev$ $Date$
 */
public class OpenEjbSystemGBean implements OpenEjbSystem {
    private static final Log log = LogFactory.getLog(OpenEjbSystemGBean.class);
    private final ConfigurationFactory configurationFactory;
    private final Assembler assembler;
    private final ConcurrentMap<String,ResourceAdapterWrapper> processedResourceAdapterWrappers =  new ConcurrentHashMap<String,ResourceAdapterWrapper>() ;
    private final ClassLoader classLoader;
    private final SingleElementCollection orbProvider;                

    public OpenEjbSystemGBean(TransactionManager transactionManager) throws Exception {
        this(transactionManager, null, null, null, OpenEjbSystemGBean.class.getClassLoader());
    }
    public OpenEjbSystemGBean(TransactionManager transactionManager, Collection<ResourceAdapterWrapper> resourceAdapters, Collection<ORBProvider> orbProviders, Kernel kernel, ClassLoader classLoader) throws Exception {
        this.classLoader = classLoader;
        orbProvider = new SingleElementCollection(orbProviders); 
        
        System.setProperty("duct tape","");
        SystemInstance systemInstance = SystemInstance.get();

        String format = systemInstance.getProperty("openejb.deploymentId.format");
        if (format == null){
            systemInstance.setProperty("openejb.deploymentId.format", "{moduleId}/{ejbName}");
        }

        System.setProperty("openejb.naming", "xbean");
        if (transactionManager == null) {
            throw new NullPointerException("transactionManager is null");
        }

        boolean offline = true;
        configurationFactory = new ConfigurationFactory(offline);
        assembler = new Assembler();

        // install application server
        ApplicationServer applicationServer = new ServerFederation();
        SystemInstance.get().setComponent(ApplicationServer.class, applicationServer);

        // install transaction manager
        transactionManager = getRawService(kernel, transactionManager);
        TransactionServiceInfo transactionServiceInfo = new TransactionServiceInfo();
        PassthroughFactory.add(transactionServiceInfo, transactionManager);
        try {
            transactionServiceInfo.id = "Default Transaction Manager";
            transactionServiceInfo.serviceType = "TransactionManager";
            assembler.createTransactionManager(transactionServiceInfo);
        } finally {
            PassthroughFactory.remove(transactionServiceInfo);
        }

        // install security service
        SecurityServiceInfo securityServiceInfo = configurationFactory.configureService(SecurityServiceInfo.class);
        assembler.createSecurityService(securityServiceInfo);

        // install proxy factory
        ProxyFactoryInfo proxyFactoryInfo = new ProxyFactoryInfo();
        proxyFactoryInfo.id = "Default JDK 1.3 ProxyFactory";
        proxyFactoryInfo.serviceType = "ProxyFactory";
        proxyFactoryInfo.className = Jdk13ProxyFactory.class.getName();
        proxyFactoryInfo.properties = new Properties();
        assembler.createProxyFactory(proxyFactoryInfo);
        
        // install CORBA values 
        ORBProvider orbSource = (ORBProvider)orbProvider.getElement(); 
        if (orbSource != null) {
            SystemInstance.get().setComponent(ORB.class, orbSource.getORB());
            SystemInstance.get().setComponent(HandleDelegate.class, orbSource.getHandleDelegate());
        }

        // add our thread context listener
        GeronimoThreadContextListener.init();

        // process all resource adapters
        processResourceAdapterWrappers(resourceAdapters);
    }

    @SuppressWarnings({"unchecked"})
    private static <T> T getRawService(Kernel kernel, T proxy) {
        if (kernel == null) return proxy;

        AbstractName abstractName = kernel.getAbstractNameFor(proxy);
        if (abstractName == null) return proxy;

        try {
            Object service = kernel.getGBean(abstractName);
            return (T) service;
        } catch (GBeanNotFoundException e) {
        }

        return proxy;
    }

    private void processResourceAdapterWrappers(Collection<ResourceAdapterWrapper> resourceAdapterWrappers) {
        if (resourceAdapterWrappers == null) {
            return;
        }

        if (resourceAdapterWrappers instanceof ReferenceCollection) {
            ReferenceCollection referenceCollection = (ReferenceCollection) resourceAdapterWrappers;
            referenceCollection.addReferenceCollectionListener(new ReferenceCollectionListener() {
                public void memberAdded(ReferenceCollectionEvent event) {
                    addResourceAdapter((ResourceAdapterWrapper) event.getMember());
                }

                public void memberRemoved(ReferenceCollectionEvent event) {
                    removeResourceAdapter((ResourceAdapterWrapper) event.getMember());
                }
            });
        }
        for (ResourceAdapterWrapper resourceAdapterWrapper : resourceAdapterWrappers) {
            addResourceAdapter(resourceAdapterWrapper);
        }

    }

    private void addResourceAdapter(ResourceAdapterWrapper resourceAdapterWrapper) {
        ResourceAdapter resourceAdapter = resourceAdapterWrapper.getResourceAdapter();
        if (resourceAdapter == null) {
            return;
        }
        
        Map<String, String> listenerToActivationSpecMap = resourceAdapterWrapper.getMessageListenerToActivationSpecMap();
        if (listenerToActivationSpecMap == null) {
            return;
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            for (Map.Entry<String, String> entry : listenerToActivationSpecMap.entrySet()) {
                String messageListenerInterface = entry.getKey();
                String activationSpecClass = entry.getValue();

                // only process RA if not previously processed
                String containerName = getResourceAdapterId(resourceAdapterWrapper) + "-" + messageListenerInterface;
                if (processedResourceAdapterWrappers.putIfAbsent(containerName,  resourceAdapterWrapper) == null) {
                    try {
                        // get default mdb config
                        ContainerInfo containerInfo = configurationFactory.configureService(MdbContainerInfo.class);
                        containerInfo.id = containerName;
                        containerInfo.displayName = containerName;

                        // set ra specific properties
                        containerInfo.properties.put("MessageListenerInterface",
                                resourceAdapter.getClass().getClassLoader().loadClass(messageListenerInterface));
                        containerInfo.properties.put("ActivationSpecClass",
                                resourceAdapter.getClass().getClassLoader().loadClass(activationSpecClass));
                        containerInfo.properties.put("ResourceAdapter", resourceAdapter);

                        // create the container
                        assembler.createContainer(containerInfo);
                    } catch (Exception e) {
                        log.error("Unable to deploy mdb container " + containerName, e);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void removeResourceAdapter(ResourceAdapterWrapper resourceAdapterWrapper) {
        for (String messageListenerInterface : resourceAdapterWrapper.getMessageListenerToActivationSpecMap().keySet()) {
            String containerName = getResourceAdapterId(resourceAdapterWrapper) + "-" + messageListenerInterface;
            processedResourceAdapterWrappers.remove(containerName);
            assembler.removeContainer(containerName);
        }
    }

    private String getResourceAdapterId(ResourceAdapterWrapper resourceAdapterWrapper) {
        String name = resourceAdapterWrapper.getName();
        try {
            ObjectName objectName = new ObjectName(name);
            Map properties = objectName.getKeyPropertyList();
            String shortName = (String) properties.get("name");
            String moduleName = (String) properties.get("ResourceAdapterModule");
            if (shortName != null && moduleName != null) {
                return moduleName + "." + shortName;
            }
        } catch (Exception ignored) {
        }
        return name;
    }

    public ContainerSystem getContainerSystem() {
        return assembler.getContainerSystem();
    }

    public Container createContainer(Class<? extends ContainerInfo> type, String serviceId, Properties declaredProperties, String providerId) throws OpenEJBException {
        ContainerInfo containerInfo = configurationFactory.configureService(type, serviceId, declaredProperties, providerId, "Container");
        assembler.createContainer(containerInfo);
        Container container = assembler.getContainerSystem().getContainer(serviceId);
        return container;
    }

    public ClientInfo configureApplication(ClientModule clientModule) throws OpenEJBException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clientModule.getClassLoader());
        try {
            return configurationFactory.configureApplication(clientModule);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public AppInfo configureApplication(AppModule appModule) throws OpenEJBException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(appModule.getClassLoader());
        try {
            return configurationFactory.configureApplication(appModule);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public EjbJarInfo configureApplication(EjbModule ejbModule) throws OpenEJBException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ejbModule.getClassLoader());
        try {
            return configurationFactory.configureApplication(ejbModule);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public void createClient(ClientInfo clientInfo, ClassLoader classLoader) throws NamingException, IOException, OpenEJBException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            assembler.createClient(clientInfo, classLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public void createEjbJar(EjbJarInfo ejbJarInfo, ClassLoader classLoader) throws NamingException, IOException, OpenEJBException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            assembler.createEjbJar(ejbJarInfo, classLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public void removeEjbJar(EjbJarInfo ejbJarInfo, ClassLoader classLoader) throws UndeployException, NoSuchApplicationException {
        assembler.destroyApplication(ejbJarInfo.jarPath);
    }

    public DeploymentInfo getDeploymentInfo(String deploymentId) {
        return getContainerSystem().getDeploymentInfo(deploymentId);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(OpenEjbSystemGBean.class);
        infoBuilder.addReference("TransactionManager", TransactionManager.class);
        infoBuilder.addReference("ResourceAdapterWrappers", ResourceAdapterWrapper.class);
        infoBuilder.addReference("ORBProviders", ORBProvider.class);
        infoBuilder.addAttribute("kernel", Kernel.class, false);
        infoBuilder.addAttribute("classLoader", ClassLoader.class, false);
        infoBuilder.setConstructor(new String[] {
                "TransactionManager",
                "ResourceAdapterWrappers",
                "ORBProviders",
                "kernel",
                "classLoader",
        });
        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
