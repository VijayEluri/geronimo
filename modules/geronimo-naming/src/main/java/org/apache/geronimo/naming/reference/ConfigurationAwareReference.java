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

package org.apache.geronimo.naming.reference;

import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.GBeanNotFoundException;

import java.util.Set;
import java.util.Collections;
import java.util.List;

/**
 * TODO: document me
 *
 * NOTE: this class is serialized when modules are installed.  Any changes should
 * be carefully reviewed to ensure they don't cause deserialization errors!
 *
 * @version $Rev$ $Date$
 */
public abstract class ConfigurationAwareReference extends SimpleAwareReference {
    private static final long serialVersionUID = 283358809226901462L;
    private final Artifact[] configId;
    protected final Set<AbstractNameQuery> abstractNameQueries;

    protected ConfigurationAwareReference(Artifact[] configId, AbstractNameQuery abstractNameQuery) {
        this(configId, Collections.singleton(abstractNameQuery));
    }

    protected ConfigurationAwareReference(Artifact[] configId, Set<AbstractNameQuery> abstractNameQueries) {
        if (configId == null || configId.length == 0) {
            throw new NullPointerException("No configId");
        }
        this.configId = configId;
        this.abstractNameQueries = abstractNameQueries;
    }

    public Configuration getConfiguration() {
        Kernel kernel = getKernel();
        ConfigurationManager configurationManager = ConfigurationUtil.getConfigurationManager(kernel);
        Configuration configuration =  configurationManager.getConfiguration(configId[0]);
        if (configuration == null) {
            throw new IllegalStateException("No configuration found for id: " + configId[0]);
        }
        next: for (int i = 1; i < configId.length; i++) {
            List<Configuration> children = configuration.getChildren();
            for (Configuration child: children) {
                if (child.getId().equals(configId[i])) {
                    configuration = child;
                    break next;
                }
            }
            throw new IllegalStateException("No configuration found for id: " + configId[i]);
        }
        return configuration;
    }

    public AbstractName resolveTargetName() throws GBeanNotFoundException {
        Configuration configuration = getConfiguration();
        try {
            return configuration.findGBean(abstractNameQueries);
        } catch (GBeanNotFoundException e) {
            Set results = getKernel().listGBeans(abstractNameQueries);
            if (results.size() == 1) {
                return (AbstractName) results.iterator().next();
            }
            throw new GBeanNotFoundException("Name query " + abstractNameQueries + " not satisfied in kernel, matches: " + results, e);
        }
    }

}
