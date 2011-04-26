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
package org.apache.geronimo.system.configuration;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import junit.framework.TestCase;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GReferenceInfo;
import org.apache.geronimo.gbean.ReferencePatterns;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.osgi.MockBundle;
import org.apache.geronimo.kernel.repository.Artifact;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @version $Rev$ $Date$
 */
public class LocalAttributeManagerTest extends TestCase {
//    private static final String basedir = System.getProperties().getProperty("basedir", ".");

    private LocalAttributeManager localAttributeManager;
    private Artifact configurationName;
    private AbstractName gbeanName;
    private GAttributeInfo attributeInfo;
    private GAttributeInfo encryptedAttributeInfo;
    private GReferenceInfo referenceInfo;
    private Bundle bundle = new MockBundle(getClass().getClassLoader(), "", 0L);

    public void testConfigurationShouldLoad() throws Exception {
        // should load by default
        Set<GBeanData> originalDatas = new HashSet<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        originalDatas.add(gbeanData);

        Set<GBeanData> newDatas;
        newDatas = new HashSet<GBeanData>(localAttributeManager.applyOverrides(configurationName, originalDatas, bundle));
        assertEquals(1, newDatas.size());
        assertEquals(originalDatas, newDatas);

        // declare an attribute value so this configuration will exist in the store
        String attributeValue = "attribute value";
        localAttributeManager.addConfiguration(configurationName);
        localAttributeManager.setValue(configurationName, gbeanName, attributeInfo, attributeValue, bundle);

        // should still load
        newDatas = new HashSet<GBeanData>(localAttributeManager.applyOverrides(configurationName, originalDatas, bundle));
        assertEquals(1, newDatas.size());
        assertEquals(originalDatas, newDatas);

        // remove the configuration from the store
        localAttributeManager.removeConfiguration(configurationName);

        // should still get the same gbeans, config list and gbean attribute override functions are separate interfaces.
        newDatas = new HashSet<GBeanData>(localAttributeManager.applyOverrides(configurationName, originalDatas, bundle));
        assertEquals(1, newDatas.size());
        assertEquals(originalDatas, newDatas);
    }

    public void testGBeanShouldLoad() throws Exception {
        ObjectName objectName = ObjectName.getInstance(":name=gbean2");
        AbstractName gbeanName2 = new AbstractName(configurationName, objectName.getKeyPropertyList(), objectName);

        // should load by default
        Set<GBeanData> originalDatas = new HashSet<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        GBeanData gbeanData2 = new GBeanData(gbeanName2, GBEAN_INFO);
        originalDatas.add(gbeanData);
        originalDatas.add(gbeanData2);

        Set<GBeanData> newDatas;
        newDatas = new HashSet<GBeanData>(localAttributeManager.applyOverrides(configurationName, originalDatas, bundle));
        assertEquals(2, newDatas.size());
        assertEquals(originalDatas, newDatas);

        // declare an attribute value so this configuration will exist in the store
        String attributeValue = "attribute value";
        localAttributeManager.addConfiguration(configurationName);
        localAttributeManager.setValue(configurationName, gbeanName, attributeInfo, attributeValue, bundle);

        // should still load
        newDatas = new HashSet<GBeanData>(localAttributeManager.applyOverrides(configurationName, originalDatas, bundle));
        assertEquals(2, newDatas.size());
        assertEquals(originalDatas, newDatas);

        // set the gbean to not load
        localAttributeManager.setShouldLoad(configurationName, gbeanName, false);

        // should not load
        newDatas = new HashSet<GBeanData>(localAttributeManager.applyOverrides(configurationName, originalDatas, bundle));
        assertEquals(1, newDatas.size());
        GBeanData newGBeanData = newDatas.iterator().next();
        assertSame(gbeanData2, newGBeanData);
        assertEquals(attributeValue, gbeanData.getAttribute(attributeInfo.getName()));
    }

    public void testSetAttribute() throws Exception {
        String attributeValue = "attribute value";
        localAttributeManager.setValue(configurationName, gbeanName, attributeInfo, attributeValue, bundle);
        Collection<GBeanData> gbeanDatas = new ArrayList<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        gbeanDatas.add(gbeanData);
        gbeanDatas = localAttributeManager.applyOverrides(configurationName, gbeanDatas, bundle);
        assertEquals(attributeValue, gbeanData.getAttribute(attributeInfo.getName()));
        StringWriter w = new StringWriter();
        localAttributeManager.write(w);
        assertTrue(w.toString().contains(attributeValue));
    }

