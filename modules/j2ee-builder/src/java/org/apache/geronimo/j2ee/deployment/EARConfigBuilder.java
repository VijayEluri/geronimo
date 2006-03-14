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
package org.apache.geronimo.j2ee.deployment;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.ConfigurationBuilder;
import org.apache.geronimo.deployment.service.EnvironmentBuilder;
import org.apache.geronimo.deployment.service.ServiceConfigBuilder;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.deployment.util.NestedJarFile;
import org.apache.geronimo.deployment.xbeans.ArtifactType;
import org.apache.geronimo.deployment.xbeans.EnvironmentType;
import org.apache.geronimo.deployment.xbeans.GbeanType;
import org.apache.geronimo.deployment.xmlbeans.XmlBeansUtil;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.ReferencePatterns;
import org.apache.geronimo.j2ee.ApplicationInfo;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.j2ee.management.impl.J2EEApplicationImpl;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.config.ConfigurationAlreadyExistsException;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.Repository;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.security.deployment.SecurityBuilder;
import org.apache.geronimo.security.deployment.SecurityConfiguration;
import org.apache.geronimo.xbeans.geronimo.j2ee.GerApplicationDocument;
import org.apache.geronimo.xbeans.geronimo.j2ee.GerApplicationType;
import org.apache.geronimo.xbeans.geronimo.j2ee.GerExtModuleType;
import org.apache.geronimo.xbeans.geronimo.j2ee.GerModuleType;
import org.apache.geronimo.xbeans.j2ee.ApplicationType;
import org.apache.geronimo.xbeans.j2ee.ModuleType;
import org.apache.geronimo.management.J2EEServer;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @version $Rev:385232 $ $Date$
 */
public class EARConfigBuilder implements ConfigurationBuilder {

    private final static QName APPLICATION_QNAME = GerApplicationDocument.type.getDocumentElementName();

    private final Kernel kernel;
    private final Repository repository;
    private final ModuleBuilder ejbConfigBuilder;
    private final ModuleBuilder webConfigBuilder;
    private final ModuleBuilder connectorConfigBuilder;
    private final ModuleBuilder appClientConfigBuilder;
    private final EJBReferenceBuilder ejbReferenceBuilder;
    private final ResourceReferenceBuilder resourceReferenceBuilder;
    private final ServiceReferenceBuilder serviceReferenceBuilder;

    private final Environment defaultEnvironment;
    //TODO configid FIXME
    private final AbstractNameQuery serverName = null;
    private final AbstractNameQuery transactionContextManagerObjectName;
    private final AbstractNameQuery connectionTrackerObjectName;
    private final AbstractNameQuery transactionalTimerObjectName;
    private final AbstractNameQuery nonTransactionalTimerObjectName;
    private final AbstractNameQuery corbaGBeanObjectName;
    private static final String DEFAULT_GROUPID = "defaultGroupId";


    public EARConfigBuilder(Environment defaultEnvironment,
            AbstractNameQuery transactionContextManagerAbstractName,
            AbstractNameQuery connectionTrackerAbstractName,
            AbstractNameQuery transactionalTimerAbstractName,
            AbstractNameQuery nonTransactionalTimerAbstractName,
            AbstractNameQuery corbaGBeanAbstractName,
            Repository repository,
            ModuleBuilder ejbConfigBuilder,
            EJBReferenceBuilder ejbReferenceBuilder,
            ModuleBuilder webConfigBuilder,
            ModuleBuilder connectorConfigBuilder,
            ResourceReferenceBuilder resourceReferenceBuilder,
            ModuleBuilder appClientConfigBuilder,
            ServiceReferenceBuilder serviceReferenceBuilder,
            Kernel kernel) {
        this.kernel = kernel;
        this.repository = repository;
        this.defaultEnvironment = defaultEnvironment;

        this.ejbConfigBuilder = ejbConfigBuilder;
        this.ejbReferenceBuilder = ejbReferenceBuilder;
        this.resourceReferenceBuilder = resourceReferenceBuilder;
        this.webConfigBuilder = webConfigBuilder;
        this.connectorConfigBuilder = connectorConfigBuilder;
        this.appClientConfigBuilder = appClientConfigBuilder;
        this.serviceReferenceBuilder = serviceReferenceBuilder;
        this.transactionContextManagerObjectName = transactionContextManagerAbstractName;
        this.connectionTrackerObjectName = connectionTrackerAbstractName;
        this.transactionalTimerObjectName = transactionalTimerAbstractName;
        this.nonTransactionalTimerObjectName = nonTransactionalTimerAbstractName;
        this.corbaGBeanObjectName = corbaGBeanAbstractName;
    }

