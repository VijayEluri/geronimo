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
package org.apache.geronimo.deployment.plugin.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.deployment.ModuleConfigurer;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;

/**
 * Connects to a kernel in the same VM.
 *
 * @version $Rev$ $Date$
 */
public class LocalDeploymentManager extends JMXDeploymentManager {
    private static final Log log = LogFactory.getLog(LocalDeploymentManager.class);
    private static final AbstractNameQuery CONFIGURER_QUERY = new AbstractNameQuery(ModuleConfigurer.class.getName());

    public LocalDeploymentManager(Kernel kernel) {
        super(loadModuleConfigurers(kernel));
        initialize(kernel);
    }
    
    private static Collection<ModuleConfigurer> loadModuleConfigurers(Kernel kernel) {
        Collection<ModuleConfigurer> moduleConfigurers = new ArrayList<ModuleConfigurer>();
        Set configurerNames = kernel.listGBeans(CONFIGURER_QUERY);
        for (Object configurerName : configurerNames) {
            AbstractName name = (AbstractName) configurerName;
            try {
                ModuleConfigurer configurer = (ModuleConfigurer) kernel.getGBean(name);
                moduleConfigurers.add(configurer);
            } catch (GBeanNotFoundException e) {
                log.warn("No gbean found for name returned in query : " + name);
            }
        }
        return moduleConfigurers;
    }
    
}
