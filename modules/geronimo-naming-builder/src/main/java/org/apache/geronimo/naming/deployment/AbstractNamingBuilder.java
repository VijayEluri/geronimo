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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.service.EnvironmentBuilder;
import org.apache.geronimo.deployment.xmlbeans.XmlBeansUtil;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.NamingBuilder;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.Dependency;
import org.apache.geronimo.kernel.repository.ImportType;
import org.apache.geronimo.schema.NamespaceElementConverter;
import org.apache.geronimo.xbeans.geronimo.naming.GerPatternType;
import org.apache.geronimo.xbeans.geronimo.naming.GerAbstractNamingEntryDocument;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractNamingBuilder implements NamingBuilder {
    protected static final String J2EE_NAMESPACE = "http://java.sun.com/xml/ns/j2ee";
    protected static final String JEE_NAMESPACE = "http://java.sun.com/xml/ns/javaee";
    protected static final NamespaceElementConverter J2EE_CONVERTER = new NamespaceElementConverter(J2EE_NAMESPACE);
    protected static final NamespaceElementConverter NAMING_CONVERTER = new NamespaceElementConverter(GerAbstractNamingEntryDocument.type.getDocumentElementName().getNamespaceURI());

    private final Environment defaultEnvironment;

    protected AbstractNamingBuilder() {
        defaultEnvironment = null;
    }

    protected AbstractNamingBuilder(Environment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public void buildEnvironment(XmlObject specDD, XmlObject plan, Environment environment) throws DeploymentException {
        if (willMergeEnvironment(specDD, plan)) {
            EnvironmentBuilder.mergeEnvironments(environment, defaultEnvironment);
        }
    }

    protected boolean willMergeEnvironment(XmlObject specDD, XmlObject plan) throws DeploymentException {
        return false;
    }

    protected boolean matchesDefaultEnvironment(Environment environment) {
        for (Iterator iterator = defaultEnvironment.getDependencies().iterator(); iterator.hasNext();) {
            Dependency defaultDependency = (Dependency) iterator.next();
            boolean matches = false;
            for (Iterator iterator1 = environment.getDependencies().iterator(); iterator1.hasNext();) {
                Dependency actualDependency = (Dependency) iterator1.next();
                if (matches(defaultDependency, actualDependency)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(Dependency defaultDependency, Dependency actualDependency) {
        if (defaultDependency.getArtifact().matches(actualDependency.getArtifact())
                || actualDependency.getArtifact().matches(defaultDependency.getArtifact())) {
            return defaultDependency.getImportType() == actualDependency.getImportType()
                    || actualDependency.getImportType() == ImportType.ALL;
        }
        return false;
    }

    public void initContext(XmlObject specDD, XmlObject plan, Configuration localConfiguration, Configuration remoteConfiguration, Module module) throws DeploymentException {
    }

    protected Map getJndiContextMap(Map sharedContext) {
        return (Map) sharedContext.get(JNDI_KEY);
    }

    protected AbstractName getGBeanName(Map sharedContext) {
        return (AbstractName) sharedContext.get(GBEAN_NAME_KEY);
    }

    protected static QNameSet buildQNameSet(String[] eeNamespaces, String localPart) {
        Set qnames = new HashSet(eeNamespaces.length);
        for (int i = 0; i < eeNamespaces.length; i++) {
            String namespace = eeNamespaces[i];
            qnames.add(new QName(namespace, localPart));
        }
        //xmlbeans 2.0 has a bug so forArray doesn't work.  Don't know if it's fixed in later xmlbeans versions
        //return QNameSet.forArray(qnames);
        return QNameSet.forSets(null, Collections.EMPTY_SET, Collections.EMPTY_SET, qnames);
    }

    /**
     * @deprecated
     * @param xmlObjects
     * @param converter
     * @param type
     * @return
     * @throws DeploymentException
     */
    protected static XmlObject[] convert(XmlObject[] xmlObjects, NamespaceElementConverter converter, SchemaType type) throws DeploymentException {
               //bizarre ArrayStoreException if xmlObjects is loaded by the wrong classloader
        XmlObject[] converted = new XmlObject[xmlObjects.length];
        for (int i = 0; i < xmlObjects.length; i++) {
            XmlObject xmlObject = xmlObjects[i].copy();
            if (xmlObject.schemaType() != type) {
                converter.convertElement(xmlObject);
                converted[i] = xmlObject.changeType(type);
            } else {
                converted[i] = xmlObject;
            }
            try {
                XmlBeansUtil.validateDD(converted[i]);
            } catch (XmlException e) {
                throw new DeploymentException("Could not validate xmlObject of type " + type, e);
            }
        }
        return converted;

    }

    protected static <T extends XmlObject> List<T> convert(XmlObject[] xmlObjects, NamespaceElementConverter converter, Class<T> c, SchemaType type) throws DeploymentException {
        //there's probably a better way to say T extends XmlObject and get the type from that
        List<T> result = new ArrayList<T>(xmlObjects.length);
        for (XmlObject xmlObject : xmlObjects) {
            xmlObject = xmlObject.copy();
            if (xmlObject.schemaType() != type) {
                converter.convertElement(xmlObject);
                xmlObject = xmlObject.changeType(type);
            }
            try {
                XmlBeansUtil.validateDD(xmlObject);
            } catch (XmlException e) {
                throw new DeploymentException("Could not validate xmlObject of type " + type, e);
            }
            result.add((T) xmlObject);
        }
        return result;
    }

    protected static String getStringValue(org.apache.geronimo.xbeans.j2ee.String string) {
        if (string == null) {
            return null;
        }
        String s = string.getStringValue();
        return s == null ? null : s.trim();
    }

    public static AbstractNameQuery buildAbstractNameQuery(GerPatternType pattern, String type, String moduleType, Set interfaceTypes) {
        String groupId = pattern.isSetGroupId() ? pattern.getGroupId().trim() : null;
        String artifactid = pattern.isSetArtifactId() ? pattern.getArtifactId().trim() : null;
        String version = pattern.isSetVersion() ? pattern.getVersion().trim() : null;
        String module = pattern.isSetModule() ? pattern.getModule().trim() : null;
        String name = pattern.getName().trim();

        Artifact artifact = artifactid != null ? new Artifact(groupId, artifactid, version, null) : null;
        Map nameMap = new HashMap();
        nameMap.put("name", name);
        if (type != null) {
            nameMap.put("j2eeType", type);
        }
        if (module != null && moduleType != null) {
            nameMap.put(moduleType, module);
        }
        if (interfaceTypes != null) {
            Set trimmed = new HashSet();
            for (Iterator it = interfaceTypes.iterator(); it.hasNext();) {
                String intf = (String) it.next();
                trimmed.add(intf == null ? null : intf.trim());
            }
            interfaceTypes = trimmed;
        }
        return new AbstractNameQuery(artifact, nameMap, interfaceTypes);
    }

    public static AbstractNameQuery buildAbstractNameQuery(Artifact configId, String module, String name, String type, String moduleType) {
        Map nameMap = new HashMap();
        nameMap.put("name", name);
        if (type != null) {
            nameMap.put("j2eeType", type);
        }
        if (module != null) {
            nameMap.put(moduleType, module);
        }
        return new AbstractNameQuery(configId, nameMap);
    }

    public static Class assureInterface(String interfaceName, String superInterfaceName, String interfaceType, ClassLoader cl) throws DeploymentException {
        if (interfaceName == null || interfaceName.equals("")) {
            throw new DeploymentException("interface name cannot be blank");
        }
        Class clazz;
        try {
            clazz = cl.loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            throw new DeploymentException(interfaceType + " interface class not found: " + interfaceName);
        }
        if (!clazz.isInterface()) {
            throw new DeploymentException(interfaceType + " interface is not an interface: " + interfaceName);
        }
        Class superInterface;
        try {
            superInterface = cl.loadClass(superInterfaceName);
        } catch (ClassNotFoundException e) {
            throw new DeploymentException("Class " + superInterfaceName + " could not be loaded");
        }
        if (!superInterface.isAssignableFrom(clazz)) {
            throw new DeploymentException(interfaceType + " interface does not extend " + superInterfaceName + ": " + interfaceName);
        }
        return clazz;
    }


}
