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

package org.apache.geronimo.myfaces.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

import javax.faces.webapp.FacesServlet;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.Deployable;
import org.apache.geronimo.deployment.ModuleIDBuilder;
import org.apache.geronimo.deployment.service.EnvironmentBuilder;
import org.apache.geronimo.deployment.xmlbeans.XmlBeansUtil;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.annotation.Holder;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.ModuleBuilderExtension;
import org.apache.geronimo.j2ee.deployment.NamingBuilder;
import org.apache.geronimo.j2ee.deployment.WebModule;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.j2ee.jndi.JndiKey;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.myfaces.LifecycleProviderGBean;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.xbeans.javaee.FacesConfigDocument;
import org.apache.geronimo.xbeans.javaee.FacesConfigManagedBeanType;
import org.apache.geronimo.xbeans.javaee.FacesConfigType;
import org.apache.geronimo.xbeans.javaee.FullyQualifiedClassType;
import org.apache.geronimo.xbeans.javaee6.ParamValueType;
import org.apache.geronimo.xbeans.javaee6.ServletType;
import org.apache.geronimo.xbeans.javaee6.WebAppType;
import org.apache.myfaces.webapp.StartupServletContextListener;
import org.apache.xbean.finder.ClassFinder;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev $Date
 */
public class MyFacesModuleBuilderExtension implements ModuleBuilderExtension {

    private static final Logger log = LoggerFactory.getLogger(MyFacesModuleBuilderExtension.class);

    private final Environment defaultEnvironment;
    private final AbstractNameQuery providerFactoryNameQuery;
    private final NamingBuilder namingBuilders;
    private static final String CONTEXT_LISTENER_NAME = StartupServletContextListener.class.getName();
    private static final String FACES_SERVLET_NAME = FacesServlet.class.getName();
    private static final String SCHEMA_LOCATION_URL = "http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd";
    private static final String VERSION = "2.0";


    public MyFacesModuleBuilderExtension(Environment defaultEnvironment, AbstractNameQuery providerFactoryNameQuery, NamingBuilder namingBuilders) {
        this.defaultEnvironment = defaultEnvironment;
        this.providerFactoryNameQuery = providerFactoryNameQuery;
        this.namingBuilders = namingBuilders;
    }

    public void createModule(Module module, Bundle bundle, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
        mergeEnvironment(module);
    }

    public void createModule(Module module, Object plan, JarFile moduleFile, String targetPath, URL specDDUrl, Environment environment, Object moduleContextInfo, AbstractName earName, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
        mergeEnvironment(module);
    }

    private void mergeEnvironment(Module module) {
        if (!(module instanceof WebModule)) {
            //not a web module, nothing to do
            return;
        }
        WebModule webModule = (WebModule) module;
        WebAppType webApp = (WebAppType) webModule.getSpecDD();
        if (!hasFacesServlet(webApp)) {
            return;
        }

        EnvironmentBuilder.mergeEnvironments(module.getEnvironment(), defaultEnvironment);
    }

    public void installModule(JarFile earFile, EARContext earContext, Module module, Collection configurationStores, ConfigurationStore targetConfigurationStore, Collection repository) throws DeploymentException {
    }

    public void initContext(EARContext earContext, Module module, Bundle bundle) throws DeploymentException {
    }

