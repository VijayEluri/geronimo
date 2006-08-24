/**
 *
 * Copyright 2004-2005 The Apache Software Foundation
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
package org.apache.geronimo.connector.deployment;

import java.beans.Introspector;
import java.beans.PropertyEditor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.naming.Reference;
import javax.xml.namespace.QName;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.common.UnresolvedReferenceException;
import org.apache.geronimo.common.propertyeditor.PropertyEditors;
import org.apache.geronimo.connector.ActivationSpecWrapperGBean;
import org.apache.geronimo.connector.AdminObjectWrapper;
import org.apache.geronimo.connector.AdminObjectWrapperGBean;
import org.apache.geronimo.connector.JCAResourceImplGBean;
import org.apache.geronimo.connector.ResourceAdapterImplGBean;
import org.apache.geronimo.connector.ResourceAdapterModuleImplGBean;
import org.apache.geronimo.connector.ResourceAdapterWrapperGBean;
import org.apache.geronimo.connector.outbound.JCAConnectionFactoryImplGBean;
import org.apache.geronimo.connector.outbound.ManagedConnectionFactoryWrapper;
import org.apache.geronimo.connector.outbound.ManagedConnectionFactoryWrapperGBean;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.LocalTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PartitionedPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.SinglePool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionLog;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.deployment.ModuleIDBuilder;
import org.apache.geronimo.deployment.NamespaceDrivenBuilderCollection;
import org.apache.geronimo.deployment.NamespaceDrivenBuilder;
import org.apache.geronimo.deployment.DeployableModule;
import org.apache.geronimo.deployment.service.EnvironmentBuilder;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.deployment.xbeans.EnvironmentType;
import org.apache.geronimo.deployment.xmlbeans.XmlBeansUtil;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.DynamicGAttributeInfo;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.apache.geronimo.j2ee.deployment.ConnectorModule;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.ModuleBuilder;
import org.apache.geronimo.j2ee.deployment.ResourceReferenceBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.management.JCAConnectionFactory;
import org.apache.geronimo.management.geronimo.JCAAdminObject;
import org.apache.geronimo.management.geronimo.JCAResourceAdapter;
import org.apache.geronimo.management.geronimo.ResourceAdapterModule;
import org.apache.geronimo.naming.deployment.ENCConfigBuilder;
import org.apache.geronimo.naming.reference.ResourceReference;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.xbeans.geronimo.GerAdminobjectInstanceType;
import org.apache.geronimo.xbeans.geronimo.GerAdminobjectType;
import org.apache.geronimo.xbeans.geronimo.GerConfigPropertySettingType;
import org.apache.geronimo.xbeans.geronimo.GerConnectionDefinitionType;
import org.apache.geronimo.xbeans.geronimo.GerConnectiondefinitionInstanceType;
import org.apache.geronimo.xbeans.geronimo.GerConnectionmanagerType;
import org.apache.geronimo.xbeans.geronimo.GerConnectorDocument;
import org.apache.geronimo.xbeans.geronimo.GerConnectorType;
import org.apache.geronimo.xbeans.geronimo.GerPartitionedpoolType;
import org.apache.geronimo.xbeans.geronimo.GerResourceadapterType;
import org.apache.geronimo.xbeans.geronimo.GerSinglepoolType;
import org.apache.geronimo.xbeans.j2ee.ActivationspecType;
import org.apache.geronimo.xbeans.j2ee.AdminobjectType;
import org.apache.geronimo.xbeans.j2ee.ConfigPropertyType;
import org.apache.geronimo.xbeans.j2ee.ConnectionDefinitionType;
import org.apache.geronimo.xbeans.j2ee.ConnectorDocument;
import org.apache.geronimo.xbeans.j2ee.ConnectorType;
import org.apache.geronimo.xbeans.j2ee.MessagelistenerType;
import org.apache.geronimo.xbeans.j2ee.ResourceadapterType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * @version $Rev:385659 $ $Date$
 */
public class ConnectorModuleBuilder implements ModuleBuilder, ResourceReferenceBuilder {
    private static QName CONNECTOR_QNAME = GerConnectorDocument.type.getDocumentElementName();
    static final String GERCONNECTOR_NAMESPACE = CONNECTOR_QNAME.getNamespaceURI();

    private final int defaultMaxSize;
    private final int defaultMinSize;
    private final int defaultBlockingTimeoutMilliseconds;
    private final int defaultIdleTimeoutMinutes;
    private final boolean defaultXATransactionCaching;
    private final boolean defaultXAThreadCaching;
    private final Environment defaultEnvironment;
    private final NamespaceDrivenBuilderCollection serviceBuilders;

    public ConnectorModuleBuilder(Environment defaultEnvironment,
            int defaultMaxSize,
            int defaultMinSize,
            int defaultBlockingTimeoutMilliseconds,
            int defaultIdleTimeoutMinutes,
            boolean defaultXATransactionCaching,
            boolean defaultXAThreadCaching,
            Collection serviceBuilders) {
        this.defaultEnvironment = defaultEnvironment;

        this.defaultMaxSize = defaultMaxSize;
        this.defaultMinSize = defaultMinSize;
        this.defaultBlockingTimeoutMilliseconds = defaultBlockingTimeoutMilliseconds;
        this.defaultIdleTimeoutMinutes = defaultIdleTimeoutMinutes;
        this.defaultXATransactionCaching = defaultXATransactionCaching;
        this.defaultXAThreadCaching = defaultXAThreadCaching;
        this.serviceBuilders = new NamespaceDrivenBuilderCollection(serviceBuilders);
    }

    public Module createModule(File plan, DeployableModule moduleFile, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
        return createModule(plan, moduleFile, "rar", null, null, null, naming, idBuilder);
    }

    public Module createModule(Object plan, DeployableModule moduleFile, String targetPath, URL specDDUrl, Environment environment, Object moduleContextInfo, AbstractName earName, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
        return createModule(plan, moduleFile, targetPath, specDDUrl, environment, earName, naming, idBuilder);
    }

