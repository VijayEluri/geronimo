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

package org.apache.geronimo.kernel.basic;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.runtime.GBeanInstance;
import org.apache.geronimo.kernel.DependencyManager;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.InternalKernelException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelGBean;
import org.apache.geronimo.kernel.KernelRegistry;
import org.apache.geronimo.kernel.NoSuchAttributeException;
import org.apache.geronimo.kernel.NoSuchOperationException;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.management.State;
import org.apache.geronimo.kernel.lifecycle.LifecycleMonitor;
import org.apache.geronimo.kernel.proxy.ProxyManager;


/**
 * The core of a Geronimo instance.
 * A Kernel is responsible for managing the Configurations that comprise a
 * Geronimo system and exposing them using JMX. Each Kernel is associated
 * with an MBeanServer that is used to register the Configurations themselves
 * and the MBeans they define.
 * <p/>
 * Dependencies between MBeans are handled by a dedicated DependencyManager
 * that is responsible for tracking those dependencies and ensuring that the
 * dependent objects follow the appropriate lifecycle and receive appropriate
 * notifications.
 * <p/>
 * The Kernel also provides a ConfigurationStore which is used to stage
 * installed Configurations (providing a local filesystem based classpath) and
 * used hold the persistent state of each Configuration. This allows
 * Configurations to restart in the event of system failure.
 * 
 * TODO: Describe the order of method invocation (e.g. if loadGbean may be before boot)
 *
 * @version $Rev:386276 $ $Date$
 */
public class BasicKernel implements Kernel {
    /**
     * Helper objects for invoke and getAttribute
     */
    private static final String[] NO_TYPES = new String[0];
    private static final Object[] NO_ARGS = new Object[0];

    /**
     * Name of this kernel
     */
    private final String kernelName;

    /**
     * The log
     */
    private Log log;

    /**
     * Is this kernel running?
     */
    private boolean running;

    /**
     * The timestamp when the kernel was started
     */
    private Date bootTime;

    /**
     * The registry
     */
    private final BasicRegistry registry;

    /**
     * Listeners for when the kernel shutdown
     */
    private final LinkedList shutdownHooks = new LinkedList();

    /**
     * This manager is used by the kernel to manage dependencies between gbeans
     */
    private DependencyManager dependencyManager;

    /**
     * Monitors the lifecycle of all gbeans.
     */
    private BasicLifecycleMonitor lifecycleMonitor;
    private LifecycleMonitor publicLifecycleMonitor;

    /**
     * This factory gbean proxies, and tracks all proxies in the system
     */
    private ProxyManager proxyManager;

    private static final Naming INSTANCE = new Jsr77Naming();

    /**
     * Construct a Kernel with the specified name.
     *
     * @param kernelName the name of the kernel
     */
    public BasicKernel(String kernelName) {
        if (kernelName.indexOf(':') >= 0 || kernelName.indexOf('*') >= 0 || kernelName.indexOf('?') >= 0) {
            throw new IllegalArgumentException("Kernel name may not contain a ':', '*' or '?' character");
        }
        this.kernelName = kernelName;
        this.registry = new BasicRegistry();
    }

    public String getKernelName() {
        return kernelName;
    }

    public Naming getNaming() {
        return INSTANCE;
    }

    /**
     * @deprecated this will be removed as when we add generalized dependencies to gbeans... the only current user is Configuration
     */
    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    /**
     * Gets the lifecycle monitor.
     * @deprecated don't use this yet... it may change or go away
     */
    public LifecycleMonitor getLifecycleMonitor() {
        return publicLifecycleMonitor;
    }