    public void addGBeans(EARContext earContext, Module module, Bundle bundle, Collection repository) throws DeploymentException {
        if (!(module instanceof WebModule)) {
            //not a web module, nothing to do
            return;
        }
        WebModule webModule = (WebModule) module;
        WebAppType webApp = (WebAppType) webModule.getSpecDD();
        if (!hasFacesServlet(webApp)) {
            return;
        }

        EARContext moduleContext = module.getEarContext();
        Map sharedContext = module.getSharedContext();
        //add the ServletContextListener to the web app context
        GBeanData webAppData = (GBeanData) sharedContext.get(WebModule.WEB_APP_DATA);
        // add myfaces listener
        Object value = webAppData.getAttribute("listenerClassNames");
        if (value instanceof Collection && !((Collection) value).contains(CONTEXT_LISTENER_NAME)) {
            ((Collection<String>) value).add(CONTEXT_LISTENER_NAME);
        }
        AbstractName moduleName = moduleContext.getModuleName();
        Map<EARContext.Key, Object> buildingContext = new HashMap<EARContext.Key, Object>();
        buildingContext.put(NamingBuilder.GBEAN_NAME_KEY, moduleName);

        //use the same jndi context as the web app
        Map<JndiKey, Map<String, Object>> compContext = NamingBuilder.JNDI_KEY.get(module.getSharedContext());
        buildingContext.put(NamingBuilder.JNDI_KEY, compContext);

        //use the same holder object as the web app.
        Holder holder = NamingBuilder.INJECTION_KEY.get(sharedContext);
        buildingContext.put(NamingBuilder.INJECTION_KEY, holder);

        XmlObject jettyWebApp = webModule.getVendorDD();

//        Configuration earConfiguration = earContext.getConfiguration();

        ClassFinder classFinder = createMyFacesClassFinder(webApp, webModule);
        webModule.setClassFinder(classFinder);

        namingBuilders.buildNaming(webApp, jettyWebApp, webModule, buildingContext);

        AbstractName providerName = moduleContext.getNaming().createChildName(moduleName, "jsf-lifecycle", "jsf");
        GBeanData providerData = new GBeanData(providerName, LifecycleProviderGBean.class);
        providerData.setAttribute("holder", holder);
        providerData.setReferencePatterns("ContextSource", webAppData.getReferencePatterns("ContextSource"));
        providerData.setReferencePattern("LifecycleProviderFactory", providerFactoryNameQuery);
        try {
            moduleContext.addGBean(providerData);
        } catch (GBeanAlreadyExistsException e) {
            throw new DeploymentException("Duplicate jsf config gbean in web module", e);
        }

        //make the web app start second after the injection machinery
        webAppData.addDependency(providerName);

    }

    private boolean hasFacesServlet(WebAppType webApp) {
        for (ServletType servlet : webApp.getServletArray()) {
            if (servlet.isSetServletClass() && FACES_SERVLET_NAME.equals(servlet.getServletClass().getStringValue().trim())) {
                return true;
            }
        }
        return false;
    }


    protected ClassFinder createMyFacesClassFinder(WebAppType webApp, WebModule webModule) throws DeploymentException {

        List<Class> classes = getFacesClasses(webApp, webModule);
        return new ClassFinder(classes);
    }


    /**
     * getFacesConfigFileURL()
     * <p/>
     * <p>Locations to search for the MyFaces configuration file(s):
     * <ol>
     * <li>META-INF/faces-config.xml
     * <li>WEB-INF/faces-config.xml
     * <li>javax.faces.CONFIG_FILES -- Context initialization param of Comma separated
     * list of URIs of (additional) faces config files
     * </ol>
     * <p/>
     * <p><strong>Notes:</strong>
     * <ul>
     * </ul>
     *
     * @param webApp    spec DD for module
     * @param webModule module being deployed
     * @return list of all managed bean classes from all faces-config xml files.
     * @throws org.apache.geronimo.common.DeploymentException
     *          if a faces-config.xml file is located but cannot be parsed.
     */
    private List<Class> getFacesClasses(WebAppType webApp, WebModule webModule) throws DeploymentException {
        log.debug("getFacesClasses( " + webApp.toString() + "," + '\n' +
                           (webModule != null ? webModule.getName() : null) + " ): Entry");

        Deployable deployable = webModule.getDeployable();
        Bundle bundle = webModule.getEarContext().getDeploymentBundle();

        // 1. META-INF/faces-config.xml
        List<Class> classes = new ArrayList<Class>();
        URL url = deployable.getResource("META-INF/faces-config.xml");
        if (url != null) {
            parseConfigFile(url, bundle, classes);
        }

        // 2. WEB-INF/faces-config.xml
        url = deployable.getResource("WEB-INF/faces-config.xml");
        if (url != null) {
            parseConfigFile(url, bundle, classes);
        }

        // 3. javax.faces.CONFIG_FILES
        ParamValueType[] paramValues = webApp.getContextParamArray();
        for (ParamValueType paramValue : paramValues) {
            if (paramValue.getParamName().getStringValue().trim().equals("javax.faces.CONFIG_FILES")) {
                String configFiles = paramValue.getParamValue().getStringValue().trim();
                StringTokenizer st = new StringTokenizer(configFiles, ",", false);
                while (st.hasMoreTokens()) {
                    String configfile = st.nextToken().trim();
                    if (!configfile.equals("")) {
                        if (configfile.startsWith("/")) {
                            configfile = configfile.substring(1);
                        }
                        url = deployable.getResource(configfile);
                        if (url == null) {
                            throw new DeploymentException("Could not locate config file " + configfile);
                        } else {
                            parseConfigFile(url, bundle, classes);
                        }
                    }
                }
                break;
            }
        }

        log.debug("getFacesClasses() Exit: " + classes.size() + " " + classes.toString());
        return classes;
    }

