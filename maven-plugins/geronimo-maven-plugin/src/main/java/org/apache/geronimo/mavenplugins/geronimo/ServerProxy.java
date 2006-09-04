/*
 *  Copyright 2006 The Apache Software Foundation
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

package org.apache.geronimo.mavenplugins.geronimo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.io.IOException;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.MBeanServerConnection;

//
// FIXME: It should be possible to query state with-out any Geronimo classes,
//        just using JMX interfaces.
//

import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.config.PersistentConfigurationList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper to communicate with a remote server via JMX.
 *
 * @version $Rev$ $Date$
 */
public class ServerProxy
{
    private static final Log log = LogFactory.getLog(ServerProxy.class);

    private JMXServiceURL url;

    private Map environment;

    private MBeanServerConnection mbeanConnection;

    private Throwable lastError;

    public ServerProxy(final JMXServiceURL url, final Map environment) throws Exception {
        init(url, environment);
    }

    public ServerProxy(final int port, final String username, final String password) throws Exception {
        //
        // FIXME: Should be able to pass in hostname too
        //
        
        this("service:jmx:rmi://localhost/jndi/rmi://localhost:" + port + "/JMXConnector", username, password);
    }

    public ServerProxy(final String url, final String username, final String password) throws Exception {
        assert url != null;
        assert username != null;
        assert password != null;
        
        Map env = new HashMap();
        env.put("jmx.remote.credentials", new String[] {username, password});

        init(new JMXServiceURL(url), env);
    }

    private void init(final JMXServiceURL url, final Map environment) throws Exception {
        assert url != null;
        assert environment != null;
        
        this.url = url;
        this.environment = new HashMap();
        this.environment.put("jmx.remote.credentials", new String[] {"system", "manager"});

        log.debug("Initialized with URL: " + url + ", environment: " + environment);
    }

    private MBeanServerConnection getConnection() throws IOException {
        if (this.mbeanConnection == null) {
            log.debug("Connecting to: " + url);
            
            JMXConnector connector = JMXConnectorFactory.connect(url, environment);
            this.mbeanConnection = connector.getMBeanServerConnection();
        }

        return mbeanConnection;
    }

    public boolean isFullyStarted() {
        boolean fullyStarted = true;

        try {
            AbstractNameQuery query = new AbstractNameQuery(PersistentConfigurationList.class.getName());
            Set result = listGBeans(query);
            Iterator iter = result.iterator();
            while (iter.hasNext()) {
                AbstractName name = (AbstractName)iter.next();
                boolean started = getBooleanAttribute(name, "kernelFullyStarted");
                if (!started) {
                    fullyStarted = false;
                    break;
                }
            }
        }
        catch (IOException e) {
            log.debug("Connection failure; ignoring", e);
            fullyStarted = false;
            lastError = e;
        }
        catch (Exception e) {
            log.warn("Unable to determine if the server is fully started", e);
            fullyStarted = false;
            lastError = e;
        }
        
        return fullyStarted;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public void shutdown() {
        log.info("Attempting to shutdown the remote server");
        
        try {
            invoke("shutdown");
        }
        catch (Exception e) {
            log.warn("Unable to shutdown the server", e);
            lastError = e;
        }
    }

    //
    // Kernel invocation helpers
    //

    private Object invoke(final String operation, final Object[] args, final String[] signature) throws Exception {
        assert operation != null;
        assert args != null;
        assert signature != null;

        return getConnection().invoke(Kernel.KERNEL, operation, args, signature);
    }

    private Object invoke(final String operation, final Object[] args) throws Exception {
        assert args != null;

        String[] signature = new String[args.length];
        for (int i=0; i<args.length; i++) {
            signature[i] = args[i].getClass().getName();
        }

        return invoke(operation, args, signature);
    }

    private Object invoke(final String operation) throws Exception {
        return invoke(operation, new Object[0]);
    }

    private Set listGBeans(final AbstractNameQuery query) throws Exception {
        return (Set)invoke("listGBeans", new Object[] { query });
    }

    private Object getAttribute(final AbstractName name, final String attribute) throws Exception {
        assert name != null;
        assert attribute != null;

        return invoke("getAttribute", new Object[] { name, attribute });
    }

    private boolean getBooleanAttribute(final AbstractName name, final String attribute) throws Exception {
        Object obj = getAttribute(name, attribute);
        if (obj instanceof Boolean) {
            return ((Boolean)obj).booleanValue();
        }
        else {
            throw new RuntimeException("Attribute is not of type Boolean: " + attribute);
        }
    }
}
