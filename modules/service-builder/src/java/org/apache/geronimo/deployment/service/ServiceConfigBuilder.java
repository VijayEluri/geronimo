/**
 *
 * Copyright 2003-2005 The Apache Software Foundation
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

package org.apache.geronimo.deployment.service;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.ConfigurationBuilder;
import org.apache.geronimo.deployment.DeploymentContext;
import org.apache.geronimo.deployment.xbeans.AttributeType;
import org.apache.geronimo.deployment.xbeans.ConfigurationDocument;
import org.apache.geronimo.deployment.xbeans.ConfigurationType;
import org.apache.geronimo.deployment.xbeans.EnvironmentType;
import org.apache.geronimo.deployment.xbeans.GbeanType;
import org.apache.geronimo.deployment.xbeans.PatternType;
import org.apache.geronimo.deployment.xbeans.ReferenceType;
import org.apache.geronimo.deployment.xbeans.ReferencesType;
import org.apache.geronimo.deployment.xbeans.XmlAttributeType;
import org.apache.geronimo.deployment.xmlbeans.XmlBeansUtil;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.ReferenceMap;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.config.ConfigurationAlreadyExistsException;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.Repository;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import javax.management.MalformedObjectNameException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @version $Rev: 384933 $ $Date$
 */
public class ServiceConfigBuilder implements ConfigurationBuilder {
    private final Environment defaultEnvironment;
    private final Repository repository;
    private final Kernel kernel;

    //TODO this being static is a really good argument that all other builders should have a reference to this gbean, not use static methods on it.
    private static final Map xmlAttributeBuilderMap = new HashMap();
    private static final Map xmlReferenceBuilderMap = new HashMap();
    private Map attrRefMap;
    private Map refRefMap;
    private static final QName SERVICE_QNAME = ConfigurationDocument.type.getDocumentElementName();


    public ServiceConfigBuilder(Environment defaultEnvironment, Repository repository) {
        this(defaultEnvironment, repository, null, null, null);
    }

    public ServiceConfigBuilder(Environment defaultEnvironment, Repository repository, Collection xmlAttributeBuilders, Collection xmlReferenceBuilders, Kernel kernel) {
        EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();
        xmlAttributeBuilderMap.put(environmentBuilder.getNamespace(), environmentBuilder);
        this.defaultEnvironment = defaultEnvironment;

        this.repository = repository;
        this.kernel = kernel;
        if (xmlAttributeBuilders != null) {
            ReferenceMap.Key key = new ReferenceMap.Key() {

                public Object getKey(Object object) {
                    return ((XmlAttributeBuilder) object).getNamespace();
                }
            };
            attrRefMap = new ReferenceMap(xmlAttributeBuilders, xmlAttributeBuilderMap, key);
        }
        if (xmlReferenceBuilders != null) {
            ReferenceMap.Key key = new ReferenceMap.Key() {

                public Object getKey(Object object) {
                    return ((XmlReferenceBuilder) object).getNamespace();
                }
            };
            refRefMap = new ReferenceMap(xmlReferenceBuilders, xmlReferenceBuilderMap, key);
        }
    }

    public Object getDeploymentPlan(File planFile, JarFile module) throws DeploymentException {
        if (planFile == null) {
            return null;
        }

        try {
            XmlObject xmlObject = XmlBeansUtil.parse(planFile);
            XmlCursor cursor = xmlObject.newCursor();
            try {
                cursor.toFirstChild();
                if (!SERVICE_QNAME.equals(cursor.getName())) {
                    return null;
                }
            } finally {
                cursor.dispose();
            }
            ConfigurationDocument configurationDoc;
            if (xmlObject instanceof ConfigurationDocument) {
                configurationDoc = (ConfigurationDocument) xmlObject;
            } else {
                configurationDoc = (ConfigurationDocument) xmlObject.changeType(ConfigurationDocument.type);
            }
            Collection errors = new ArrayList();
            if (!configurationDoc.validate(XmlBeansUtil.createXmlOptions(errors))) {
                throw new DeploymentException("Invalid deployment descriptor: " + errors + "\nDescriptor: " + configurationDoc.toString());
            }
            return configurationDoc.getConfiguration();
        } catch (XmlException e) {
            throw new DeploymentException("Could not parse xml in plan", e);
        } catch (IOException e) {
            throw new DeploymentException("no plan at " + planFile, e);
        }
    }

    public Artifact getConfigurationID(Object plan, JarFile module) throws IOException, DeploymentException {
        ConfigurationType configType = (ConfigurationType) plan;
        EnvironmentType environmentType = configType.getEnvironment();
        //TODO default id based on name?
        Environment environment = EnvironmentBuilder.buildEnvironment(environmentType, defaultEnvironment);
        return environment.getConfigId();
    }

    public ConfigurationData buildConfiguration(Object plan, JarFile unused, ConfigurationStore configurationStore) throws IOException, DeploymentException {
        ConfigurationType configType = (ConfigurationType) plan;

        return buildConfiguration(configType, configurationStore);
    }

    public ConfigurationData buildConfiguration(ConfigurationType configurationType, ConfigurationStore configurationStore) throws DeploymentException, IOException {

        Environment environment = EnvironmentBuilder.buildEnvironment(configurationType.getEnvironment(), defaultEnvironment);
        Artifact configId = environment.getConfigId();
        File outfile;
        try {
            outfile = configurationStore.createNewConfigurationDir(configId);
        } catch (ConfigurationAlreadyExistsException e) {
            throw new DeploymentException(e);
        }
        DeploymentContext context = new DeploymentContext(outfile, environment, ConfigurationModuleType.SERVICE, kernel);
        ClassLoader cl = context.getClassLoader();


        AbstractName moduleName;
        try {
            moduleName = NameFactory.buildModuleName(environment.getProperties(), configId, ConfigurationModuleType.SERVICE, null);
        } catch (MalformedObjectNameException e) {
            throw new DeploymentException(e);
        }
        GbeanType[] gbeans = configurationType.getGbeanArray();
        addGBeans(gbeans, cl, moduleName, context);
        context.close();
        return context.getConfigurationData();
    }