    public Object getDeploymentPlan(File planFile, JarFile jarFile) throws DeploymentException {
        if (planFile == null && jarFile == null) {
            return null;
        }
        ApplicationInfo plan = getEarPlan(planFile, jarFile);
        if (plan != null) {
            return plan;
        }
        //Only "synthetic" ears with only external modules can have no jar file.
        if (jarFile == null) {
            return null;
        }

        // get the modules either the application plan or for a stand alone module from the specific deployer
        Module module = null;
        if (webConfigBuilder != null) {
            module = webConfigBuilder.createModule(planFile, jarFile);
        }
        if (module == null && ejbConfigBuilder != null) {
            module = ejbConfigBuilder.createModule(planFile, jarFile);
        }
        if (module == null && connectorConfigBuilder != null) {
            module = connectorConfigBuilder.createModule(planFile, jarFile);
        }
        if (module == null && appClientConfigBuilder != null) {
            module = appClientConfigBuilder.createModule(planFile, jarFile);
        }
        if (module == null) {
            return null;
        }

        return new ApplicationInfo(module.getType(),
                module.getEnvironment(),
                module.getModuleName(),
                null,
                null,
                Collections.singleton(module),
                Collections.EMPTY_SET,
                null);
    }

    private ApplicationInfo getEarPlan(File planFile, JarFile earFile) throws DeploymentException {
        String specDD;
        ApplicationType application = null;
        if (earFile != null) {
            try {
                URL applicationXmlUrl = DeploymentUtil.createJarURL(earFile, "META-INF/application.xml");
                specDD = DeploymentUtil.readAll(applicationXmlUrl);
            } catch (Exception e) {
                //no application.xml, not for us
                return null;
            }
            //we found something called application.xml in the right place, if we can't parse it it's an error
            try {
                XmlObject xmlObject = XmlBeansUtil.parse(specDD);
                application = SchemaConversionUtils.convertToApplicationSchema(xmlObject).getApplication();
            } catch (XmlException e) {
                throw new DeploymentException("Could not parse application.xml", e);
            }
        }

        GerApplicationType gerApplication = null;
        try {
            // load the geronimo-application.xml from either the supplied plan or from the earFile
            XmlObject rawPlan;
            try {
                if (planFile != null) {
                    rawPlan = XmlBeansUtil.parse(planFile.toURL());
                    gerApplication = (GerApplicationType) SchemaConversionUtils.fixGeronimoSchema(rawPlan, APPLICATION_QNAME, GerApplicationType.type);
                    if (gerApplication == null) {
                        return null;
                    }
                } else {
                    URL path = DeploymentUtil.createJarURL(earFile, "META-INF/geronimo-application.xml");
                    rawPlan = XmlBeansUtil.parse(path);
                    gerApplication = (GerApplicationType) SchemaConversionUtils.fixGeronimoSchema(rawPlan, APPLICATION_QNAME, GerApplicationType.type);
                }
            } catch (IOException e) {
                //TODO isn't this an error?
            }

            // if we got one extract the validate it otherwise create a default one
            if (gerApplication == null) {
                gerApplication = createDefaultPlan(application, earFile);
            }
        } catch (XmlException e) {
            throw new DeploymentException(e);
        }

        EnvironmentType environmentType = gerApplication.getEnvironment();
        Environment environment = EnvironmentBuilder.buildEnvironment(environmentType, defaultEnvironment);

        Artifact artifact = environment.getConfigId();
        AbstractName earName = Naming.createRootName(artifact, artifact.toString(), NameFactory.J2EE_APPLICATION);

        // get the modules either the application plan or for a stand alone module from the specific deployer
        // todo change module so you can extract the real module path back out.. then we can eliminate
        // the moduleLocations and have addModules return the modules
        Set moduleLocations = new HashSet();
        Set modules = new LinkedHashSet();
        try {
            addModules(earFile, application, gerApplication, moduleLocations, modules, environment, earName);
        } catch (Throwable e) {
            // close all the modules
            for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
                Module module = (Module) iterator.next();
                module.close();
            }

            if (e instanceof DeploymentException) {
                throw (DeploymentException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else if (e instanceof Error) {
                throw (Error) e;
            }
            throw new DeploymentException(e);
        }

        return new ApplicationInfo(ConfigurationModuleType.EAR,
                environment,
                earName,
                application,
                gerApplication,
                modules,
                moduleLocations,
                application == null ? null : application.toString());
    }


    private GerApplicationType createDefaultPlan(ApplicationType application, JarFile module) {
        // construct the empty geronimo-application.xml
        GerApplicationType gerApplication = GerApplicationType.Factory.newInstance();
        EnvironmentType environmentType = gerApplication.addNewEnvironment();
        ArtifactType artifactType = environmentType.addNewConfigId();

        artifactType.setGroupId(DEFAULT_GROUPID);

        // set the configId
        String id = application.getId();
        if (id == null) {
            File fileName = new File(module.getName());
            id = fileName.getName();
            if (id.endsWith(".ear")) {
                id = id.substring(0, id.length() - 4);
            }
            if (id.endsWith("/")) {
                id = id.substring(0, id.length() - 1);
            }
        }

        artifactType.setArtifactId(id);
        artifactType.setVersion("" + System.currentTimeMillis());
        artifactType.setType("car");
        return gerApplication;
    }

    public Artifact getConfigurationID(Object plan, JarFile module) throws IOException, DeploymentException {
        ApplicationInfo applicationInfo = (ApplicationInfo) plan;
        return applicationInfo.getEnvironment().getConfigId();
    }

    public ConfigurationData buildConfiguration(Object plan, JarFile earFile, ConfigurationStore configurationStore) throws IOException, DeploymentException {
        assert plan != null;
        ApplicationInfo applicationInfo = (ApplicationInfo) plan;
        try {
            // Create the output ear context
            EARContext earContext;
            ConfigurationModuleType applicationType = applicationInfo.getType();
            Environment environment = applicationInfo.getEnvironment();
            Artifact configId = environment.getConfigId();
            File configurationDir;
            try {
                configurationDir = configurationStore.createNewConfigurationDir(configId);
            } catch (ConfigurationAlreadyExistsException e) {
                throw new DeploymentException(e);
            }
            earContext = new EARContext(configurationDir,
                    applicationInfo.getEnvironment(),
                    applicationType,
                    kernel,
                    serverName,
                    applicationInfo.getBaseName(),
                    transactionContextManagerObjectName,
                    connectionTrackerObjectName,
                    transactionalTimerObjectName,
                    nonTransactionalTimerObjectName,
                    corbaGBeanObjectName,
                    new RefContext(ejbReferenceBuilder, resourceReferenceBuilder, serviceReferenceBuilder, kernel));

            // Copy over all files that are _NOT_ modules
            Set moduleLocations = applicationInfo.getModuleLocations();
            if (ConfigurationModuleType.EAR == applicationType && earFile != null) {
                for (Enumeration e = earFile.entries(); e.hasMoreElements();) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    if (!moduleLocations.contains(entry.getName())) {
                        earContext.addFile(URI.create(entry.getName()), earFile, entry);
                    }
                }
            }

            GerApplicationType geronimoApplication = (GerApplicationType) applicationInfo.getVendorDD();

            // each module installs it's files into the output context.. this is different for each module type
            Set modules = applicationInfo.getModules();
            for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
                Module module = (Module) iterator.next();
                getBuilder(module).installModule(earFile, earContext, module, configurationStore, repository);
            }

            // give each module a chance to populate the earContext now that a classloader is available
            ClassLoader cl = earContext.getClassLoader();
            for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
                Module module = (Module) iterator.next();
                getBuilder(module).initContext(earContext, module, cl);
            }