    public void testEncryptedAttribute() throws Exception {
        String attributeValue = "attribute value";
        localAttributeManager.setValue(configurationName, gbeanName, encryptedAttributeInfo, attributeValue, bundle);
        Collection<GBeanData> gbeanDatas = new ArrayList<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        gbeanDatas.add(gbeanData);
        gbeanDatas = localAttributeManager.applyOverrides(configurationName, gbeanDatas, bundle);
        assertEquals(attributeValue, gbeanData.getAttribute(encryptedAttributeInfo.getName()));
        StringWriter w = new StringWriter();
        localAttributeManager.write(w);
        assertFalse(w.toString().contains(attributeValue));
    }

    public void testSetReference() throws Exception {
        ObjectName referencePatternObjectName = new ObjectName(":name=referencePattern");
        AbstractName referencePattern = new AbstractName(configurationName, referencePatternObjectName.getKeyPropertyList(), referencePatternObjectName);
        ReferencePatterns referencePatterns = new ReferencePatterns(referencePattern);
        localAttributeManager.setReferencePatterns(configurationName, gbeanName, referenceInfo, referencePatterns);
        Collection<GBeanData> gbeanDatas = new ArrayList<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        gbeanDatas.add(gbeanData);
        gbeanDatas = localAttributeManager.applyOverrides(configurationName, gbeanDatas, bundle);
        assertEquals(referencePatterns, gbeanData.getReferencePatterns(referenceInfo.getName()));
    }

    public void testSetReferences() throws Exception {
        Naming naming = new Jsr77Naming();

        AbstractName referencePattern1 = naming.createRootName(gbeanName.getArtifact(), "name", "referencePattern1");
        AbstractName referencePattern2 = naming.createRootName(gbeanName.getArtifact(), "name", "referencePattern2");
        ReferencePatterns referencePatterns = new ReferencePatterns(new LinkedHashSet(Arrays.asList(new AbstractName[]{referencePattern1, referencePattern2})));
        localAttributeManager.setReferencePatterns(configurationName, gbeanName, referenceInfo, referencePatterns);
        Collection<GBeanData> gbeanDatas = new ArrayList<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        gbeanDatas.add(gbeanData);
        gbeanDatas = localAttributeManager.applyOverrides(configurationName, gbeanDatas, bundle);
        assertEquals(referencePatterns, gbeanData.getReferencePatterns(referenceInfo.getName()));
    }

    public void testAddGBean() throws Exception {
        String attributeValue = "attribute value";
        AbstractNameQuery referencePattern = new AbstractNameQuery(LocalAttributeManagerTest.class.getName());

        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        gbeanData.setAttribute(attributeInfo.getName(), attributeValue);
        gbeanData.setReferencePattern(referenceInfo.getName(), referencePattern);
        localAttributeManager.addConfiguration(configurationName);
        localAttributeManager.addGBean(configurationName, gbeanData, bundle);


        Collection<GBeanData> gbeanDatas = new ArrayList<GBeanData>();
        gbeanDatas = localAttributeManager.applyOverrides(configurationName, gbeanDatas, bundle);
        assertEquals(1, gbeanDatas.size());
        GBeanData newGBeanData = gbeanDatas.iterator().next();

        assertNotSame(gbeanData, newGBeanData);
        assertSame(gbeanData.getGBeanInfo(), newGBeanData.getGBeanInfo());
        assertSame(gbeanData.getAbstractName(), newGBeanData.getAbstractName());
        assertEquals(Collections.singleton(referencePattern), newGBeanData.getReferencePatterns(referenceInfo.getName()).getPatterns());
        assertEquals(attributeValue, newGBeanData.getAttribute(attributeInfo.getName()));
    }

    public void testBadGBeanSpec() throws Exception {
        String attributeValue = "attribute value";
        localAttributeManager.addConfiguration(configurationName);
        localAttributeManager.setValue(configurationName, gbeanName, attributeInfo, attributeValue, bundle);
        try {
            localAttributeManager.applyOverrides(configurationName, Collections.<GBeanData>emptySet(), bundle);
            fail("no gbeans were specified in the 'plan' so overrides should fail");
        } catch (InvalidConfigException e) {
            //pass
        }
    }