    public static void addGBeans(GbeanType[] gbeans, ClassLoader cl, AbstractName moduleName, DeploymentContext context) throws DeploymentException {
        for (int i = 0; i < gbeans.length; i++) {
            addGBeanData(gbeans[i], moduleName, cl, context);
        }
    }

    public static AbstractName addGBeanData(GbeanType gbean, AbstractName moduleName, ClassLoader cl, DeploymentContext context) throws DeploymentException {
        GBeanInfo gBeanInfo = GBeanInfo.getGBeanInfo(gbean.getClass1(), cl);
        AbstractName abstractName;
//        if (gbean.isSetGbeanName()) {
//            try {
//                abstractName = ObjectName.getInstance(gbean.getGbeanName());
//                nameMap.putAll(abstractName.getKeyPropertyList());
//            } catch (MalformedObjectNameException e) {
//                throw new DeploymentException("Invalid ObjectName: " + gbean.getName(), e);
//            }
//        } else {
            String namePart = gbean.getName();
            String j2eeType = gBeanInfo.getJ2eeType();
            //todo investigate using the module type from the j2eecontext.
            abstractName = NameFactory.getChildName(moduleName, j2eeType, namePart, gBeanInfo.getInterfaces());
//                nameMap.put("name", namePart);
//                nameMap.put("type", j2eeType);
//        }
//        AbstractName abstractName = new AbstractName(context.getConfigID(), nameMap, gBeanInfo.getInterfaces(), abstractName);
        GBeanBuilder builder = new GBeanBuilder(abstractName, gBeanInfo, cl, context, moduleName, xmlAttributeBuilderMap, xmlReferenceBuilderMap);

        // set up attributes
        AttributeType[] attributeArray = gbean.getAttributeArray();
        if (attributeArray != null) {
            for (int j = 0; j < attributeArray.length; j++) {
                builder.setAttribute(attributeArray[j].getName().trim(), attributeArray[j].getType(), attributeArray[j].getStringValue());
            }
        }

        XmlAttributeType[] xmlAttributeArray = gbean.getXmlAttributeArray();
        if (xmlAttributeArray != null) {
            for (int i = 0; i < xmlAttributeArray.length; i++) {
                XmlAttributeType xmlAttributeType = xmlAttributeArray[i];
                String name = xmlAttributeType.getName().trim();
                XmlObject[] anys = xmlAttributeType.selectChildren(XmlAttributeType.type.qnameSetForWildcardElements());
                if (anys.length != 1) {
                    throw new DeploymentException("Unexpected count of xs:any elements in xml-attribute " + anys.length + " qnameset: " + XmlAttributeType.type.qnameSetForWildcardElements());
                }
                builder.setXmlAttribute(name, anys[0]);
            }
        }

        // set up all single pattern references
        ReferenceType[] referenceArray = gbean.getReferenceArray();
        if (referenceArray != null) {
            for (int j = 0; j < referenceArray.length; j++) {
                builder.setReference(referenceArray[j].getName2(), referenceArray[j], moduleName);
            }
        }

        // set up app multi-patterned references
        ReferencesType[] referencesArray = gbean.getReferencesArray();
        if (referencesArray != null) {
            for (int j = 0; j < referencesArray.length; j++) {
                builder.setReference(referencesArray[j].getName(), referencesArray[j].getPatternArray(), moduleName);
            }
        }

        XmlAttributeType[] xmlReferenceArray = gbean.getXmlReferenceArray();
        if (xmlReferenceArray != null) {
            for (int i = 0; i < xmlReferenceArray.length; i++) {
                XmlAttributeType xmlAttributeType = xmlReferenceArray[i];
                String name = xmlAttributeType.getName().trim();
                XmlObject[] anys = xmlAttributeType.selectChildren(XmlAttributeType.type.qnameSetForWildcardElements());
                if (anys.length != 1) {
                    throw new DeploymentException("Unexpected count of xs:any elements in xml-attribute " + anys.length + " qnameset: " + XmlAttributeType.type.qnameSetForWildcardElements());
                }
                builder.setXmlReference(name, anys[0]);
            }
        }

        PatternType[] dependencyArray = gbean.getDependencyArray();
        if (dependencyArray != null) {
            for (int i = 0; i < dependencyArray.length; i++) {
                PatternType patternType = dependencyArray[i];
                builder.addDependency(patternType);
            }
        }

        GBeanData gbeanData = builder.getGBeanData();
        try {
            context.addGBean(gbeanData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException(e);
        }
        return abstractName;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(ServiceConfigBuilder.class, NameFactory.CONFIG_BUILDER);

        infoFactory.addInterface(ConfigurationBuilder.class);

        infoFactory.addAttribute("defaultEnvironment", Environment.class, true);
        infoFactory.addReference("Repository", Repository.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("XmlAttributeBuilders", XmlAttributeBuilder.class, "XmlAttributeBuilder");
        infoFactory.addReference("XmlReferenceBuilders", XmlReferenceBuilder.class, "XmlReferenceBuilder");
        infoFactory.addAttribute("kernel", Kernel.class, false);

        infoFactory.setConstructor(new String[]{"defaultEnvironment", "Repository", "XmlAttributeBuilders", "XmlReferenceBuilders", "kernel"});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
