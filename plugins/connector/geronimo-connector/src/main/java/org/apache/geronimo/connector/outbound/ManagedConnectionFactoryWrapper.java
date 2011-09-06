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

package org.apache.geronimo.connector.outbound;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterAssociation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.ResourceAdapterWrapper;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.DynamicGBean;
import org.apache.geronimo.gbean.DynamicGBeanDelegate;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.naming.ResourceSource;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.management.geronimo.JCAManagedConnectionFactory;

/**
 * @version $Rev$ $Date$
 */
public class ManagedConnectionFactoryWrapper implements GBeanLifecycle, DynamicGBean, JCAManagedConnectionFactory, ResourceSource<ResourceException> {

    private static final Log log = LogFactory.getLog(ManagedConnectionFactoryWrapper.class);

    private final String managedConnectionFactoryClass;
    private final String connectionFactoryInterface;
    private final String[] implementedInterfaces;
    private final String connectionFactoryImplClass;
    private final String connectionInterface;
    private final String connectionImplClass;

    private final LinkedHashSet<Class> allImplementedInterfaces = new LinkedHashSet<Class>();

    private final ResourceAdapterWrapper resourceAdapterWrapper;
    private final ConnectionManagerContainer connectionManagerContainer;

    private ManagedConnectionFactory managedConnectionFactory;

    private DynamicGBeanDelegate delegate;


    private boolean registered = false;
    private final Kernel kernel;
    private final AbstractName abstractName;
    private final String objectName;
    private final ClassLoader classLoader;

    //default constructor for enhancement proxy endpoint
    public ManagedConnectionFactoryWrapper() {
        managedConnectionFactoryClass = null;
        connectionFactoryInterface = null;
        implementedInterfaces = null;
        connectionFactoryImplClass = null;
        connectionInterface = null;
        connectionImplClass = null;
        kernel = null;
        abstractName = null;
        objectName = null;
        classLoader = null;
        resourceAdapterWrapper = null;
        connectionManagerContainer = null;
    }

    public ManagedConnectionFactoryWrapper(String managedConnectionFactoryClass,
                                           String connectionFactoryInterface,
                                           String[] implementedInterfaces,
                                           String connectionFactoryImplClass,
                                           String connectionInterface,
                                           String connectionImplClass,
                                           ResourceAdapterWrapper resourceAdapterWrapper,
                                           ConnectionManagerContainer connectionManagerContainer,
                                           Kernel kernel,
                                           AbstractName abstractName,
                                           String objectName,
                                           ClassLoader cl) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        this.managedConnectionFactoryClass = managedConnectionFactoryClass;
        this.connectionFactoryInterface = connectionFactoryInterface;
        this.implementedInterfaces = implementedInterfaces;
        this.connectionFactoryImplClass = connectionFactoryImplClass;
        this.connectionInterface = connectionInterface;
        this.connectionImplClass = connectionImplClass;

        allImplementedInterfaces.add(cl.loadClass(connectionFactoryInterface));
        for (String interfaceName: implementedInterfaces) {
            allImplementedInterfaces.add(cl.loadClass(interfaceName));
        }

        this.resourceAdapterWrapper = resourceAdapterWrapper;
        this.connectionManagerContainer = connectionManagerContainer;

