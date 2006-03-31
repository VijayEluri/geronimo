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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.geronimo.kernel.repository.Artifact;

/**
 * Interface to a store for Configurations.
 *
 * @version $Rev$ $Date$
 */
public interface ConfigurationStore {
    /**
     * Move the unpacked configuration directory into this store
     *
     * @param configurationData the configuration data
     * @throws IOException if the direcotyr could not be moved into the store
     * @throws InvalidConfigException if there is a configuration problem within the source direcotry
     */
    void install(ConfigurationData configurationData) throws IOException, InvalidConfigException;

    /**
     * Removes a configuration from the store
     * @param configId the id of the configuration to remove
     * @throws NoSuchConfigException if the configuration is not contained in the store
     * @throws IOException if a problem occurs during the removal
     */
    void uninstall(Artifact configId) throws NoSuchConfigException, IOException;

    /**
     * Loads the specified configuration into the kernel
     * @param configId the id of the configuration to load
     * @return the the configuration object
     * @throws NoSuchConfigException if the configuration is not contained in the kernel
     * @throws IOException if a problem occurs loading the configuration from the store
     * @throws InvalidConfigException if the configuration is corrupt
     */
    ConfigurationData loadConfiguration(Artifact configId) throws NoSuchConfigException, IOException, InvalidConfigException;

    /**
     * Determines if the store contains a configuration with the spedified ID.
     *
     * @param configId the unique ID of the configuration
     * @return true if the store contains the configuration
     */
    boolean containsConfiguration(Artifact configId);

    /**
     * Return the object name for the store.
     *
     * @return the object name for the store
     */
    String getObjectName();

    /**
     * Return the configurations in the store
     *
     * @return a List ConfigurationInfo objects
     */
    List listConfigurations();

    /**
     * Creates an empty diretory for a new configuration with the specified configId
     * @param configId the unique ID of the configuration
     * @return the location of the new directory
     * @throws ConfigurationAlreadyExistsException if the configuration already exists in this store
     */
    File createNewConfigurationDir(Artifact configId) throws ConfigurationAlreadyExistsException;

    /**
     * Locate the classpath component for the supplied uri in the given artifact
     * @param configId
     * @param uri
     * @return URL for the configuration component.
     */
    URL resolve(Artifact configId, URI uri) throws NoSuchConfigException, MalformedURLException;
}
