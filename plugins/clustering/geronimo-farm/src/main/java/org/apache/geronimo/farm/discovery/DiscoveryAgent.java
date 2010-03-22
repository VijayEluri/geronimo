/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.farm.discovery;

import java.net.URI;
import java.io.IOException;

/**
 * @version $Rev$ $Date$
 */
public interface DiscoveryAgent {
    /**
     * Sets the discovery listener
     * @param listener listener for addeed/removed services
     */
    void setDiscoveryListener(DiscoveryListener listener);

    /**
     * register a service
     * @param serviceUri uri for new service. sample service uri:
     *    farm:rmi://localhost:1109/JMXConnector?cluster=cluster1
     *    cluster1:ejb:ejbd://localhost:4201
     * 
     * @throws java.io.IOException on error
     */
    void registerService(URI serviceUri) throws IOException;

    /**
     * register a service
     * @param serviceUri uri for removed service
     * @throws java.io.IOException on error
     */
    void unregisterService(URI serviceUri) throws IOException;

    /**
     * A process actively using a service may see it go down before the DiscoveryAgent notices the
     * service's failure.  That process can use this method to notify the DiscoveryAgent of the failure
     * so that other listeners of this DiscoveryAgent can also be made aware of the failure.
     * @param serviceUri uri for failed service
     * @throws java.io.IOException on error
     */
    void reportFailed(URI serviceUri) throws IOException;

}
