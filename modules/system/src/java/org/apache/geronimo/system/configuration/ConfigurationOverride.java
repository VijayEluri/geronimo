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
package org.apache.geronimo.system.configuration;

import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.kernel.InvalidGBeanException;
import org.apache.geronimo.kernel.repository.Artifact;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @version $Rev$ $Date$
 */
class ConfigurationOverride {
    private final Artifact name;
    private boolean load;
    private final Map gbeans = new LinkedHashMap();

    public ConfigurationOverride(Artifact name, boolean load) {
        this.name = name;
        this.load = load;
    }

    public ConfigurationOverride(ConfigurationOverride base, Artifact name) {
        this.name = name;
        this.load = base.load;
        this.gbeans.putAll(base.gbeans);
    }

    public ConfigurationOverride(Element element) throws InvalidGBeanException {
        name = Artifact.create(element.getAttribute("name"));

        String loadConfigString = element.getAttribute("load");
        load = !"false".equals(loadConfigString);

        NodeList gbeans = element.getElementsByTagName("gbean");
        for (int g = 0; g < gbeans.getLength(); g++) {
            Element gbeanElement = (Element) gbeans.item(g);
            GBeanOverride gbean = new GBeanOverride(gbeanElement);
            addGBean(gbean);
        }
    }

    public Artifact getName() {
        return name;
    }

    public boolean isLoad() {
        return load;
    }

    public void setLoad(boolean load) {
        this.load = load;
    }

    public GBeanOverride getGBean(String gbeanName) {
        return (GBeanOverride) gbeans.get(gbeanName);
    }

    public void addGBean(GBeanOverride gbean) {
        gbeans.put(gbean.getName(), gbean);
    }

    public void addGBean(String gbeanName, GBeanOverride gbean) {
        gbeans.put(gbeanName, gbean);
    }

    public Map getGBeans() {
        return gbeans;
    }

    public GBeanOverride getGBean(AbstractName gbeanName) {
        return (GBeanOverride) gbeans.get(gbeanName);
    }

    public void addGBean(AbstractName gbeanName, GBeanOverride gbean) {
        gbeans.put(gbeanName, gbean);
    }

    public void writeXml(PrintWriter out) {
        out.print("  <configuration name=\"" + name + "\"");
        if (!load) {
            out.print(" load=\"false\"");
        }
        out.println(">");

        // GBeans
        for (Iterator gb = gbeans.entrySet().iterator(); gb.hasNext();) {
            Map.Entry gbean = (Map.Entry) gb.next();

            GBeanOverride gbeanOverride = (GBeanOverride) gbean.getValue();
            gbeanOverride.writeXml(out);
        }

        out.println("  </configuration>");
    }
}