    private Module createModule(Object plan, DeployableModule moduleFile, String targetPath, URL specDDUrl, Environment earEnvironment, AbstractName earName, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
        assert moduleFile != null: "moduleFile is null";
        assert targetPath != null: "targetPath is null";
        assert !targetPath.endsWith("/"): "targetPath must not end with a '/'";

        String specDD;
        XmlObject connector;
        try {
            if (specDDUrl == null) {
                specDDUrl = moduleFile.resolve("META-INF/ra.xml");
            }

            // read in the entire specDD as a string, we need this for getDeploymentDescriptor
            // on the J2ee management object
            specDD = DeploymentUtil.readAll(specDDUrl);
        } catch (Exception e) {
            //no ra.xml, not for us.
            return null;
        }
        //we found ra.xml, if it won't parse it's an error.
        try {
            // parse it
            XmlObject xmlObject = XmlBeansUtil.parse(specDD);
            ConnectorDocument connectorDoc = SchemaConversionUtils.convertToConnectorSchema(xmlObject);
            connector = connectorDoc.getConnector();
        } catch (XmlException e) {
            throw new DeploymentException("Could not parse ra.xml descriptor", e);
        }
        GerConnectorType gerConnector = null;
        try {
            // load the geronimo connector plan from either the supplied plan or from the earFile
            try {
                if (plan instanceof XmlObject) {
                    gerConnector = (GerConnectorType) SchemaConversionUtils.getNestedObjectAsType((XmlObject) plan,
                            CONNECTOR_QNAME,
                            GerConnectorType.type);
                } else {
                    GerConnectorDocument gerConnectorDoc;
                    ArrayList errors = new ArrayList();
                    if (plan != null) {
                        gerConnectorDoc = GerConnectorDocument.Factory.parse((File) plan, XmlBeansUtil.createXmlOptions(errors));
                    } else {
                        URL path = moduleFile.resolve("META-INF/geronimo-ra.xml");
                        gerConnectorDoc = GerConnectorDocument.Factory.parse(path, XmlBeansUtil.createXmlOptions(errors));
                    }
                    if (errors.size() > 0) {
                        throw new DeploymentException("Could not parse connector doc: " + errors);
                    }
                    if (gerConnectorDoc != null) {
                        gerConnector = gerConnectorDoc.getConnector();
                    }
                }
            } catch (IOException e) {
                //do nothing
            }

            // if we got one extract the validate it otherwise create a default one
            if (gerConnector == null) {
                throw new DeploymentException("A connector module must be deployed using a Geronimo deployment plan" +
                        " (either META-INF/geronimo-ra.xml in the RAR file or a standalone deployment plan passed to the deployer).");
            }
            ConnectorPlanRectifier.rectifyPlan(gerConnector);
            XmlCursor cursor = gerConnector.newCursor();
            try {
                SchemaConversionUtils.convertToGeronimoSubSchemas(cursor);
            } finally {
                cursor.dispose();
            }

            XmlBeansUtil.validateDD(gerConnector);
        } catch (XmlException e) {
            throw new DeploymentException(e);
        }

        EnvironmentType environmentType = gerConnector.getEnvironment();
        Environment environment = EnvironmentBuilder.buildEnvironment(environmentType, defaultEnvironment);
        if (earEnvironment != null) {
            EnvironmentBuilder.mergeEnvironments(earEnvironment, environment);
            environment = earEnvironment;
            if (!environment.getConfigId().isResolved()) {
                throw new IllegalStateException("Connector module ID should be fully resolved (not " + environment.getConfigId() + ")");
            }
        } else {
            idBuilder.resolve(environment, moduleFile.getRoot().getName(), "rar");
        }

        AbstractName moduleName;
        if (earName == null) {
            earName = naming.createRootName(environment.getConfigId(), NameFactory.NULL, NameFactory.J2EE_APPLICATION);
            moduleName = naming.createChildName(earName, environment.getConfigId().toString(), NameFactory.RESOURCE_ADAPTER_MODULE);
        } else {
            moduleName = naming.createChildName(earName, targetPath, NameFactory.RESOURCE_ADAPTER_MODULE);
        }

        boolean standAlone = earEnvironment == null;
        return new ConnectorModule(standAlone, moduleName, environment, moduleFile, targetPath, connector, gerConnector, specDD);
    }