            // add gbeans declared in the geronimo-application.xml
            if (geronimoApplication != null) {
                GbeanType[] gbeans = geronimoApplication.getGbeanArray();
                ServiceConfigBuilder.addGBeans(gbeans, cl, earContext.getModuleName(), earContext);
            }

            // Create the J2EEApplication managed object
            if (ConfigurationModuleType.EAR == applicationType) {
                GBeanData gbeanData = new GBeanData(earContext.getModuleName(), J2EEApplicationImpl.GBEAN_INFO);
                try {
                    String originalSpecDD = applicationInfo.getOriginalSpecDD();
                    if (originalSpecDD == null) {
                        originalSpecDD = "Synthetic EAR";
                    }
                    gbeanData.setAttribute("deploymentDescriptor", originalSpecDD);
                } catch (Exception e) {
                    throw new DeploymentException("Error initializing J2EEApplication managed object");
                }
                gbeanData.setReferencePatterns("j2eeServer", new ReferencePatterns(new AbstractNameQuery(J2EEServer.class.getName())));
                earContext.addGBean(gbeanData);
            }

            //look for application plan security config
            if (geronimoApplication != null && geronimoApplication.isSetSecurity()) {
                SecurityConfiguration securityConfiguration = SecurityBuilder.buildSecurityConfiguration(geronimoApplication.getSecurity(), cl);
                earContext.setSecurityConfiguration(securityConfiguration);
            }

