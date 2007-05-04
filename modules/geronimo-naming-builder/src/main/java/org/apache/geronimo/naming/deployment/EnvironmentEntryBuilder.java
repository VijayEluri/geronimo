/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.naming.deployment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.annotation.AnnotatedApp;
import org.apache.geronimo.j2ee.deployment.annotation.ResourceAnnotationHelper;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.naming.reference.KernelReference;
import org.apache.geronimo.xbeans.javaee.DescriptionType;
import org.apache.geronimo.xbeans.javaee.EnvEntryType;
import org.apache.geronimo.xbeans.javaee.EnvEntryTypeValuesType;
import org.apache.geronimo.xbeans.javaee.InjectionTargetType;
import org.apache.geronimo.xbeans.javaee.JndiNameType;
import org.apache.geronimo.xbeans.javaee.XsdStringType;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlObject;

/**
 * @version $Rev$ $Date$
 */
public class EnvironmentEntryBuilder extends AbstractNamingBuilder {

    private static final Log log = LogFactory.getLog(EnvironmentEntryBuilder.class);

    private final QNameSet envEntryQNameSet;

    public EnvironmentEntryBuilder(String[] eeNamespaces) {
        envEntryQNameSet = buildQNameSet(eeNamespaces, "env-entry");
    }

    public void buildNaming(XmlObject specDD, XmlObject plan, Module module, Map componentContext) throws DeploymentException {

        // Discover and process any @Resource annotations (if !metadata-complete)
        if ((module != null) && (module.getClassFinder() != null)) {

            // Process all the annotations for this naming builder type
            try {
                ResourceAnnotationHelper.processAnnotations(module.getAnnotatedApp(), module.getClassFinder(), EnvEntryRefProcessor.INSTANCE);
            }
            catch (Exception e) {
                log.warn("Unable to process @Resource annotations for module" + module.getName(), e);
            }
        }

        List<EnvEntryType> envEntriesUntyped = convert(specDD.selectChildren(envEntryQNameSet), JEE_CONVERTER, EnvEntryType.class, EnvEntryType.type);
        for (EnvEntryType envEntry: envEntriesUntyped) {
            String name = getStringValue(envEntry.getEnvEntryName());
            addInjections(name, envEntry.getInjectionTargetArray(), componentContext);
            String type = getStringValue(envEntry.getEnvEntryType());
            String text = getStringValue(envEntry.getEnvEntryValue());
            try {
                Object value;
                if (text == null) {
                    if ("org.apache.geronimo.kernel.Kernel".equals(type)) {
                        value = new KernelReference();
                    } else {
                        value = null;
                    }
                } else if ("java.lang.String".equals(type)) {
                    value = text;
                } else if ("java.lang.Character".equals(type)) {
                    value = text.charAt(0);
                } else if ("java.lang.Boolean".equals(type)) {
                    value = Boolean.valueOf(text);
                } else if ("java.lang.Byte".equals(type)) {
                    value = Byte.valueOf(text);
                } else if ("java.lang.Short".equals(type)) {
                    value = Short.valueOf(text);
                } else if ("java.lang.Integer".equals(type)) {
                    value = Integer.valueOf(text);
                } else if ("java.lang.Long".equals(type)) {
                    value = Long.valueOf(text);
                } else if ("java.lang.Float".equals(type)) {
                    value = Float.valueOf(text);
                } else if ("java.lang.Double".equals(type)) {
                    value = Double.valueOf(text);
                } else {
                    throw new DeploymentException("unrecognized type: " + type);
                }
                getJndiContextMap(componentContext).put(ENV + name, value);
            } catch (NumberFormatException e) {
                throw new DeploymentException("Invalid env-entry value for name: " + name, e);
            }
        }

    }

    public QNameSet getSpecQNameSet() {
        return envEntryQNameSet;
    }

    public QNameSet getPlanQNameSet() {
        return QNameSet.EMPTY;
    }

    public static class EnvEntryRefProcessor extends ResourceAnnotationHelper.ResourceProcessor {

        public static final EnvEntryRefProcessor INSTANCE = new EnvEntryRefProcessor();

        private EnvEntryRefProcessor() {
        }

        public boolean processResource(AnnotatedApp annotatedApp, Resource annotation, Class cls, Method method, Field field) {
            String resourceName = getResourceName(annotation, method, field);
            String resourceType = getResourceType(annotation, method, field);
            if (resourceType.equals("java.lang.String") ||
                    resourceType.equals("java.lang.Character") ||
                    resourceType.equals("java.lang.Integer") ||
                    resourceType.equals("java.lang.Boolean") ||
                    resourceType.equals("java.lang.Double") ||
                    resourceType.equals("java.lang.Byte") ||
                    resourceType.equals("java.lang.Short") ||
                    resourceType.equals("java.lang.Long") ||
                    resourceType.equals("java.lang.Float")) {

                log.debug("addResource(): <env-entry> found");

                boolean exists = false;
                EnvEntryType[] envEntries = annotatedApp.getEnvEntryArray();
                for (EnvEntryType envEntry : envEntries) {
                    if (getStringValue(envEntry.getEnvEntryName()).equals(resourceName)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    try {

                        log.debug("addResource(): Does not exist in DD: " + resourceName);

                        // Doesn't exist in deployment descriptor -- add new
                        EnvEntryType envEntry = annotatedApp.addNewEnvEntry();

                        //------------------------------------------------------------------------------
                        // <env-entry> required elements:
                        //------------------------------------------------------------------------------

                        // env-entry-name
                        JndiNameType envEntryName = envEntry.addNewEnvEntryName();
                        envEntryName.setStringValue(resourceName);

                        if (!resourceType.equals("")) {
                            // env-entry-type
                            EnvEntryTypeValuesType envEntryType = envEntry.addNewEnvEntryType();
                            envEntryType.setStringValue(resourceType);
                        } else if (method != null || field != null) {
                            // injectionTarget
                            InjectionTargetType injectionTarget = envEntry.addNewInjectionTarget();
                            configureInjectionTarget(injectionTarget, method, field);
                        }

                        // env-entry-value
                        String mappdedNameAnnotation = annotation.mappedName();
                        if (!mappdedNameAnnotation.equals("")) {
                            XsdStringType value = envEntry.addNewEnvEntryValue();
                            value.setStringValue(mappdedNameAnnotation);
                            envEntry.setMappedName(value);
                        }

                        //------------------------------------------------------------------------------
                        // <env-entry> optional elements:
                        //------------------------------------------------------------------------------

                        // description
                        String descriptionAnnotation = annotation.description();
                        if (!descriptionAnnotation.equals("")) {
                            DescriptionType description = envEntry.addNewDescription();
                            description.setStringValue(descriptionAnnotation);
                        }

                    }
                    catch (Exception anyException) {
                        log.debug("ResourceAnnotationHelper: Exception caught while processing <env-entry>");
                    }
                }
            }
            return false;
        }
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(EnvironmentEntryBuilder.class, NameFactory.MODULE_BUILDER);
        infoBuilder.addAttribute("eeNamespaces", String[].class, true, true);
        infoBuilder.setConstructor(new String[] {"eeNamespaces"});

        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
