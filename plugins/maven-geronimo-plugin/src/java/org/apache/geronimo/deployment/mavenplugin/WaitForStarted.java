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
package org.apache.geronimo.deployment.mavenplugin;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.geronimo.deployment.plugin.factories.DeploymentFactoryImpl;
import org.apache.geronimo.kernel.jmx.KernelDelegate;
import org.apache.geronimo.kernel.jmx.KernelMBean;
import org.apache.geronimo.kernel.management.State;

public class WaitForStarted extends AbstractModuleCommand {

    private int maxTries = 40;

    private MBeanServerConnection mbServerConnection;
    private KernelMBean kernel;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void execute() throws Exception {
        String uri = getUri().substring(DeploymentFactoryImpl.URI_PREFIX.length());
        if (!uri.startsWith("jmx")) {
            throw new Exception("bad uri");
        }

        Map environment = new HashMap();
        String[] credentials = new String[]{getUsername(), getPassword()};
        environment.put(JMXConnector.CREDENTIALS, credentials);

        JMXServiceURL address = new JMXServiceURL("service:" + uri);
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            for (int tries = maxTries; true; tries--) {
                try {
                    JMXConnector jmxConnector = JMXConnectorFactory.connect(address, environment);
                    mbServerConnection = jmxConnector.getMBeanServerConnection();
                    kernel = new KernelDelegate(mbServerConnection);
                    break;
                } catch (Exception e) {
                    if (tries == 0) {
                        throw new Exception("Could not connect");
                    }
                    Thread.sleep(1000);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldcl);
        }
        URI id = new URI(getId());
        for (int tries = maxTries; tries > 0; tries--) {
            int state = kernel.getConfigurationState(id);
            if (state == State.RUNNING_INDEX) {
                return;
            }
            Thread.sleep(1000);
        }
        throw new Exception("Configuration is not yet started");
    }

}
