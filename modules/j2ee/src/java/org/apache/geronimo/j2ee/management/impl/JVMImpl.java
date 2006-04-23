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

package org.apache.geronimo.j2ee.management.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.Stats;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.ObjectNameUtil;
import org.apache.geronimo.management.StatisticsProvider;
import org.apache.geronimo.management.geronimo.JVM;
import org.apache.geronimo.management.stats.BoundedRangeImpl;
import org.apache.geronimo.management.stats.JVMStatsImpl;
import org.apache.geronimo.system.logging.SystemLog;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class JVMImpl implements JVM, StatisticsProvider {
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");
    public static final String NODE;
    private static final Runtime runtime = Runtime.getRuntime();

    static {
        String node;
        try {
            node = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            node = null;
        }
        NODE = node;
    }

    private final String objectName;
    private final Kernel kernel;
    private final SystemLog systemLog;
    private JVMStatsImpl stats;

    public JVMImpl(String objectName, Kernel kernel, SystemLog systemLog) {
        this.objectName = objectName;
        this.kernel = kernel;
        this.systemLog = systemLog;
        ObjectName myObjectName = ObjectNameUtil.getObjectName(this.objectName);
        verifyObjectName(myObjectName);
    }

    /**
     * ObjectName must match this pattern:
     * <p/>
     * domain:j2eeType=JVM,name=MyName
     */
    private void verifyObjectName(ObjectName objectName) {
        if (objectName.isPattern()) {
            throw new InvalidObjectNameException("ObjectName can not be a pattern", objectName);
        }
        Hashtable keyPropertyList = objectName.getKeyPropertyList();
        if (!"JVM".equals(keyPropertyList.get("j2eeType"))) {
            throw new InvalidObjectNameException("JVM object name j2eeType property must be 'JVM'", objectName);
        }
        if (!keyPropertyList.containsKey("name")) {
            throw new InvalidObjectNameException("JVM object must contain a name property", objectName);
        }
        if (!keyPropertyList.containsKey("J2EEServer")) {
            throw new InvalidObjectNameException("JVM object must contain a J2EEServer property", objectName);
        }
        if (keyPropertyList.size() != 3) {
            throw new InvalidObjectNameException("JVM object name can only have J2EEServer, j2eeType, and name", objectName);
        }
    }

    public String getObjectName() {
        return objectName;
    }

    public boolean isStateManageable() {
        return true;
    }

    public boolean isStatisticsProvider() {
        return true;
    }

    public boolean isEventProvider() {
        return true;
    }

    /**
     * The version of the JVMImpl we are running on.
     * This is the value of java.version system property
     * @see "JSR77.3.4.1.1"
     * @return the JVMImpl version
     */
    public String getJavaVersion() {
        return JAVA_VERSION;
    }

    /**
     * The vendor of the JVMImpl we are running on.
     * This is the value of java.vendor system property
     * @see "JSR77.3.4.1.2"
     * @return the JVMImpl version
     */
    public String getJavaVendor() {
        return JAVA_VENDOR;
    }

    /**
     * The node we are running on.
     * This is the fully qualified host name returned for InetAddress.getLocalHost.toString();
     * we return null if there is no network
     * @see "JSR77.3.4.1.3"
     * @return the node we are running on
     */
    public String getNode() {
        return NODE;
    }

    public int getAvailableProcessors() {
        return runtime.availableProcessors();
    }

    public Date getKernelBootTime() {
        return kernel.getBootTime();
    }

    public Stats getStats() {
        BoundedRangeImpl heap;
        if(stats == null) {
            stats = new JVMStatsImpl();
            long start = kernel.getBootTime().getTime();
            stats.getUpTimeImpl().setCount(start);
            stats.getUpTimeImpl().setStartTime(start);
            heap = stats.getHeapSizeImpl();
            heap.setStartTime(start);
            heap.setBounds(0, runtime.totalMemory());
            heap.setCurrent(heap.getUpperBound() - runtime.freeMemory());
            heap.setLowWaterMark(heap.getCurrent());
            heap.setHighWaterMark(heap.getCurrent());
        } else {
            heap = stats.getHeapSizeImpl();
            heap.setBounds(0, runtime.totalMemory());
            heap.setCurrent(heap.getUpperBound() - runtime.freeMemory());
        }
        long now = System.currentTimeMillis();
        stats.getUpTimeImpl().setLastSampleTime(now);
        heap.setLastSampleTime(now);
        return stats;
    }

    public Properties getSystemProperties() {
        return System.getProperties();
    }

    public SystemLog getSystemLog() {
        return systemLog;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(JVMImpl.class, NameFactory.JVM);
        infoFactory.addReference("SystemLog", SystemLog.class);
        infoFactory.setConstructor(new String[] {"objectName", "kernel", "SystemLog"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
