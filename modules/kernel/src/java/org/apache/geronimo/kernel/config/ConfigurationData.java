/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.apache.geronimo.kernel.config;

import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.io.File;

/**
 * @version $Rev: 382645 $ $Date$
 */
public class ConfigurationData {

    /**
     * Identifies the type of configuration (WAR, RAR et cetera)
     */
    private final ConfigurationModuleType moduleType;


    /**
     * List of URIs in this configuration's classpath.  These are for the classes directly included in the configuration
     */
    private final LinkedHashSet classPath;

    /**
     * GBeans contained in this configuration.
     */
    private final List gbeans;

    /**
     * Child configurations of this configuration
     */
    private final  List childConfigurations;

    private final Environment environment;

    private final File configurationDir;

    public ConfigurationData(ConfigurationModuleType moduleType, LinkedHashSet classPath, List gbeans, List childConfigurations, Environment environment, File configurationDir) {
        this.moduleType = moduleType;
        if (classPath != null) {
            this.classPath = classPath;
        } else {
            this.classPath = new LinkedHashSet();
        }
        if (gbeans == null) gbeans = Collections.EMPTY_LIST;
        this.gbeans = gbeans;
        this.childConfigurations = childConfigurations;
        this.environment = environment;
        this.configurationDir = configurationDir;
    }

    public Artifact getId() {
        return environment.getConfigId();
    }

    public ConfigurationModuleType getModuleType() {
        return moduleType;
    }

    public List getClassPath() {
        return Collections.unmodifiableList(new ArrayList(classPath));
    }

    public List getGBeans() {
        return Collections.unmodifiableList(gbeans);
    }

    public List getChildConfigurations() {
        return Collections.unmodifiableList(childConfigurations);
    }

    public Environment getEnvironment() {
        return environment;
    }

    public File getConfigurationDir() {
        return configurationDir;
    }

}