    public void testMigrate() throws Exception {
        String attributeValue = "attribute value";
        AbstractNameQuery referencePattern = new AbstractNameQuery(LocalAttributeManagerTest.class.getName());

        localAttributeManager.addConfiguration(configurationName);
        localAttributeManager.setValue(configurationName, gbeanName, attributeInfo, attributeValue, bundle);

        Collection<GBeanData> gbeanDatas = new ArrayList<GBeanData>();
        GBeanData gbeanData = new GBeanData(gbeanName, GBEAN_INFO);
        gbeanDatas.add(gbeanData);
        gbeanDatas = localAttributeManager.applyOverrides(configurationName, gbeanDatas, bundle);
        assertEquals(attributeValue, gbeanData.getAttribute(attributeInfo.getName()));

        Artifact newArtifact = Artifact.create("configuration/name/2/car");
        localAttributeManager.migrateConfiguration(configurationName, newArtifact, null);
        ObjectName objectName = ObjectName.getInstance(":name=gbean,parent="+newArtifact+",foo=bar");
        AbstractName newGBeanName = new AbstractName(newArtifact, objectName.getKeyPropertyList(), objectName);

        gbeanDatas = new ArrayList<GBeanData>();
        gbeanData = new GBeanData(newGBeanName, GBEAN_INFO);
        gbeanDatas.add(gbeanData);
        gbeanDatas = localAttributeManager.applyOverrides(newArtifact, gbeanDatas, bundle);
        assertEquals(attributeValue, gbeanData.getAttribute(attributeInfo.getName()));
    }

//    public void testSwitchableLocalAttributeManager() throws Exception {
//        GBeanInfo gBeanInfo = SwitchableLocalAttributeManager.getGBeanInfo();
//    }

    protected void setUp() throws Exception {
        super.setUp();
        localAttributeManager = new LocalAttributeManager();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(LocalAttributeManager.READ_ONLY_KEY, false);
        props.put(LocalAttributeManager.PREFIX_KEY, "org.apache.geronimo.config.substitution.");
        final Configuration  configuration = new Configuration() {

            private Hashtable<String, Object> dictionary = new Hashtable<String, Object>();

            @Override
            public String getPid() {
                return null;
            }

            @Override
            public Dictionary getProperties() {
                Hashtable<String, Object> d = new Hashtable<String, Object>();
                d.putAll(props);
                return d;
            }

            @Override
            public void update(Dictionary dictionary) throws IOException {
                this.dictionary.clear();
                this.dictionary.putAll((Map<String, Object>)dictionary);
            }

            @Override
            public void delete() throws IOException {
            }

            @Override
            public String getFactoryPid() {
                return null;
            }

            @Override
            public void update() throws IOException {
            }

            @Override
            public void setBundleLocation(String s) {
            }

            @Override
            public String getBundleLocation() {
                return null;
            }
        };
        ConfigurationAdmin ca = new ConfigurationAdmin() {

            @Override
            public Configuration createFactoryConfiguration(String s) throws IOException {
                return null;
            }

            @Override
            public Configuration createFactoryConfiguration(String s, String s1) throws IOException {
                return null;
            }

            @Override
            public Configuration getConfiguration(String s, String s1) throws IOException {
                return null;
            }

            @Override
            public Configuration getConfiguration(String s) throws IOException {
                return configuration;
            }

            @Override
            public Configuration[] listConfigurations(String s) throws IOException, InvalidSyntaxException {
                return new Configuration[0];
            }
        };
        localAttributeManager.setConfigurationAdmin(ca);
        localAttributeManager.activate(props);

        configurationName = Artifact.create("configuration/name/1/car");
        ObjectName objectName = ObjectName.getInstance(":name=gbean,parent="+configurationName+",foo=bar");
        gbeanName = new AbstractName(configurationName, objectName.getKeyPropertyList(), objectName);
        attributeInfo = GBEAN_INFO.getAttribute("attribute");
        encryptedAttributeInfo = GBEAN_INFO.getAttribute("secret");
        referenceInfo = GBEAN_INFO.getReference("reference");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        localAttributeManager = null;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(LocalAttributeManagerTest.class);
        infoFactory.addReference("reference", String.class);
        infoFactory.addAttribute("attribute", String.class, true);
        infoFactory.addAttribute("secret", String.class, true, true, true);
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public String getAttribute() {
        throw new UnsupportedOperationException("Fake method for gbean info");
    }

    public void setAttribute(String attribute) {
        throw new UnsupportedOperationException("Fake method for gbean info");
    }

    public void setReference(String reference) {
        throw new UnsupportedOperationException("Fake method for gbean info");
    }
}