        //set up that must be done before start
        classLoader = cl;
        Class clazz = cl.loadClass(managedConnectionFactoryClass);
        managedConnectionFactory = (ManagedConnectionFactory) clazz.newInstance();
        delegate = new DynamicGBeanDelegate();
        delegate.addAll(managedConnectionFactory);
        this.kernel = kernel;
        this.abstractName = abstractName;
        this.objectName = objectName;
    }

    public String getManagedConnectionFactoryClass() {
        return managedConnectionFactoryClass;
    }

    public String getConnectionFactoryInterface() {
        return connectionFactoryInterface;
    }

    public String[] getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public String getConnectionFactoryImplClass() {
        return connectionFactoryImplClass;
    }

    public String getConnectionInterface() {
        return connectionInterface;
    }

    public String getConnectionImplClass() {
        return connectionImplClass;
    }

    public ResourceAdapterWrapper getResourceAdapterWrapper() {
        return resourceAdapterWrapper;
    }

    public Object getConnectionManagerContainer() {
        return connectionManagerContainer;
    }

    public void doStart() throws Exception {
        //register with resource adapter if not yet done
        if (!registered && (managedConnectionFactory instanceof ResourceAdapterAssociation)) {
            if (resourceAdapterWrapper == null) {
                throw new IllegalStateException("Managed connection factory expects to be registered with a ResourceAdapter, but there is no ResourceAdapter");
            }
            resourceAdapterWrapper.registerResourceAdapterAssociation((ResourceAdapterAssociation) managedConnectionFactory);
            registered = true;
            log.debug("Registered managedConnectionFactory with ResourceAdapter " + resourceAdapterWrapper.toString());
        }
        connectionManagerContainer.doRecovery(managedConnectionFactory);
    }

    public void doStop() {
    }

    public void doFail() {
        doStop();
    }

    //DynamicGBean implementation
    public Object getAttribute(String name) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader oldTCL = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            return delegate.getAttribute(name);
        } finally {
            thread.setContextClassLoader(oldTCL);
        }
    }

    public void setAttribute(String name, Object value) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader oldTCL = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            //Convert java.util.Properties to java.lang.String
            if(value != null && value instanceof Properties){
                Properties ps = (Properties) value;
                if (!ps.isEmpty()) {
                    String s = null;
                    for (Object o : ps.keySet()) {
                        String k = (String) o;
                        String v = ps.getProperty(k);
                        s = k + "=" + v + ",";
                        log.debug("Setting " + k + "=" + value);
                    }
                    delegate.setAttribute(name, s);
                    log.debug("Setting " + name + " value " + s);
                }                
            } else {                
                delegate.setAttribute(name, value);
                log.debug("Setting " + name + " value " + value);
            }
        } finally {
            thread.setContextClassLoader(oldTCL);
        }
    }

    public Object invoke(String name, Object[] arguments, String[] types) throws Exception {
        //we have no dynamic operations.
        return null;
    }

    public Object getConnectionFactory() throws ResourceException {
        return $getConnectionFactory();
    }

    public Object $getResource() throws ResourceException {
        return $getConnectionFactory();
    }

    public Object $getConnectionFactory() throws ResourceException {
        Object connectionFactory =  connectionManagerContainer.createConnectionFactory(managedConnectionFactory);
        for (Class intf: allImplementedInterfaces) {
            if (!intf.isAssignableFrom(connectionFactory.getClass())) {
                throw new ResourceException("ConnectionFactory does not implement expected interface: " + intf.getName());
            }
        }
        return connectionFactory;
    }

    public ManagedConnectionFactory $getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    /**
     * Gets the config properties in the form of a map where the key is the
     * property name and the value is property type (as a Class).
     */
    public Map<String, Class> getConfigProperties() {
        String[] props = delegate.getProperties();
        Map<String, Class> map = new HashMap<String, Class>();
        for (String prop : props) {
            if (prop.equals("logWriter")) {
                continue;
            }
            map.put(prop, delegate.getPropertyType(prop));
        }
        return map;
    }

    public void setConfigProperty(String property, Object value) throws Exception {
        Class cls = delegate.getPropertyType(property);
        if(value != null && value instanceof String && !cls.getName().equals("java.lang.String")) {
            if(cls.isPrimitive()) {
                if(cls.equals(int.class)) {
                    cls = Integer.class;
                } else if(cls.equals(boolean.class)) {
                    cls = Boolean.class;
                } else if(cls.equals(float.class)) {
                    cls = Float.class;
                } else if(cls.equals(double.class)) {
                    cls = Double.class;
                } else if(cls.equals(long.class)) {
                    cls = Long.class;
                } else if(cls.equals(short.class)) {
                    cls = Short.class;
                } else if(cls.equals(byte.class)) {
                    cls = Byte.class;
                } else if(cls.equals(char.class)) {
                    cls = Character.class;
                }
            }
            Constructor con = cls.getConstructor(new Class[]{String.class});
            value = con.newInstance(new Object[]{value});
        }
        kernel.setAttribute(abstractName, property, value);
    }

    public Object getConfigProperty(String property) throws Exception {
        return delegate.getAttribute(property);
    }

    public String getObjectName() {
        return objectName;
    }

    public boolean isStateManageable() {
        return false;
    }

    public boolean isStatisticsProvider() {
        return false;
    }

    public boolean isEventProvider() {
        return false;
    }
}
