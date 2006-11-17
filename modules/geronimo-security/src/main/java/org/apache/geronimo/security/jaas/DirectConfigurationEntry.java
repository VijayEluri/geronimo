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
package org.apache.geronimo.security.jaas;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.security.jaas.server.JaasLoginModuleConfiguration;


/**
 * Exposes a LoginModule directly to JAAS clients, without any particular
 * wrapping by Geronimo.  You do still need to declare the login module as a
 * GBean, but it's not like it will be run through the login service or
 * anything.
 *
 * @version $Rev$ $Date$
 */
public class DirectConfigurationEntry implements ConfigurationEntryFactory {
    private final String applicationConfigName;
    private final LoginModuleControlFlag controlFlag;
    private final LoginModuleSettings module;

    public DirectConfigurationEntry() {
        this.applicationConfigName = null;
        this.controlFlag = null;
        this.module = null;
    }

    public DirectConfigurationEntry(String applicationConfigName, LoginModuleControlFlag controlFlag, LoginModuleSettings module) {
        this.applicationConfigName = applicationConfigName;
        this.controlFlag = controlFlag;
        this.module = module;
    }

    public String getConfigurationName() {
        return applicationConfigName;
    }

    public JaasLoginModuleConfiguration generateConfiguration() {
        return new JaasLoginModuleConfiguration(module.getLoginModuleClass(), controlFlag, module.getOptions(), module.isServerSide(), applicationConfigName, false, module.getClassLoader());
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(DirectConfigurationEntry.class, NameFactory.CONFIGURATION_ENTRY);
        infoFactory.addInterface(ConfigurationEntryFactory.class);
        infoFactory.addAttribute("applicationConfigName", String.class, true);
        infoFactory.addAttribute("controlFlag", LoginModuleControlFlag.class, true);

        infoFactory.addReference("Module", LoginModuleSettings.class, NameFactory.LOGIN_MODULE);

        infoFactory.setConstructor(new String[]{"applicationConfigName", "controlFlag", "Module"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