    private void parseConfigFile(URL url,  Bundle bundle, List<Class> classes) throws DeploymentException {
        log.debug("parseConfigFile( " + url.toString() + " ): Entry");

        try {
            XmlObject xml = XmlBeansUtil.parse(url, null);
            FacesConfigDocument fcd = convertToFacesConfigSchema(xml);
            FacesConfigType facesConfig = fcd.getFacesConfig();

            // Get all the managed beans from the faces configuration file
            FacesConfigManagedBeanType[] managedBeans = facesConfig.getManagedBeanArray();
            for (FacesConfigManagedBeanType managedBean : managedBeans) {
                FullyQualifiedClassType cls = managedBean.getManagedBeanClass();
                String className = cls.getStringValue().trim();
                Class<?> clas;
                try {
                    clas = bundle.loadClass(className);
                    classes.add(clas);
                }
                catch (ClassNotFoundException e) {
                    log.warn("MyFacesModuleBuilderExtension: Could not load managed bean class: " + className  + " mentioned in faces-config.xml file at " + url.toString());
                }
            }
        }
        catch (XmlException xmle) {
            throw new DeploymentException("Could not parse alleged faces-config.xml at " + url.toString(), xmle);
        }
        catch (IOException ioe) {
            throw new DeploymentException("Error reading jsf configuration file " + url, ioe);
        }

        log.debug("parseConfigFile(): Exit");
    }

    protected static FacesConfigDocument convertToFacesConfigSchema(XmlObject xmlObject) throws XmlException {
        log.debug("convertToFacesConfigSchema( " + xmlObject.toString() + " ): Entry");
        XmlCursor cursor = xmlObject.newCursor();
        try {
            cursor.toStartDoc();
            cursor.toFirstChild();
            String nameSpaceURI = cursor.getName().getNamespaceURI();
            if (SchemaConversionUtils.JAVAEE_NAMESPACE.equals(nameSpaceURI) || SchemaConversionUtils.J2EE_NAMESPACE.equals(nameSpaceURI)) {
                SchemaConversionUtils.convertSchemaVersion(cursor, SchemaConversionUtils.JAVAEE_NAMESPACE, SCHEMA_LOCATION_URL, VERSION);
            } else {
                // otherwise assume DTD
                SchemaConversionUtils.convertToSchema(cursor, SchemaConversionUtils.JAVAEE_NAMESPACE, SCHEMA_LOCATION_URL, VERSION);
            }
        } finally {
            cursor.dispose();
        }
        XmlObject result = xmlObject.changeType(FacesConfigDocument.type);
        if (result != null) {
            XmlBeansUtil.validateDD(result);
            log.debug("convertToFacesConfigSchema(): Exit 2");
            return (FacesConfigDocument) result;
        }
        XmlBeansUtil.validateDD(xmlObject);
        log.debug("convertToFacesConfigSchema(): Exit 3");
        return (FacesConfigDocument) xmlObject;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(MyFacesModuleBuilderExtension.class, NameFactory.MODULE_BUILDER);
        infoBuilder.addAttribute("defaultEnvironment", Environment.class, true, true);
        infoBuilder.addAttribute("providerFactoryNameQuery", AbstractNameQuery.class, true, true);
        infoBuilder.addReference("NamingBuilders", NamingBuilder.class, NameFactory.MODULE_BUILDER);

        infoBuilder.setConstructor(new String[]{
                "defaultEnvironment",
                "providerFactoryNameQuery",
                "NamingBuilders"});
        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }


}
