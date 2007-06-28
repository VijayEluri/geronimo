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
package org.apache.geronimo.system.jmx;

import java.util.ArrayList;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;

/**
 * Creates a real mbean server of finds an existing one with the specified mbeanServerId
 * @version $Rev$ $Date$
 */
public class RealMBeanServerReference implements MBeanServerReference {
    private static final String GERONIMO_DEFAULT_DOMAIN = "geronimo";
    
    private MBeanServer mbeanServer;

    public RealMBeanServerReference(String mbeanServerId) throws MBeanServerNotFound {
        ArrayList servers = MBeanServerFactory.findMBeanServer(mbeanServerId);
        if (servers.size() == 0) {
            mbeanServer = MBeanServerFactory.createMBeanServer(GERONIMO_DEFAULT_DOMAIN);
        } else if (servers.size() > 1) {
            throw new MBeanServerNotFound(servers.size() + " MBeanServers were found with the agent id " + mbeanServerId);
        } else {
            mbeanServer = (MBeanServer) servers.get(0);
        }
    }
    
    /**
     * Finds an existing MBeanServer with default domain GERONIMO_DEFAULT_DOMAIN
     * or creates a new one if there isn't any.
     */
    public RealMBeanServerReference() {
        // Find all MBeanServers
        ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        for(MBeanServer server: servers) {
            // Look for one with default domain GERONIMO_DEFAULT_DOMAIN
            if (GERONIMO_DEFAULT_DOMAIN.equals(server.getDefaultDomain())) {
                mbeanServer = server;
                break;
            }
        }
        if(mbeanServer == null) {
            // No MBeanServer with default domain GERONIMO_DEFAULT_DOMAIN exists. Create one.
            mbeanServer = MBeanServerFactory.createMBeanServer(GERONIMO_DEFAULT_DOMAIN);
        }
    }

    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(RealMBeanServerReference.class);
        infoFactory.addAttribute("mbeanServerId", String.class, true);
        //infoFactory.setConstructor(new String[]{"mbeanServerId"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