    /**
     * Gets the proxy manager.
     * @deprecated don't use this yet... it may change or go away
     */
    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public Object getAttribute(ObjectName objectName, String attributeName) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(objectName));
        return gbeanInstance.getAttribute(attributeName);
    }

    public Object getAttribute(AbstractName abstractName, String attributeName) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(abstractName);
        return gbeanInstance.getAttribute(attributeName);
    }

    public Object getAttribute(String shortName, String attributeName) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        return getAttribute(shortName, null, attributeName);
    }

    public Object getAttribute(Class type, String attributeName) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        return getAttribute(null, type, attributeName);
    }

    public Object getAttribute(String shortName, Class type, String attributeName) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.getAttribute(attributeName);
    }

    public void setAttribute(ObjectName objectName, String attributeName, Object attributeValue) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(objectName));
        gbeanInstance.setAttribute(attributeName, attributeValue);
    }

    public void setAttribute(AbstractName abstractName, String attributeName, Object attributeValue) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(abstractName);
        gbeanInstance.setAttribute(attributeName, attributeValue);
    }

    public void setAttribute(String shortName, String attributeName, Object attributeValue) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        setAttribute(shortName, null, attributeName, attributeValue);
    }

    public void setAttribute(Class type, String attributeName, Object attributeValue) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        setAttribute(null, type, attributeName, attributeValue);
    }

    public void setAttribute(String shortName, Class type, String attributeName, Object attributeValue) throws GBeanNotFoundException, NoSuchAttributeException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        gbeanInstance.setAttribute(attributeName, attributeValue);
    }

    public Object invoke(ObjectName objectName, String methodName) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(objectName, methodName, NO_ARGS, NO_TYPES);
    }

    public Object invoke(AbstractName abstractName, String methodName) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(abstractName, methodName, NO_ARGS, NO_TYPES);
    }

    public Object invoke(String shortName, String methodName) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(shortName, null, methodName, NO_ARGS, NO_TYPES);
    }

    public Object invoke(Class type, String methodName) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(null, type, methodName, NO_ARGS, NO_TYPES);
    }

    public Object invoke(String shortName, Class type, String methodName) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(shortName, type, methodName, NO_ARGS, NO_TYPES);
    }

    public Object invoke(ObjectName objectName, String methodName, Object[] args, String[] types) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(objectName));
        return gbeanInstance.invoke(methodName, args, types);
    }

    public Object invoke(AbstractName abstractName, String methodName, Object[] args, String[] types) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(abstractName);
        return gbeanInstance.invoke(methodName, args, types);
    }

    public Object invoke(String shortName, String methodName, Object[] args, String[] types) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(shortName, null, methodName, args, types);
    }

    public Object invoke(Class type, String methodName, Object[] args, String[] types) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        return invoke(null, type,methodName, args, types);
    }

    public Object invoke(String shortName, Class type, String methodName, Object[] args, String[] types) throws GBeanNotFoundException, NoSuchOperationException, InternalKernelException, Exception {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.invoke(methodName, args, types);
    }

    public boolean isLoaded(ObjectName name) {
        return registry.isRegistered(createGBeanName(name));
    }

    public boolean isLoaded(AbstractName name) {
        return registry.isRegistered(name);
    }

    public boolean isLoaded(String shortName) {
        return isLoaded(shortName, null);
    }

    public boolean isLoaded(Class type) {
        return isLoaded(null, type);
    }

    public boolean isLoaded(String shortName, Class type) {
        try {
            registry.getGBeanInstance(shortName, type);
            return true;
        } catch (GBeanNotFoundException e) {
            // Dain: yes this is flow control using exceptions, but I'm too lazy to add another isRegistered method to the basic registry
            return false;
        }
    }

    public Object getGBean(String shortName) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        return getGBean(shortName, null);
    }

    public Object getGBean(Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        return getGBean(null, type);
    }

    public Object getGBean(String shortName, Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        if (gbeanInstance.getState() != State.RUNNING_INDEX) {
            throw new IllegalStateException("GBean is not running: " + gbeanInstance.getAbstractName());
        }
        return gbeanInstance.getTarget();
    }

    public Object getGBean(ObjectName name) throws GBeanNotFoundException, InternalKernelException, IllegalStateException  {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(name));
        if (gbeanInstance.getState() != State.RUNNING_INDEX) {
            throw new IllegalStateException("GBean is not running: " + name);
        }
        return gbeanInstance.getTarget();
    }

    public Object getGBean(AbstractName name) throws GBeanNotFoundException, InternalKernelException, IllegalStateException  {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        if (gbeanInstance.getState() != State.RUNNING_INDEX) {
            throw new IllegalStateException("GBean is not running: " + name);
        }
        return gbeanInstance.getTarget();
    }

    public GBeanInfo getGBeanInfo(ObjectName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(name));
        return gbeanInstance.getGBeanInfo();
    }

    public GBeanInfo getGBeanInfo(AbstractName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        return gbeanInstance.getGBeanInfo();
    }

    public GBeanInfo getGBeanInfo(String shortName) throws GBeanNotFoundException {
        return getGBeanInfo(shortName, null);
    }

    public GBeanInfo getGBeanInfo(Class type) throws GBeanNotFoundException {
        return getGBeanInfo(null, type);
    }

    public GBeanInfo getGBeanInfo(String shortName, Class type) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.getGBeanInfo();
    }

    public GBeanData getGBeanData(ObjectName name) throws GBeanNotFoundException, InternalKernelException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(name));
        return gbeanInstance.getGBeanData();
    }

    public GBeanData getGBeanData(AbstractName name) throws GBeanNotFoundException, InternalKernelException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        return gbeanInstance.getGBeanData();
    }

    public GBeanData getGBeanData(String shortName) throws GBeanNotFoundException, InternalKernelException {
        return getGBeanData(shortName, null);
    }

    public GBeanData getGBeanData(Class type) throws GBeanNotFoundException, InternalKernelException {
        return getGBeanData(null, type);
    }

    public GBeanData getGBeanData(String shortName, Class type) throws GBeanNotFoundException, InternalKernelException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.getGBeanData();
    }

    public void loadGBean(GBeanData gbeanData, ClassLoader classLoader) throws GBeanAlreadyExistsException, InternalKernelException {
        AbstractName abstractName = gbeanData.getAbstractName();
        Set interfaces = gbeanData.getGBeanInfo().getInterfaces();
        GBeanInstance gbeanInstance = new GBeanInstance(gbeanData, this, dependencyManager, lifecycleMonitor.createLifecycleBroadcaster(abstractName, interfaces), classLoader);
        registry.register(gbeanInstance);
    }

    public void startGBean(AbstractName name) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        gbeanInstance.start();
    }

    public void startGBean(String shortName) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        startGBean(shortName, null);
    }

    public void startGBean(Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        startGBean(null, type);
    }

    public void startGBean(String shortName, Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        gbeanInstance.start();
    }

    public void startRecursiveGBean(AbstractName name) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        gbeanInstance.startRecursive();
    }

    public void startRecursiveGBean(String shortName) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        startRecursiveGBean(shortName, null);
    }

    public void startRecursiveGBean(Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        startRecursiveGBean(null, type);
    }

    public void startRecursiveGBean(String shortName, Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        gbeanInstance.startRecursive();
    }

    public boolean isRunning(AbstractName name) {
        try {
            GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
            return gbeanInstance.getState() == State.RUNNING_INDEX;
        } catch (GBeanNotFoundException e) {
            return false;
        }
    }

    public boolean isRunning(String shortName) {
        return isRunning(shortName, null);
    }

    public boolean isRunning(Class type) {
        return isRunning(null, type);
    }

    public boolean isRunning(String shortName, Class type) {
        try {
            GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
            return gbeanInstance.getState() == State.RUNNING_INDEX;
        } catch (GBeanNotFoundException e) {
            return false;
        }
    }

    public void stopGBean(AbstractName name) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        gbeanInstance.stop();
    }

    public void stopGBean(String shortName) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        stopGBean(shortName, null);
    }

    public void stopGBean(Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        stopGBean(null, type);
    }

    public void stopGBean(String shortName, Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        gbeanInstance.stop();
    }

    public void unloadGBean(AbstractName name) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        gbeanInstance.die();
        registry.unregister(name);
    }

    public void unloadGBean(String shortName) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        unloadGBean(shortName, null);
    }

    public void unloadGBean(Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        unloadGBean(null, type);
    }

    public void unloadGBean(String shortName, Class type) throws GBeanNotFoundException, InternalKernelException, IllegalStateException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        AbstractName name = gbeanInstance.getAbstractName();
        gbeanInstance.die();
        registry.unregister(name);
    }

    public int getGBeanState(ObjectName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(name));
        return gbeanInstance.getState();
    }

    public int getGBeanState(AbstractName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        return gbeanInstance.getState();
    }

    public int getGBeanState(String shortName) throws GBeanNotFoundException {
        return getGBeanState(shortName, null);
    }

    public int getGBeanState(Class type) throws GBeanNotFoundException {
        return getGBeanState(null, type);
    }

    public int getGBeanState(String shortName, Class type) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.getState();
    }

    public long getGBeanStartTime(AbstractName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        return gbeanInstance.getStartTime();
    }

    public long getGBeanStartTime(String shortName) throws GBeanNotFoundException {
        return getGBeanStartTime(shortName, null);
    }

    public long getGBeanStartTime(Class type) throws GBeanNotFoundException {
        return getGBeanStartTime(null, type);
    }

    public long getGBeanStartTime(String shortName, Class type) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.getStartTime();
    }

    public Set listGBeans(ObjectName pattern) {
        String domain = (pattern == null || pattern.isDomainPattern()) ? null : pattern.getDomain();
        if (domain != null && domain.length() == 0) {
            domain = kernelName;
        }

        Map props = pattern == null ? null : pattern.getKeyPropertyList();
        Set gbeans = registry.listGBeans(domain, props);
        Set result = new HashSet(gbeans.size());
        for (Iterator i = gbeans.iterator(); i.hasNext();) {
            GBeanInstance instance = (GBeanInstance) i.next();
            result.add(instance.getObjectNameObject());
        }
        return result;
    }

    public Set listGBeans(Set patterns) {
        Set gbeans = new HashSet();
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            Object pattern = iterator.next();
            if (pattern instanceof ObjectName) {
                gbeans.addAll(listGBeans((ObjectName)pattern));
            } else if (pattern instanceof AbstractNameQuery) {
                gbeans.addAll(listGBeans((AbstractNameQuery)pattern));
            }
        }
        return gbeans;
    }

    public Set listGBeans(AbstractNameQuery query) {
        Set gbeans = registry.listGBeans(query);
        Set result = new HashSet(gbeans.size());
        for (Iterator i = gbeans.iterator(); i.hasNext();) {
            GBeanInstance instance = (GBeanInstance) i.next();
            result.add(instance.getAbstractName());
        }
        return result;
    }

    public Set listGBeansByInterface(String[] interfaces) {
        Set gbeans = new HashSet();
        Set all = listGBeans((AbstractNameQuery)null);
        for (Iterator it = all.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            try {
                GBeanInfo info = getGBeanInfo(name);
                Set intfs = info.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    String candidate = interfaces[i];
                    if(intfs.contains(candidate)) {
                        gbeans.add(name);
                        break;
                    }
                }
            } catch (GBeanNotFoundException e) {}
        }
        return gbeans;
    }

    public AbstractName getAbstractNameFor(Object service) {
        if(!running) {
            return null;
        }

        // check if service is a proxy
        AbstractName name = proxyManager.getProxyTarget(service);
        if (name != null) {
            return name;
        }

        // try the registry
        GBeanInstance gbeanInstance = registry.getGBeanInstanceByInstance(service);
        if (gbeanInstance != null) {
            return gbeanInstance.getAbstractName();
        }

        // didn't fing the name
        return null;
    }

    public String getShortNameFor(Object service) {
        AbstractName name = getAbstractNameFor(service);
        if (name != null) {
            return (String) name.getName().get("name");
        }
        return null;
    }

    /**
     * Boot this Kernel, triggering the instantiation of the MBeanServer and DependencyManager,
     * and the registration of ConfigurationStore
     *
     * @throws java.lang.Exception if the boot fails
     */
    public void boot() throws Exception {
        if (running) {
            return;
        }
        bootTime = new Date();
        log = LogFactory.getLog(BasicKernel.class.getName());
        log.debug("Starting boot");

        // todo cleanup when boot fails
        KernelRegistry.registerKernel(this);

        registry.start(this);

        lifecycleMonitor = new BasicLifecycleMonitor(this);
        publicLifecycleMonitor = new LifecycleMonitorFlyweight(lifecycleMonitor);
        dependencyManager = new BasicDependencyManager(publicLifecycleMonitor);
        proxyManager = new BasicProxyManager(this);

        // load and start the kernel gbean
        GBeanData kernelGBeanData = new GBeanData(KERNEL_NAME, KernelGBean.GBEAN_INFO);
        loadGBean(kernelGBeanData, getClass().getClassLoader());
        startGBean(KERNEL_NAME);

        running = true;
        log.debug("Booted");
    }

    public Date getBootTime() {
        return bootTime;
    }

    public void registerShutdownHook(Runnable hook) {
        assert hook != null : "Shutdown hook was null";
        synchronized (shutdownHooks) {
            shutdownHooks.add(hook);
        }
    }

    public void unregisterShutdownHook(Runnable hook) {
        synchronized (shutdownHooks) {
            shutdownHooks.remove(hook);
        }
    }

    /**
     * Shut down this kernel instance, unregistering the MBeans and releasing
     * the MBeanServer.
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        log.debug("Starting kernel shutdown");

        notifyShutdownHooks();

        registry.stop();

        dependencyManager.close();
        dependencyManager = null;

        synchronized (this) {
            notify();
        }

        KernelRegistry.unregisterKernel(this);

        log.debug("Kernel shutdown complete");
    }

    private void notifyShutdownHooks() {
        while (!shutdownHooks.isEmpty()) {
            Runnable hook;
            synchronized (shutdownHooks) {
                hook = (Runnable) shutdownHooks.removeFirst();
            }
            try {
                hook.run();
            } catch (Throwable e) {
                log.warn("Error from kernel shutdown hook", e);
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public ClassLoader getClassLoaderFor(ObjectName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(createGBeanName(name));
        return gbeanInstance.getClassLoader();
    }

    public ClassLoader getClassLoaderFor(AbstractName name) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(name);
        return gbeanInstance.getClassLoader();
    }

    public ClassLoader getClassLoaderFor(String shortName) throws GBeanNotFoundException {
        return getClassLoaderFor(shortName, null);
    }

    public ClassLoader getClassLoaderFor(Class type) throws GBeanNotFoundException {
        return getClassLoaderFor(null, type);
    }

    public ClassLoader getClassLoaderFor(String shortName, Class type) throws GBeanNotFoundException {
        GBeanInstance gbeanInstance = registry.getGBeanInstance(shortName, type);
        return gbeanInstance.getClassLoader();
    }

    private GBeanName createGBeanName(ObjectName objectName) {
        if (objectName.getDomain().length() == 0) {
            return new GBeanName(kernelName, objectName.getKeyPropertyList());
        }
        return new GBeanName(objectName);
    }


    /**
     * @deprecated Experimental feature
     */
    public String getStateReason(AbstractName abstractName) {
        try {
            GBeanInstance gbeanInstance = registry.getGBeanInstance(abstractName);
            return gbeanInstance.getStateReason();
        } catch (GBeanNotFoundException e) {
            return null;
        }
    }
}
