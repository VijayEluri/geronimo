/**
 *
 * Copyright 2005 The Apache Software Foundation
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
import org.apache.geronimo.deployment.xmlbeans.XmlBeansUtil;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.ReferenceCollection;
import org.apache.geronimo.gbean.ReferenceCollectionEvent;
import org.apache.geronimo.gbean.ReferenceCollectionListener;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.Repository;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @version $Rev$ $Date$
 */
public class SwitchingModuleBuilder implements ModuleBuilder {

    private final ReferenceCollection builders;
    private final Map namespaceToBuilderMap = new HashMap();

    private String defaultNamespace;

    public SwitchingModuleBuilder(Collection builders) {
        this.builders = (ReferenceCollection) builders;
        this.builders.addReferenceCollectionListener(new ReferenceCollectionListener() {
            public void memberAdded(ReferenceCollectionEvent event) {
                ModuleBuilder builder = (ModuleBuilder) event.getMember();
                String namespace = builder.getSchemaNamespace();
                namespaceToBuilderMap.put(namespace, builder);
            }

            public void memberRemoved(ReferenceCollectionEvent event) {
                ModuleBuilder builder = (ModuleBuilder) event.getMember();
                String namespace =  builder.getSchemaNamespace();
                namespaceToBuilderMap.remove(namespace);
            }
        });
        for (Iterator iterator = builders.iterator(); iterator.hasNext();) {
            ModuleBuilder builder = (ModuleBuilder) iterator.next();
            String namespace =  builder.getSchemaNamespace();
            namespaceToBuilderMap.put(namespace, builder);
        }

    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public Module createModule(File plan, JarFile moduleFile) throws DeploymentException {
        String namespace;
        if (plan == null) {
            namespace = defaultNamespace;
        } else {
            namespace = getNamespaceFromPlan(plan);
        }
        ModuleBuilder builder = getBuilderFromNamespace(namespace);
        if (builder != null) {
            return builder.createModule(plan, moduleFile);
        } else {
            return null;
        }
    }

    private String getNamespaceFromPlan(Object plan) throws DeploymentException {
        XmlObject xmlObject;
        if (plan instanceof File) {
            try {
                xmlObject = XmlBeansUtil.parse(((File) plan).toURL());
            } catch (IOException e) {
                throw new DeploymentException("Could not read plan file", e);
            } catch (XmlException e) {
                throw new DeploymentException("Plan file does not contain well formed xml", e);
            }
        } else if (plan instanceof XmlObject) {
            xmlObject = (XmlObject) plan;
        } else {
            return defaultNamespace;
        }
        XmlCursor cursor = xmlObject.newCursor();
        try {
            while (cursor.hasNextToken()){
                if (cursor.isStart()) {
                    String namespace = cursor.getName().getNamespaceURI();
                    return namespace;
                }
                cursor.toNextToken();
            }
        } finally {
            cursor.dispose();
        }
        throw new DeploymentException("Cannot find namespace in xmlObject: " + xmlObject.xmlText());
    }

    private ModuleBuilder getBuilderFromNamespace(String namespace) {
        ModuleBuilder builder = (ModuleBuilder) namespaceToBuilderMap.get(namespace);
        if (builder == null) {
            builder = (ModuleBuilder) namespaceToBuilderMap.get(defaultNamespace);
        }
        return builder;
    }

    public Module createModule(Object plan, JarFile moduleFile, String targetPath, URL specDDUrl, Environment environment, Object moduleContextInfo, AbstractName earName) throws DeploymentException {
        String namespace = getNamespaceFromPlan(plan);
        ModuleBuilder builder = getBuilderFromNamespace(namespace);
        if (builder != null) {
            return builder.createModule(plan, moduleFile, targetPath, specDDUrl, environment, moduleContextInfo, earName);
        } else {
            return null;
        }
    }

    public void installModule(JarFile earFile, EARContext earContext, Module module, ConfigurationStore configurationStore, Repository repository) throws DeploymentException {
        String namespace = module.getNamespace();
        ModuleBuilder builder = getBuilderFromNamespace(namespace);
        builder.installModule(earFile, earContext, module, configurationStore, repository);
    }

    public void initContext(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
        String namespace = module.getNamespace();
        ModuleBuilder builder = getBuilderFromNamespace(namespace);
        builder.initContext(earContext, module, cl);
    }

    public void addGBeans(EARContext earContext, Module module, ClassLoader cl, Repository repository) throws DeploymentException {
        String namespace = module.getNamespace();
        ModuleBuilder builder = getBuilderFromNamespace(namespace);
        builder.addGBeans(earContext, module, cl, repository);
    }

    public String getSchemaNamespace() {
        return null;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(SwitchingModuleBuilder.class, NameFactory.MODULE_BUILDER);
        infoBuilder.addAttribute("defaultNamespace", String.class, true, true);
        infoBuilder.addReference("ModuleBuilders", ModuleBuilder.class, NameFactory.MODULE_BUILDER);
        infoBuilder.addInterface(ModuleBuilder.class);

        infoBuilder.setConstructor(new String[] {"ModuleBuilders"});
        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
