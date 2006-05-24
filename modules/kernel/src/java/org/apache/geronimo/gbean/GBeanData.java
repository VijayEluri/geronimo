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
package org.apache.geronimo.gbean;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @version $Rev$ $Date$
 */
public class GBeanData implements Externalizable {
    private GBeanInfo gbeanInfo;
    private final Map attributes;
    private final Map references;
    private final Set dependencies;
    private AbstractName abstractName;

    public GBeanData() {
        attributes = new HashMap();
        references = new HashMap();
        dependencies = new HashSet();
    }

    public GBeanData(GBeanInfo gbeanInfo) {
        this();
        this.gbeanInfo = gbeanInfo;
    }

    public GBeanData(AbstractName abstractName, GBeanInfo gbeanInfo) {
        this();
        this.abstractName = abstractName;
        this.gbeanInfo = gbeanInfo;
    }

    public GBeanData(GBeanData gbeanData) {
        gbeanInfo = gbeanData.gbeanInfo;
        attributes = new HashMap(gbeanData.attributes);
        references = new HashMap(gbeanData.references);
        dependencies = new HashSet(gbeanData.dependencies);
        abstractName = gbeanData.abstractName;
    }

    public AbstractName getAbstractName() {
        return abstractName;
    }

    public void setAbstractName(AbstractName abstractName) {
        this.abstractName = abstractName;
    }

    public GBeanInfo getGBeanInfo() {
        return gbeanInfo;
    }

    public void clearAttribute(String name) {
        attributes.remove(name);
    }

    public void clearReference(String name) {
        references.remove(name);
    }

    public void setGBeanInfo(GBeanInfo gbeanInfo) {
        this.gbeanInfo = gbeanInfo;
    }

    public Map getAttributes() {
        return new HashMap(attributes);
    }

    public Set getAttributeNames() {
        return new HashSet(attributes.keySet());
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Map getReferences() {
        return new HashMap(references);
    }

    public Set getReferencesNames() {
        return new HashSet(references.keySet());
    }

    public ReferencePatterns getReferencePatterns(String name) {
        return (ReferencePatterns) references.get(name);
    }

    public void setReferencePattern(String name, AbstractNameQuery pattern) {
        setReferencePatterns(name, Collections.singleton(pattern));
    }

    public void setReferencePattern(String name, AbstractName abstractName) {
        setReferencePatterns(name, new ReferencePatterns(abstractName));
    }

    public void setReferencePatterns(String name, Set patterns) {
        setReferencePatterns(name, new ReferencePatterns(patterns));
    }

    public void setReferencePatterns(String name, ReferencePatterns patterns) {
        references.put(name, patterns);
    }

    public Set getDependencies() {
        return new HashSet(dependencies);
    }

    public void setDependencies(Set dependencies) {
        this.dependencies.clear();
        addDependencies(dependencies);
    }

    public void addDependencies(Set dependencies) {
        for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
            Object dependency = iterator.next();
            if (dependency instanceof AbstractName) {
                AbstractName name = (AbstractName) dependency;
                addDependency(name);
            } else if (dependency instanceof AbstractNameQuery) {
                AbstractNameQuery nameQuery = (AbstractNameQuery) dependency;
                addDependency(nameQuery);
            } else if (dependency instanceof ReferencePatterns) {
                ReferencePatterns referencePatterns = (ReferencePatterns) dependency;
                addDependency(referencePatterns);
            } else {
                throw new IllegalArgumentException("Unknown dependency type: " + dependency);
            }
        }
    }

    public void addDependency(ReferencePatterns dependency) {
        this.dependencies.add(dependency);
    }

    public void addDependency(AbstractNameQuery refInfo) {
        this.dependencies.add(new ReferencePatterns(refInfo));
    }

    public void addDependency(AbstractName dependency) {
        this.dependencies.add(new ReferencePatterns(dependency));
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        // write the gbean info
        out.writeObject(gbeanInfo);

        // write the abstract name
        out.writeObject(abstractName);

        // write the attributes
        out.writeInt(attributes.size());
        for (Iterator iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            try {
                out.writeObject(name);
                out.writeObject(value);
            } catch (IOException e) {
                throw (IOException) new IOException("Unable to write attribute: " + name).initCause(e);
            }
        }

        // write the references
        out.writeInt(references.size());
        for (Iterator iterator = references.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            ReferencePatterns value = (ReferencePatterns) entry.getValue();
            try {
                out.writeObject(name);
                out.writeObject(value);
            } catch (IOException e) {
                throw (IOException) new IOException("Unable to write reference pattern: " + name).initCause(e);
            }
        }
        //write the dependencies
        out.writeInt(dependencies.size());
        for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
            ReferencePatterns referencePatterns = (ReferencePatterns) iterator.next();
            out.writeObject(referencePatterns);
        }

    }


    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // read the gbean info
        gbeanInfo = (GBeanInfo) in.readObject();

        // read the abstract name
        try {
            abstractName = (AbstractName) in.readObject();
        } catch (IOException e) {
            throw (IOException) new IOException("Unable to deserialize AbstractName for GBeanData of type " + gbeanInfo.getClassName()).initCause(e);
        }


        try {
            // read the attributes
            int attributeCount = in.readInt();
            for (int i = 0; i < attributeCount; i++) {
                String attributeName = (String) in.readObject();
                Object attributeValue = null;
                try {
                    attributeValue = in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new ClassNotFoundException("Unable to find class used in GBeanData " + abstractName + ", attribute: " + attributeName, e);
                } catch (IOException e) {
                    throw (IOException) new IOException("Unable to deserialize GBeanData " + abstractName + ", attribute: " + attributeName).initCause(e);
                }
                setAttribute(attributeName, attributeValue);
            }

            // read the references
            int endpointCount = in.readInt();
            for (int i = 0; i < endpointCount; i++) {
                String referenceName = (String) in.readObject();
                ReferencePatterns referencePattern;
                try {
                    referencePattern = (ReferencePatterns) in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new ClassNotFoundException("Unable to find class used in GBeanData " + abstractName + ", reference: " + referenceName, e);
                } catch (IOException e) {
                    throw (IOException) new IOException("Unable to deserialize GBeanData " + abstractName + ", reference: " + referenceName).initCause(e);
                }
                setReferencePatterns(referenceName, referencePattern);
            }

            //read the dependencies
            int dependencyCount = in.readInt();
            for (int i = 0; i < dependencyCount; i++) {
                ReferencePatterns depdendencyPattern = (ReferencePatterns) in.readObject();
                dependencies.add(depdendencyPattern);
            }
        } catch (IOException e) {
            throw (IOException) new IOException("Unable to deserialize GBeanData " + abstractName).initCause(e);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Unable to find class used in GBeanData " + abstractName, e);
        }
    }

}