    public void installModule(DeployableModule earFile, EARContext earContext, Module module, Collection configurationStores, ConfigurationStore targetConfigurationStore, Collection repository) throws DeploymentException {
        try {
            JarFile moduleFile = module.getModuleFile();

            // add the manifest classpath entries declared in the connector to the class loader
            // we have to explicitly add these since we are unpacking the connector module
            // and the url class loader will not pick up a manifiest from an unpacked dir
            // N.B. If we ever introduce a separate configuration/module for a rar inside an ear
            // this will need to be modified to use "../" instead of module.getTargetPath().
            // See AbstractWebModuleBuilder.
            earContext.addManifestClassPath(moduleFile, URI.create(module.getTargetPath()));

            URI targetURI = URI.create(module.getTargetPath() + "/");
            Enumeration entries = moduleFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                URI target = targetURI.resolve(entry.getName());
                if (entry.getName().endsWith(".jar")) {
                    earContext.addInclude(target, moduleFile, entry);
                } else {
                    earContext.addFile(target, moduleFile, entry);
                }
            }

        } catch (IOException e) {
            throw new DeploymentException("Problem deploying connector", e);
        }
    }

    public void initContext(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
        ConnectorModule resourceModule = (ConnectorModule) module;

        final ConnectorType connector = (ConnectorType) module.getSpecDD();

        /*
        The chain of idiotic jsr-77 meaningless objects is:
        ResourceAdapterModule (1)  >
        ResourceAdapter (n, but there can only be 1 resource adapter in a rar, so we use 1) >
        JCAResource (1) >
        JCAConnectionFactory (n) >
        JCAManagedConnectionFactory (1)
        We also include:
        JCAResourceAdapter (n)  (from JCAResource) (actual instance of ResourceAdapter)
        TODO include admin objects (n) from JCAResource presumably
        */
        AbstractName resourceAdapterModuleName = resourceModule.getModuleName();

        AbstractName resourceAdapterjsr77Name = earContext.getNaming().createChildName(resourceAdapterModuleName, module.getName(), NameFactory.RESOURCE_ADAPTER);
        AbstractName jcaResourcejsr77Name = earContext.getNaming().createChildName(resourceAdapterjsr77Name, module.getName(), NameFactory.JCA_RESOURCE);

        //set up the metadata for the ResourceAdapterModule
        GBeanData resourceAdapterModuleData = new GBeanData(resourceAdapterModuleName, ResourceAdapterModuleImplGBean.GBEAN_INFO);
        // initalize the GBean
        if (earContext.getServerName() != null) {
            //app clients don't have a Server gbean
            resourceAdapterModuleData.setReferencePattern(NameFactory.J2EE_SERVER, earContext.getServerName());
            //app clients don't have an application name either
            if (!earContext.getModuleName().equals(resourceAdapterModuleName)) {
                resourceAdapterModuleData.setReferencePattern(NameFactory.J2EE_APPLICATION, earContext.getModuleName());
            }
        }
        resourceAdapterModuleData.setReferencePattern("ResourceAdapter", resourceAdapterjsr77Name);

        resourceAdapterModuleData.setAttribute("deploymentDescriptor", module.getOriginalSpecDD());
        resourceAdapterModuleData.setAttribute("displayName", connector.getDisplayNameArray().length == 0 ? null : connector.getDisplayNameArray(0).getStringValue());
        resourceAdapterModuleData.setAttribute("description", connector.getDescriptionArray().length == 0 ? null : connector.getDescriptionArray(0).getStringValue());
        resourceAdapterModuleData.setAttribute("vendorName", connector.getVendorName().getStringValue());
        resourceAdapterModuleData.setAttribute("EISType", connector.getEisType().getStringValue());
        resourceAdapterModuleData.setAttribute("resourceAdapterVersion", connector.getResourceadapterVersion().getStringValue());

        ResourceadapterType resourceadapter = connector.getResourceadapter();
        // Create the resource adapter gbean
        if (resourceadapter.isSetResourceadapterClass()) {
            GBeanInfoBuilder resourceAdapterInfoBuilder = new GBeanInfoBuilder(ResourceAdapterWrapperGBean.class, ResourceAdapterWrapperGBean.GBEAN_INFO);
            GBeanData resourceAdapterGBeanData = setUpDynamicGBean(resourceAdapterInfoBuilder, resourceadapter.getConfigPropertyArray(), cl);

            resourceAdapterGBeanData.setAttribute("resourceAdapterClass", resourceadapter.getResourceadapterClass().getStringValue().trim());
            resourceAdapterModuleData.setAttribute("resourceAdapterGBeanData", resourceAdapterGBeanData);
        }

        if (resourceadapter.isSetInboundResourceadapter() && resourceadapter.getInboundResourceadapter().isSetMessageadapter())
        {
            Map activationSpecInfoMap = getActivationSpecInfoMap(resourceadapter.getInboundResourceadapter().getMessageadapter().getMessagelistenerArray(), cl);
            resourceAdapterModuleData.setAttribute("activationSpecInfoMap", activationSpecInfoMap);
        }
        Map adminObjectInfoMap = getAdminObjectInfoMap(resourceadapter.getAdminobjectArray(), cl);
        resourceAdapterModuleData.setAttribute("adminObjectInfoMap", adminObjectInfoMap);
        if (resourceadapter.isSetOutboundResourceadapter()) {
            Map managedConnectionFactoryInfoMap = getManagedConnectionFactoryInfoMap(resourceadapter.getOutboundResourceadapter().getConnectionDefinitionArray(), cl);
            resourceAdapterModuleData.setAttribute("managedConnectionFactoryInfoMap", managedConnectionFactoryInfoMap);
        }

        try {
            earContext.addGBean(resourceAdapterModuleData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Could not add resource adapter module gbean to context", e);
        }

        //construct the bogus resource adapter and jca resource placeholders
        GBeanData resourceAdapterData = new GBeanData(resourceAdapterjsr77Name, ResourceAdapterImplGBean.GBEAN_INFO);
        resourceAdapterData.setReferencePattern("JCAResource", jcaResourcejsr77Name);
        try {
            earContext.addGBean(resourceAdapterData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Could not add resource adapter gbean to context", e);
        }

        GBeanData jcaResourceData = new GBeanData(jcaResourcejsr77Name, JCAResourceImplGBean.GBEAN_INFO);
        Map thisModule = new LinkedHashMap(2);
        thisModule .put(NameFactory.J2EE_APPLICATION, resourceAdapterModuleName.getNameProperty(NameFactory.J2EE_APPLICATION));
        thisModule .put(NameFactory.RESOURCE_ADAPTER_MODULE, resourceAdapterModuleName.getNameProperty(NameFactory.J2EE_NAME));
        jcaResourceData.setReferencePattern("ConnectionFactories", new AbstractNameQuery(resourceAdapterModuleName.getArtifact(), thisModule, JCAConnectionFactory.class.getName()));
        jcaResourceData.setReferencePattern("ResourceAdapters", new AbstractNameQuery(resourceAdapterModuleName.getArtifact(), thisModule, JCAResourceAdapter.class.getName()));
        jcaResourceData.setReferencePattern("AdminObjects", new AbstractNameQuery(resourceAdapterModuleName.getArtifact(), thisModule, JCAAdminObject.class.getName()));

        try {
            earContext.addGBean(jcaResourceData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Could not add jca resource gbean to context", e);
        }

        GerConnectorType geronimoConnector = (GerConnectorType) module.getVendorDD();

        serviceBuilders.build(geronimoConnector, earContext, earContext);

        addConnectorGBeans(earContext, jcaResourcejsr77Name, resourceAdapterModuleData, connector, geronimoConnector, cl);

    }

    public void addGBeans(EARContext earContext, Module module, ClassLoader cl, Collection repository) throws DeploymentException {
        //all our gbeans are added in  the initContext step
    }

    public String getSchemaNamespace() {
        return GERCONNECTOR_NAMESPACE;
    }

    private void addConnectorGBeans(EARContext earContext, AbstractName jcaResourceName, GBeanData resourceAdapterModuleData, ConnectorType connector, GerConnectorType geronimoConnector, ClassLoader cl) throws DeploymentException {
        ResourceadapterType resourceadapter = connector.getResourceadapter();

        GerResourceadapterType[] geronimoResourceAdapters = geronimoConnector.getResourceadapterArray();
        for (int k = 0; k < geronimoResourceAdapters.length; k++) {
            GerResourceadapterType geronimoResourceAdapter = geronimoResourceAdapters[k];

            // Resource Adapter
            AbstractName resourceAdapterAbstractName = null;
            if (resourceadapter.isSetResourceadapterClass()) {
                GBeanData resourceAdapterGBeanData = locateResourceAdapterGBeanData(resourceAdapterModuleData);
                GBeanData resourceAdapterInstanceGBeanData = new GBeanData(resourceAdapterGBeanData);

                setDynamicGBeanDataAttributes(resourceAdapterInstanceGBeanData, geronimoResourceAdapter.getResourceadapterInstance().getConfigPropertySettingArray(), cl);

                // set the work manager name
                AbstractNameQuery workManagerName = ENCConfigBuilder.getGBeanQuery(NameFactory.JCA_WORK_MANAGER, geronimoResourceAdapter.getResourceadapterInstance().getWorkmanager());
                resourceAdapterInstanceGBeanData.setReferencePattern("WorkManager", workManagerName);

                // set the xa terminator name which is the same as our transaction manager
                resourceAdapterInstanceGBeanData.setReferencePattern("XATerminator", earContext.getTransactionManagerObjectName());

                String resourceAdapterName = geronimoResourceAdapter.getResourceadapterInstance().getResourceadapterName();
                resourceAdapterAbstractName = earContext.getNaming().createChildName(jcaResourceName, resourceAdapterName, NameFactory.JCA_RESOURCE_ADAPTER);
                resourceAdapterInstanceGBeanData.setAbstractName(resourceAdapterAbstractName);
                try {
                    earContext.addGBean(resourceAdapterInstanceGBeanData);
                } catch (GBeanAlreadyExistsException e) {
                    throw new DeploymentException("Could not add resource adapter instance gbean to context", e);
                }
            }

            // Outbound Managed Connection Factories (think JDBC data source or JMS connection factory)

            // ManagedConnectionFactory setup
            if (geronimoResourceAdapter.isSetOutboundResourceadapter()) {
                if (!resourceadapter.isSetOutboundResourceadapter()) {
                    throw new DeploymentException("Geronimo plan configures an outbound resource adapter but ra.xml does not describe any");
                }
                String transactionSupport = resourceadapter.getOutboundResourceadapter().getTransactionSupport().getStringValue().trim();
                for (int i = 0; i < geronimoResourceAdapter.getOutboundResourceadapter().getConnectionDefinitionArray().length; i++)
                {
                    GerConnectionDefinitionType geronimoConnectionDefinition = geronimoResourceAdapter.getOutboundResourceadapter().getConnectionDefinitionArray(i);
                    assert geronimoConnectionDefinition != null: "Null GeronimoConnectionDefinition";

                    String connectionFactoryInterfaceName = geronimoConnectionDefinition.getConnectionfactoryInterface().trim();
                    GBeanData connectionFactoryGBeanData = locateConnectionFactoryInfo(resourceAdapterModuleData, connectionFactoryInterfaceName);

                    if (connectionFactoryGBeanData == null) {
                        throw new DeploymentException("No connection definition for ConnectionFactory class: " + connectionFactoryInterfaceName);
                    }

                    for (int j = 0; j < geronimoConnectionDefinition.getConnectiondefinitionInstanceArray().length; j++)
                    {
                        GerConnectiondefinitionInstanceType connectionfactoryInstance = geronimoConnectionDefinition.getConnectiondefinitionInstanceArray()[j];

                        addOutboundGBeans(earContext, jcaResourceName, resourceAdapterAbstractName, connectionFactoryGBeanData, connectionfactoryInstance, transactionSupport, cl);
                    }
                }
            }
        }
        // admin objects (think message queues and topics)

        // add configured admin objects
        for (int i = 0; i < geronimoConnector.getAdminobjectArray().length; i++) {
            GerAdminobjectType gerAdminObject = geronimoConnector.getAdminobjectArray()[i];

            String adminObjectInterface = gerAdminObject.getAdminobjectInterface().trim();
            GBeanData adminObjectGBeanData = locateAdminObjectInfo(resourceAdapterModuleData, adminObjectInterface);

            if (adminObjectGBeanData == null) {
                throw new DeploymentException("No admin object declared for interface: " + adminObjectInterface);
            }

            for (int j = 0; j < gerAdminObject.getAdminobjectInstanceArray().length; j++) {
                GBeanData adminObjectInstanceGBeanData = new GBeanData(adminObjectGBeanData);
                GerAdminobjectInstanceType gerAdminObjectInstance = gerAdminObject.getAdminobjectInstanceArray()[j];
                setDynamicGBeanDataAttributes(adminObjectInstanceGBeanData, gerAdminObjectInstance.getConfigPropertySettingArray(), cl);
                // add it
                AbstractName adminObjectAbstractName = earContext.getNaming().createChildName(jcaResourceName, gerAdminObjectInstance.getMessageDestinationName().trim(), NameFactory.JCA_ADMIN_OBJECT);
                adminObjectInstanceGBeanData.setAbstractName(adminObjectAbstractName);
                try {
                    earContext.addGBean(adminObjectInstanceGBeanData);
                } catch (GBeanAlreadyExistsException e) {
                    throw new DeploymentException("Could not add admin object gbean to context", e);
                }
            }
        }
    }

    private Map getActivationSpecInfoMap(MessagelistenerType[] messagelistenerArray, ClassLoader cl) throws DeploymentException {
        Map activationSpecInfos = new HashMap();
        for (int i = 0; i < messagelistenerArray.length; i++) {
            MessagelistenerType messagelistenerType = messagelistenerArray[i];
            String messageListenerInterface = messagelistenerType.getMessagelistenerType().getStringValue().trim();
            ActivationspecType activationspec = messagelistenerType.getActivationspec();
            String activationSpecClassName = activationspec.getActivationspecClass().getStringValue().trim();
            GBeanInfoBuilder infoBuilder = new GBeanInfoBuilder(ActivationSpecWrapperGBean.class, ActivationSpecWrapperGBean.GBEAN_INFO);

            //add all javabean properties that have both getter and setter.  Ignore the "required" flag from the dd.
            Map getters = new HashMap();
            Set setters = new HashSet();
            Method[] methods;
            try {
                Class activationSpecClass = cl.loadClass(activationSpecClassName);
                methods = activationSpecClass.getMethods();
            } catch (ClassNotFoundException e) {
                throw new DeploymentException("Can not load activation spec class", e);
            }
            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];
                String methodName = method.getName();
                if ((methodName.startsWith("get") || methodName.startsWith("is")) && method.getParameterTypes().length == 0)
                {
                    String attributeName = (methodName.startsWith("get")) ? methodName.substring(3) : methodName.substring(2);
                    getters.put(Introspector.decapitalize(attributeName), method.getReturnType().getName());
                } else if (methodName.startsWith("set") && method.getParameterTypes().length == 1) {
                    setters.add(Introspector.decapitalize(methodName.substring(3)));
                }
            }
            getters.keySet().retainAll(setters);
            getters.remove("resourceAdapter");

            for (Iterator iterator = getters.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                infoBuilder.addAttribute(new DynamicGAttributeInfo((String) entry.getKey(), (String) entry.getValue(), true, true, true, true));
            }

            GBeanInfo gbeanInfo = infoBuilder.getBeanInfo();
            try {
                //make sure the class is available, but we don't use it.
                cl.loadClass(activationSpecClassName);
            } catch (ClassNotFoundException e) {
                throw new DeploymentException("Could not load ActivationSpec class", e);
            }
            GBeanData activationSpecInfo = new GBeanData(gbeanInfo);
            activationSpecInfo.setAttribute("activationSpecClass", activationSpecClassName);
            activationSpecInfos.put(messageListenerInterface, activationSpecInfo);
        }
        return activationSpecInfos;
    }

    private Map getManagedConnectionFactoryInfoMap(ConnectionDefinitionType[] connectionDefinitionArray, ClassLoader cl) throws DeploymentException {
        Map managedConnectionFactoryInfos = new HashMap();
        for (int i = 0; i < connectionDefinitionArray.length; i++) {
            ConnectionDefinitionType connectionDefinition = connectionDefinitionArray[i];

            GBeanInfoBuilder managedConnectionFactoryInfoBuilder = new GBeanInfoBuilder(ManagedConnectionFactoryWrapper.class, ManagedConnectionFactoryWrapperGBean.GBEAN_INFO);
            GBeanData managedConnectionFactoryGBeanData = setUpDynamicGBean(managedConnectionFactoryInfoBuilder, connectionDefinition.getConfigPropertyArray(), cl);

            // set the standard properties
            String connectionfactoryInterface = connectionDefinition.getConnectionfactoryInterface().getStringValue().trim();
            managedConnectionFactoryGBeanData.setAttribute("managedConnectionFactoryClass", connectionDefinition.getManagedconnectionfactoryClass().getStringValue().trim());
            managedConnectionFactoryGBeanData.setAttribute("connectionFactoryInterface", connectionfactoryInterface);
            managedConnectionFactoryGBeanData.setAttribute("connectionFactoryImplClass", connectionDefinition.getConnectionfactoryImplClass().getStringValue().trim());
            managedConnectionFactoryGBeanData.setAttribute("connectionInterface", connectionDefinition.getConnectionInterface().getStringValue().trim());
            managedConnectionFactoryGBeanData.setAttribute("connectionImplClass", connectionDefinition.getConnectionImplClass().getStringValue().trim());
            managedConnectionFactoryInfos.put(connectionfactoryInterface, managedConnectionFactoryGBeanData);
        }
        return managedConnectionFactoryInfos;
    }

    private Map getAdminObjectInfoMap(AdminobjectType[] adminobjectArray, ClassLoader cl) throws DeploymentException {
        Map adminObjectInfos = new HashMap();
        for (int i = 0; i < adminobjectArray.length; i++) {
            AdminobjectType adminObject = adminobjectArray[i];

            GBeanInfoBuilder adminObjectInfoBuilder = new GBeanInfoBuilder(AdminObjectWrapper.class, AdminObjectWrapperGBean.GBEAN_INFO);
            GBeanData adminObjectGBeanData = setUpDynamicGBean(adminObjectInfoBuilder, adminObject.getConfigPropertyArray(), cl);

            // set the standard properties
            String adminObjectInterface = adminObject.getAdminobjectInterface().getStringValue().trim();
            adminObjectGBeanData.setAttribute("adminObjectInterface", adminObjectInterface);
            adminObjectGBeanData.setAttribute("adminObjectClass", adminObject.getAdminobjectClass().getStringValue().trim());
            adminObjectInfos.put(adminObjectInterface, adminObjectGBeanData);
        }
        return adminObjectInfos;
    }


    private GBeanData setUpDynamicGBean(GBeanInfoBuilder infoBuilder, ConfigPropertyType[] configProperties, ClassLoader cl) throws DeploymentException {
        for (int i = 0; i < configProperties.length; i++) {
            infoBuilder.addAttribute(new DynamicGAttributeInfo(configProperties[i].getConfigPropertyName().getStringValue().trim(), configProperties[i].getConfigPropertyType().getStringValue().trim(), true, true, true, true));
        }

        GBeanInfo gbeanInfo = infoBuilder.getBeanInfo();
        GBeanData gbeanData = new GBeanData(gbeanInfo);
        for (int i = 0; i < configProperties.length; i++) {
            if (configProperties[i].isSetConfigPropertyValue()) {
                gbeanData.setAttribute(configProperties[i].getConfigPropertyName().getStringValue(),
                        getValue(configProperties[i].getConfigPropertyType().getStringValue(),
                                configProperties[i].getConfigPropertyValue().getStringValue(),
                                cl));
            }
        }
        return gbeanData;
    }

    private void setDynamicGBeanDataAttributes(GBeanData gbeanData, GerConfigPropertySettingType[] configProperties, ClassLoader cl) throws DeploymentException {

        try {
            for (int i = 0; i < configProperties.length; i++) {
                String name = configProperties[i].getName();
                GAttributeInfo attributeInfo = gbeanData.getGBeanInfo().getAttribute(name);
                if (attributeInfo == null) {
                    throw new DeploymentException("The plan is trying to set attribute: " + name + " which does not exist.  Known attributs are: " + gbeanData.getGBeanInfo().getAttributes());
                }
                String type = attributeInfo.getType();
                gbeanData.setAttribute(name,
                        getValue(type, configProperties[i].getStringValue().trim(), cl));
            }
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }

    private Object getValue(String type, String value, ClassLoader cl) throws DeploymentException {
        if (value == null) {
            return null;
        }

        Class clazz;
        try {
            clazz = cl.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new DeploymentException("Could not load attribute class:  type: " + type, e);
        }

        // Handle numeric fields with no value set
        if (value.equals("")) {
            if (Number.class.isAssignableFrom(clazz) || Date.class.isAssignableFrom(clazz)) {
                return null;
            }
        }

        PropertyEditor editor = PropertyEditors.getEditor(clazz);
        editor.setAsText(value);
        return editor.getValue();
    }

    private AbstractName configureConnectionManager(EARContext earContext, AbstractName jcaResourceName, String ddTransactionSupport, GerConnectiondefinitionInstanceType connectionfactoryInstance, ClassLoader cl) throws DeploymentException {
//        if (connectionfactoryInstance.getConnectionmanagerRef() != null) {
        //we don't configure anything, just use the supplied gbean
//            try {
//                return AbstractName.getInstance(connectionfactoryInstance.getConnectionmanagerRef());
//            } catch (MalformedAbstractNameException e) {
//                throw new DeploymentException("Invalid AbstractName string supplied for ConnectionManager reference", e);
//            }
//        }

        // create the object name for our connection manager
        AbstractName connectionManagerAbstractName = earContext.getNaming().createChildName(jcaResourceName, connectionfactoryInstance.getName().trim(), NameFactory.JCA_CONNECTION_MANAGER);

        // create the data holder for our connection manager
        GBeanInfo gbeanInfo;
        try {
            gbeanInfo = GBeanInfo.getGBeanInfo("org.apache.geronimo.connector.outbound.GenericConnectionManagerGBean", cl);
        } catch (InvalidConfigurationException e) {
            throw new DeploymentException("Unable to create GMBean", e);
        }
        GBeanData connectionManagerGBean = new GBeanData(connectionManagerAbstractName, gbeanInfo);

        //we configure our connection manager
        GerConnectionmanagerType connectionManager = connectionfactoryInstance.getConnectionmanager();
        TransactionSupport transactionSupport;
        if (connectionManager.isSetNoTransaction()) {
            transactionSupport = NoTransactions.INSTANCE;
        } else if (connectionManager.isSetLocalTransaction()) {
            if ("NoTransaction".equals(ddTransactionSupport)) {
                throw new DeploymentException("You are requesting local transaction support for a connector that does not support transactions: named: " + connectionfactoryInstance.getName().trim());
            }
            transactionSupport = LocalTransactions.INSTANCE;
        } else if (connectionManager.isSetTransactionLog()) {
            if ("NoTransaction".equals(ddTransactionSupport)) {
                throw new DeploymentException("You are requesting local transaction support for a connector that does not support transactions: named: " + connectionfactoryInstance.getName().trim());
            }
            transactionSupport = TransactionLog.INSTANCE;
        } else if (connectionManager.isSetXaTransaction()) {
            if ("NoTransaction".equals(ddTransactionSupport)) {
                throw new DeploymentException("You are requesting xa transaction support for a connector that does not support transactions: named: " + connectionfactoryInstance.getName().trim());
            }
            if ("LocalTransaction".equals(ddTransactionSupport)) {
                throw new DeploymentException("You are requesting xa transaction support for a connector that supports only local transactions: named: " + connectionfactoryInstance.getName().trim());
            }
            transactionSupport = new XATransactions(connectionManager.getXaTransaction().isSetTransactionCaching(),
                    connectionManager.getXaTransaction().isSetThreadCaching());
        } else if ("NoTransaction".equals(ddTransactionSupport)) {
            transactionSupport = NoTransactions.INSTANCE;
        } else if ("LocalTransaction".equals(ddTransactionSupport)) {
            transactionSupport = LocalTransactions.INSTANCE;
        } else if ("XATransaction".equals(ddTransactionSupport)) {
            transactionSupport = new XATransactions(defaultXATransactionCaching, defaultXAThreadCaching);
        } else {
            //this should not happen
            throw new DeploymentException("Unexpected transaction support element in connector named: " + connectionfactoryInstance.getName().trim());
        }
        PoolingSupport pooling;
        if (connectionManager.getSinglePool() != null) {
            GerSinglepoolType pool = connectionManager.getSinglePool();

            pooling = new SinglePool(pool.isSetMaxSize() ? pool.getMaxSize() : defaultMaxSize,
                    pool.isSetMinSize() ? pool.getMinSize() : defaultMinSize,
                    pool.isSetBlockingTimeoutMilliseconds() ? pool.getBlockingTimeoutMilliseconds() : defaultBlockingTimeoutMilliseconds,
                    pool.isSetIdleTimeoutMinutes() ? pool.getIdleTimeoutMinutes() : defaultIdleTimeoutMinutes,
                    pool.getMatchOne() != null,
                    pool.getMatchAll() != null,
                    pool.getSelectOneAssumeMatch() != null);
        } else if (connectionManager.getPartitionedPool() != null) {
            GerPartitionedpoolType pool = connectionManager.getPartitionedPool();
            pooling = new PartitionedPool(pool.isSetMaxSize() ? pool.getMaxSize() : defaultMaxSize,
                    pool.isSetMinSize() ? pool.getMinSize() : defaultMinSize,
                    pool.isSetBlockingTimeoutMilliseconds() ? pool.getBlockingTimeoutMilliseconds() : defaultBlockingTimeoutMilliseconds,
                    pool.isSetIdleTimeoutMinutes() ? pool.getIdleTimeoutMinutes() : defaultIdleTimeoutMinutes,
                    pool.getMatchOne() != null,
                    pool.getMatchAll() != null,
                    pool.getSelectOneAssumeMatch() != null,
                    pool.isSetPartitionByConnectionrequestinfo(),
                    pool.isSetPartitionBySubject());
        } else if (connectionManager.getNoPool() != null) {
            pooling = new NoPool();
        } else {
            throw new DeploymentException("Unexpected pooling support element in connector named " + connectionfactoryInstance.getName().trim());
        }
        try {
            connectionManagerGBean.setAttribute("transactionSupport", transactionSupport);
            connectionManagerGBean.setAttribute("pooling", pooling);
            connectionManagerGBean.setReferencePattern("ConnectionTracker", earContext.getConnectionTrackerObjectName());
            connectionManagerGBean.setAttribute("containerManagedSecurity", Boolean.valueOf(connectionManager.isSetContainerManagedSecurity()));
            connectionManagerGBean.setReferencePattern("TransactionManager", earContext.getTransactionManagerObjectName());
        } catch (Exception e) {
            throw new DeploymentException("Problem setting up ConnectionManager named " + connectionfactoryInstance.getName().trim(), e);
        }

        try {
            earContext.addGBean(connectionManagerGBean);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Could not add connection manager gbean to context: name: " + connectionfactoryInstance.getName().trim(), e);
        }
        return connectionManagerAbstractName;
    }

    private void addOutboundGBeans(EARContext earContext, AbstractName jcaResourceName, AbstractName resourceAdapterAbstractName, GBeanData managedConnectionFactoryPrototypeGBeanData, GerConnectiondefinitionInstanceType connectiondefinitionInstance, String transactionSupport, ClassLoader cl) throws DeploymentException {
        GBeanData managedConnectionFactoryInstanceGBeanData = new GBeanData(managedConnectionFactoryPrototypeGBeanData);
        AbstractName connectionFactoryAbstractName = earContext.getNaming().createChildName(jcaResourceName, connectiondefinitionInstance.getName().trim(), NameFactory.JCA_CONNECTION_FACTORY);
        AbstractName managedConnectionFactoryAbstractName = earContext.getNaming().createChildName(connectionFactoryAbstractName, connectiondefinitionInstance.getName().trim(), NameFactory.JCA_MANAGED_CONNECTION_FACTORY);
        // ConnectionManager
        AbstractName connectionManagerAbstractName = configureConnectionManager(earContext, managedConnectionFactoryAbstractName, transactionSupport, connectiondefinitionInstance, cl);

        // ManagedConnectionFactory
        setDynamicGBeanDataAttributes(managedConnectionFactoryInstanceGBeanData, connectiondefinitionInstance.getConfigPropertySettingArray(), cl);
        try {
            if (resourceAdapterAbstractName != null) {
                managedConnectionFactoryInstanceGBeanData.setReferencePattern("ResourceAdapterWrapper", resourceAdapterAbstractName);
            }
            managedConnectionFactoryInstanceGBeanData.setReferencePattern("ConnectionManagerContainer", connectionManagerAbstractName);
            //additional interfaces implemented by connection factory
            String[] implementedInterfaces = connectiondefinitionInstance.getImplementedInterfaceArray();
            if (implementedInterfaces != null) {
                for (int i = 0; i < implementedInterfaces.length; i++) {
                    implementedInterfaces[i] = implementedInterfaces[i].trim();
                }
            } else {
                implementedInterfaces = new String[0];
            }
            managedConnectionFactoryInstanceGBeanData.setAttribute("implementedInterfaces", implementedInterfaces);

        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        managedConnectionFactoryInstanceGBeanData.setAbstractName(managedConnectionFactoryAbstractName);
        try {
            earContext.addGBean(managedConnectionFactoryInstanceGBeanData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Could not add managed connection factory gbean to context", e);
        }

        // ConnectionFactory
        GBeanData connectionFactoryGBeanData = new GBeanData(connectionFactoryAbstractName, JCAConnectionFactoryImplGBean.GBEAN_INFO);
        connectionFactoryGBeanData.setReferencePattern("JCAManagedConnectionFactory", managedConnectionFactoryAbstractName);

        try {
            earContext.addGBean(connectionFactoryGBeanData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Could not add connection factory gbean to context", e);
        }
    }

    //ResourceReferenceBuilder implementation
    public Reference createResourceRef(AbstractNameQuery containerId, Class iface, Configuration configuration) throws DeploymentException {
        try {
            configuration.findGBean(containerId);
        } catch (GBeanNotFoundException e) {
            throw new UnresolvedReferenceException("Resource", false, containerId.toString(), configuration.getId().toString());
        }
        return new ResourceReference(configuration.getId(), containerId, iface);
    }

    public Reference createAdminObjectRef(AbstractNameQuery containerId, Class iface, Configuration configuration) throws DeploymentException {
        try {
            configuration.findGBean(containerId);
        } catch (GBeanNotFoundException e) {
            throw new DeploymentException("Can not resolve admin object ref " + containerId + " in configuration " + configuration.getId());
        }
        return new ResourceReference(configuration.getId(), containerId, iface);
    }

    public GBeanData locateActivationSpecInfo(AbstractNameQuery resourceAdapterInstanceQuery, String messageListenerInterface, Configuration configuration) throws DeploymentException {
        //First, locate the module gbean from the JCAResourceAdapter instance
        AbstractName instanceName;
        try {
            instanceName = configuration.findGBean(resourceAdapterInstanceQuery);
        } catch (GBeanNotFoundException e) {
            throw new DeploymentException("No resource adapter instance gbean found matching " + resourceAdapterInstanceQuery + " from configuration " + configuration.getId());
        }
        String moduleName = (String) instanceName.getName().get(NameFactory.RESOURCE_ADAPTER_MODULE);
        Map moduleNameMap = new HashMap(instanceName.getName());
        moduleNameMap.remove(NameFactory.JCA_RESOURCE);
        moduleNameMap.remove(NameFactory.RESOURCE_ADAPTER);
        moduleNameMap.remove(NameFactory.RESOURCE_ADAPTER_MODULE);
        moduleNameMap.put(NameFactory.J2EE_TYPE, NameFactory.RESOURCE_ADAPTER_MODULE);
        moduleNameMap.put(NameFactory.J2EE_NAME, moduleName);
        AbstractNameQuery nameQuery = new AbstractNameQuery(instanceName.getArtifact(), moduleNameMap, ResourceAdapterModule.class.getName());
        //now find the gbeandata and extract the activation spec info.
        GBeanData resourceModuleData;
        try {
            resourceModuleData = configuration.findGBeanData(nameQuery);
        } catch (GBeanNotFoundException e) {
            throw new DeploymentException("No resource module gbean found matching " + nameQuery + " from configuration " + configuration.getId());
        }
        Map activationSpecInfos = (Map) resourceModuleData.getAttribute("activationSpecInfoMap");
        if (activationSpecInfos == null) {
            throw new DeploymentException("No activation spec info map found in resource adapter module: " + resourceModuleData.getAbstractName());
        }
        return (GBeanData) activationSpecInfos.get(messageListenerInterface);
    }

    private GBeanData locateResourceAdapterGBeanData(GBeanData resourceAdapterModuleData) throws DeploymentException {
        GBeanData data = (GBeanData) resourceAdapterModuleData.getAttribute("resourceAdapterGBeanData");
        if (data == null) {
            throw new DeploymentException("No resource adapter info found for resource adapter module: " + resourceAdapterModuleData.getAbstractName());
        }
        return data;
    }

    private GBeanData locateAdminObjectInfo(GBeanData resourceAdapterModuleData, String adminObjectInterfaceName) throws DeploymentException {
        Map adminObjectInfos = (Map) resourceAdapterModuleData.getAttribute("adminObjectInfoMap");
        if (adminObjectInfos == null) {
            throw new DeploymentException("No admin object infos found for resource adapter module: " + resourceAdapterModuleData.getAbstractName());
        }
        return (GBeanData) adminObjectInfos.get(adminObjectInterfaceName);
    }

    private GBeanData locateConnectionFactoryInfo(GBeanData resourceAdapterModuleData, String connectionFactoryInterfaceName) throws DeploymentException {
        Map managedConnectionFactoryInfos = (Map) resourceAdapterModuleData.getAttribute("managedConnectionFactoryInfoMap");
        if (managedConnectionFactoryInfos == null) {
            throw new DeploymentException("No managed connection factory infos found for resource adapter module: " + resourceAdapterModuleData.getAbstractName());
        }
        return (GBeanData) managedConnectionFactoryInfos.get(connectionFactoryInterfaceName);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(ConnectorModuleBuilder.class, NameFactory.MODULE_BUILDER);

        infoBuilder.addAttribute("defaultEnvironment", Environment.class, true, true);
        infoBuilder.addAttribute("defaultMaxSize", int.class, true, true);
        infoBuilder.addAttribute("defaultMinSize", int.class, true, true);
        infoBuilder.addAttribute("defaultBlockingTimeoutMilliseconds", int.class, true, true);
        infoBuilder.addAttribute("defaultIdleTimeoutMinutes", int.class, true, true);
        infoBuilder.addAttribute("defaultXATransactionCaching", boolean.class, true, true);
        infoBuilder.addAttribute("defaultXAThreadCaching", boolean.class, true, true);

        infoBuilder.addReference("ServiceBuilders", NamespaceDrivenBuilder.class, NameFactory.MODULE_BUILDER);

        infoBuilder.addInterface(ModuleBuilder.class);
        infoBuilder.addInterface(ResourceReferenceBuilder.class);

        infoBuilder.setConstructor(new String[]{"defaultEnvironment",
                "defaultMaxSize",
                "defaultMinSize",
                "defaultBlockingTimeoutMilliseconds",
                "defaultIdleTimeoutMinutes",
                "defaultXATransactionCaching",
                "defaultXAThreadCaching",
                "ServiceBuilders"});
        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