            // each module can now add it's GBeans
            for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
                Module module = (Module) iterator.next();
                getBuilder(module).addGBeans(earContext, module, cl, repository);
            }

            //add the JACC gbean if there is a principal-role mapping
            //TODO configid verify that the jaccManagerName is not needed before this.  cf. how this is handled in 1.2 branch.
            if (earContext.getSecurityConfiguration() != null) {
                GBeanData jaccBeanData = SecurityBuilder.configureApplicationPolicyManager(earContext.getModuleName(), earContext.getContextIDToPermissionsMap(), earContext.getSecurityConfiguration());
                earContext.addGBean(jaccBeanData);
                earContext.setJaccManagerName(jaccBeanData.getAbstractName());
            }
            ConfigurationData configurationData = earContext.getConfigurationData();
            earContext.close();
            return configurationData;
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException(e);
        } finally {
            Set modules = applicationInfo.getModules();
            for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
                Module module = (Module) iterator.next();
                module.close();
            }
        }
    }

    private void addModules(JarFile earFile, ApplicationType application, GerApplicationType gerApplication, Set moduleLocations, Set modules, Environment environment, AbstractName earName) throws DeploymentException {
        Map altVendorDDs = new HashMap();
        try {
            if (earFile != null) {
                ModuleType[] moduleTypes = application.getModuleArray();
                Set paths = new HashSet();
                for (int i = 0; i < moduleTypes.length; i++) {
                    ModuleType type = moduleTypes[i];
                    if (type.isSetEjb()) {
                        paths.add(type.getEjb().getStringValue());
                    } else if (type.isSetWeb()) {
                        paths.add(type.getWeb().getWebUri().getStringValue());
                    } else if (type.isSetConnector()) {
                        paths.add(type.getConnector().getStringValue());
                    } else if (type.isSetJava()) {
                        paths.add(type.getJava().getStringValue());
                    }
                }

                // build map from module path to alt vendor dd
                GerModuleType gerModuleTypes[] = gerApplication.getModuleArray();
                for (int i = 0; i < gerModuleTypes.length; i++) {
                    GerModuleType gerModule = gerModuleTypes[i];
                    String path = null;
                    if (gerModule.isSetEjb()) {
                        path = gerModule.getEjb().getStringValue();
                    } else if (gerModule.isSetWeb()) {
                        path = gerModule.getWeb().getStringValue();
                    } else if (gerModule.isSetConnector()) {
                        path = gerModule.getConnector().getStringValue();
                    } else if (gerModule.isSetJava()) {
                        path = gerModule.getJava().getStringValue();
                    }
                    if (!paths.contains(path)) {
                        throw new DeploymentException("Geronimo deployment plan refers to module '" + path + "' but that was not defined in the META-INF/application.xml");
                    }

                    if (gerModule.isSetAltDd()) {
                        // the the url of the alt dd
                        try {
                            altVendorDDs.put(path, DeploymentUtil.toTempFile(earFile, gerModule.getAltDd().getStringValue()));
                        } catch (IOException e) {
                            throw new DeploymentException("Invalid alt vendor dd url: " + gerModule.getAltDd().getStringValue(), e);
                        }
                    } else {
                        //dd is included explicitly
                        XmlObject[] anys = gerModule.selectChildren(GerModuleType.type.qnameSetForWildcardElements());
                        if (anys.length != 1) {
                            throw new DeploymentException("Unexpected count of xs:any elements in embedded vendor plan " + anys.length + " qnameset: " + GerModuleType.type.qnameSetForWildcardElements());
                        }
                        altVendorDDs.put(path, anys[0]);
                    }
                }

                // get a set containing all of the files in the ear that are actually modules
                for (int i = 0; i < moduleTypes.length; i++) {
                    ModuleType moduleXml = moduleTypes[i];

                    String modulePath;
                    ModuleBuilder builder;

                    Object moduleContextInfo = null;
                    String moduleTypeName;
                    if (moduleXml.isSetEjb()) {
                        modulePath = moduleXml.getEjb().getStringValue();
                        if (ejbConfigBuilder == null) {
                            throw new DeploymentException("Cannot deploy ejb application; No ejb deployer defined: " + modulePath);
                        }
                        builder = ejbConfigBuilder;
                        moduleTypeName = "an EJB";
                    } else if (moduleXml.isSetWeb()) {
                        modulePath = moduleXml.getWeb().getWebUri().getStringValue();
                        if (webConfigBuilder == null) {
                            throw new DeploymentException("Cannot deploy web application; No war deployer defined: " + modulePath);
                        }
                        builder = webConfigBuilder;
                        moduleTypeName = "a war";
                        moduleContextInfo = moduleXml.getWeb().getContextRoot().getStringValue().trim();
                    } else if (moduleXml.isSetConnector()) {
                        modulePath = moduleXml.getConnector().getStringValue();
                        if (connectorConfigBuilder == null) {
                            throw new DeploymentException("Cannot deploy resource adapter; No rar deployer defined: " + modulePath);
                        }
                        builder = connectorConfigBuilder;
                        moduleTypeName = "a connector";
                    } else if (moduleXml.isSetJava()) {
                        modulePath = moduleXml.getJava().getStringValue();
                        if (appClientConfigBuilder == null) {
                            throw new DeploymentException("Cannot deploy app client; No app client deployer defined: " + modulePath);
                        }
                        builder = appClientConfigBuilder;
                        moduleTypeName = "an application client";
                    } else {
                        throw new DeploymentException("Could not find a module builder for module: " + moduleXml);
                    }

                    moduleLocations.add(modulePath);

                    URL altSpecDD = null;
                    if (moduleXml.isSetAltDd()) {
                        try {
                            altSpecDD = DeploymentUtil.createJarURL(earFile, moduleXml.getAltDd().getStringValue());
                        } catch (MalformedURLException e) {
                            throw new DeploymentException("Invalid alt sped dd url: " + moduleXml.getAltDd().getStringValue(), e);
                        }
                    }

                    NestedJarFile moduleFile;
                    try {
                        moduleFile = new NestedJarFile(earFile, modulePath);
                    } catch (IOException e) {
                        throw new DeploymentException("Invalid moduleFile: " + modulePath, e);
                    }

                    Module module = builder.createModule(altVendorDDs.get(modulePath),
                            moduleFile,
                            modulePath,
                            altSpecDD,
                            environment,
                            moduleContextInfo,
                            earName);

                    if (module == null) {
                        throw new DeploymentException("Module was not " + moduleTypeName + ": " + modulePath);
                    }

                    modules.add(module);
                }
            }

            //deploy the extension modules
            GerExtModuleType gerExtModuleTypes[] = gerApplication.getExtModuleArray();
            for (int i = 0; i < gerExtModuleTypes.length; i++) {
                GerExtModuleType gerExtModule = gerExtModuleTypes[i];
                String moduleName;
                ModuleBuilder builder;
                Object moduleContextInfo = null;
                String moduleTypeName;

                if (gerExtModule.isSetEjb()) {
                    moduleName = gerExtModule.getEjb().getStringValue();
                    if (ejbConfigBuilder == null) {
                        throw new DeploymentException("Cannot deploy ejb application; No ejb deployer defined: " + moduleName);
                    }
                    builder = ejbConfigBuilder;
                    moduleTypeName = "an EJB";
                } else if (gerExtModule.isSetWeb()) {
                    moduleName = gerExtModule.getWeb().getStringValue();
                    if (webConfigBuilder == null) {
                        throw new DeploymentException("Cannot deploy web application; No war deployer defined: " + moduleName);
                    }
                    builder = webConfigBuilder;
                    moduleTypeName = "a war";
                    //ext modules must use vendor plan to set context-root
//                    moduleContextInfo = gerExtModule.getWeb().getContextRoot().getStringValue().trim();
                } else if (gerExtModule.isSetConnector()) {
                    moduleName = gerExtModule.getConnector().getStringValue();
                    if (connectorConfigBuilder == null) {
                        throw new DeploymentException("Cannot deploy resource adapter; No rar deployer defined: " + moduleName);
                    }
                    builder = connectorConfigBuilder;
                    moduleTypeName = "a connector";
                } else if (gerExtModule.isSetJava()) {
                    moduleName = gerExtModule.getJava().getStringValue();
                    if (appClientConfigBuilder == null) {
                        throw new DeploymentException("Cannot deploy app client; No app client deployer defined: " + moduleName);
                    }
                    builder = appClientConfigBuilder;
                    moduleTypeName = "an application client";
                } else {
                    throw new DeploymentException("Could not find a module builder for module: " + gerExtModule);
                }
                //dd is included explicitly
                XmlObject[] anys = gerExtModule.selectChildren(GerExtModuleType.type.qnameSetForWildcardElements());
                if (anys.length != 1) {
                    throw new DeploymentException("Unexpected count of xs:any elements in embedded vendor plan " + anys.length + " qnameset: " + GerExtModuleType.type.qnameSetForWildcardElements());
                }
                Object vendorDD = anys[0];

                JarFile moduleFile;
                if (gerExtModule.isSetInternalPath()) {
                    String modulePath = gerExtModule.getInternalPath().trim();
                    moduleLocations.add(modulePath);
                    try {
                        moduleFile = new NestedJarFile(earFile, modulePath);
                    } catch (IOException e) {
                        throw new DeploymentException("Invalid moduleFile: " + modulePath, e);
                    }
                } else {
                    String path = gerExtModule.getExternalPath().trim();
                    Artifact artifact = Artifact.create(path);
                    if (!repository.contains(artifact)) {
                        throw new DeploymentException(moduleTypeName + " is missing in repository: " + path);
                    }
                    File location = repository.getLocation(artifact);
                    try {
                        moduleFile = new JarFile(location);
                    } catch (IOException e) {
                        throw new DeploymentException("Could not access contents of " + moduleTypeName, e);
                    }

                }


                URL altSpecDD = null;
                //todo implement an alt-spec-dd element.
//                if (moduleXml.isSetAltDd()) {
//                    try {
//                        altSpecDD = DeploymentUtil.createJarURL(earFile, moduleXml.getAltDd().getStringValue());
//                    } catch (MalformedURLException e) {
//                        throw new DeploymentException("Invalid alt spec dd url: " + moduleXml.getAltDd().getStringValue(), e);
//                    }
//                }


                Module module = builder.createModule(vendorDD,
                        moduleFile,
                        moduleName,
                        altSpecDD,
                        environment,
                        moduleContextInfo, earName);

                if (module == null) {
                    throw new DeploymentException("Module was not " + moduleTypeName + ": " + moduleName);
                }

                modules.add(module);
            }


        } finally {
            // delete all the temp files created for alt vendor dds
            for (Iterator iterator = altVendorDDs.values().iterator(); iterator.hasNext();) {
                Object altVendorDD = iterator.next();
                if (altVendorDD instanceof File) {
                    ((File) altVendorDD).delete();
                }
            }
        }
    }

    private ModuleBuilder getBuilder(Module module) throws DeploymentException {
        if (module instanceof EJBModule) {
            if (ejbConfigBuilder == null) {
                throw new DeploymentException("Cannot deploy ejb application; No ejb deployer defined: " + module.getModuleURI());
            }
            return ejbConfigBuilder;
        } else if (module instanceof WebModule) {
            if (webConfigBuilder == null) {
                throw new DeploymentException("Cannot deploy web application; No war deployer defined: " + module.getModuleURI());
            }
            return webConfigBuilder;
        } else if (module instanceof ConnectorModule) {
            if (connectorConfigBuilder == null) {
                throw new DeploymentException("Cannot deploy resource adapter; No rar deployer defined: " + module.getModuleURI());
            }
            return connectorConfigBuilder;
        } else if (module instanceof AppClientModule) {
            if (appClientConfigBuilder == null) {
                throw new DeploymentException("Cannot deploy app client; No app client deployer defined: " + module.getModuleURI());
            }
            return appClientConfigBuilder;
        }
        throw new IllegalArgumentException("Unknown module type: " + module.getClass().getName());
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(EARConfigBuilder.class, NameFactory.CONFIG_BUILDER);
        infoFactory.addAttribute("defaultEnvironment", Environment.class, true, true);
        infoFactory.addAttribute("transactionContextManagerAbstractName", AbstractNameQuery.class, true);
        infoFactory.addAttribute("connectionTrackerAbstractName", AbstractNameQuery.class, true);
        infoFactory.addAttribute("transactionalTimerAbstractName", AbstractNameQuery.class, true);
        infoFactory.addAttribute("nonTransactionalTimerAbstractName", AbstractNameQuery.class, true);
        infoFactory.addAttribute("corbaGBeanAbstractName", AbstractNameQuery.class, true);

        infoFactory.addReference("Repository", Repository.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("EJBConfigBuilder", ModuleBuilder.class, NameFactory.MODULE_BUILDER);
        infoFactory.addReference("EJBReferenceBuilder", EJBReferenceBuilder.class, NameFactory.MODULE_BUILDER);
        infoFactory.addReference("WebConfigBuilder", ModuleBuilder.class, NameFactory.MODULE_BUILDER);
        infoFactory.addReference("ConnectorConfigBuilder", ModuleBuilder.class, NameFactory.MODULE_BUILDER);
        infoFactory.addReference("ResourceReferenceBuilder", ResourceReferenceBuilder.class, NameFactory.MODULE_BUILDER);
        infoFactory.addReference("AppClientConfigBuilder", ModuleBuilder.class, NameFactory.MODULE_BUILDER);
        infoFactory.addReference("ServiceReferenceBuilder", ServiceReferenceBuilder.class, NameFactory.MODULE_BUILDER);

        infoFactory.addAttribute("kernel", Kernel.class, false);

        infoFactory.addInterface(ConfigurationBuilder.class);

        infoFactory.setConstructor(new String[]{
                "defaultEnvironment",
                "transactionContextManagerAbstractName",
                "connectionTrackerAbstractName",
                "transactionalTimerAbstractName",
                "nonTransactionalTimerAbstractName",
                "corbaGBeanAbstractName",
                "Repository",
                "EJBConfigBuilder",
                "EJBReferenceBuilder",
                "WebConfigBuilder",
                "ConnectorConfigBuilder",
                "ResourceReferenceBuilder",
                "AppClientConfigBuilder",
                "ServiceReferenceBuilder",
                "kernel"
        });

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
