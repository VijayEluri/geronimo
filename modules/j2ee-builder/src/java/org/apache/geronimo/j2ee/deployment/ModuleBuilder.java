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
package org.apache.geronimo.j2ee.deployment;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.Repository;

import java.io.File;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @version $Rev$ $Date$
 */
public interface ModuleBuilder {
    Module createModule(File plan, JarFile moduleFile) throws DeploymentException;

    Module createModule(Object plan, JarFile moduleFile, String targetPath, URL specDDUrl, Environment environment, Object moduleContextInfo) throws DeploymentException;

    void installModule(JarFile earFile, EARContext earContext, Module module, ConfigurationStore configurationStore) throws DeploymentException;

    void initContext(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException;

    void addGBeans(EARContext earContext, Module module, ClassLoader cl, Repository repository) throws DeploymentException;

    String getSchemaNamespace();
}
