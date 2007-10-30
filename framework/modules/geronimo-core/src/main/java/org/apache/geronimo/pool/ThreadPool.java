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

package org.apache.geronimo.pool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.BoundedRangeStatistic;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.Stats;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;

import org.apache.geronimo.management.J2EEManagedObject;
import org.apache.geronimo.management.StatisticsProvider;
import org.apache.geronimo.management.geronimo.stats.ThreadPoolStats;
import org.apache.geronimo.management.stats.BoundedRangeStatisticImpl;
import org.apache.geronimo.management.stats.CountStatisticImpl;
import org.apache.geronimo.management.stats.StatsImpl;

/**
 * @version $Rev$ $Date$
 */
public class ThreadPool implements GeronimoExecutor, GBeanLifecycle, J2EEManagedObject, StatisticsProvider {
    private ThreadPoolExecutor executor;
    private ClassLoader classLoader;
    private ObjectName objectName;
    private boolean waitWhenBlocked;
    
    // Statistics-related fields follow
    private boolean statsActive = true;
    private PoolStatsImpl stats = new PoolStatsImpl();
    private Map clients = new HashMap();

    public ThreadPool(int poolSize, String poolName, long keepAliveTime, ClassLoader classLoader, String objectName) {
        ThreadPoolExecutor p = new ThreadPoolExecutor(
            poolSize, // core size
            poolSize, // max size
            keepAliveTime, TimeUnit.MILLISECONDS,
            new SynchronousQueue());

        p.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        p.setThreadFactory(new ThreadPoolThreadFactory(poolName, classLoader));
        
        try {
            this.objectName = ObjectName.getInstance(objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Bad object name injected: " + e.getMessage(), e);
        }

        executor = p;
        this.classLoader = classLoader;

        // set pool stats start time
        stats.setStartTime();
    }

    public String getName() {
        return objectName.getKeyProperty("name");
    }

    public String getObjectName() {
        return objectName.getCanonicalName();
    }

    public boolean isEventProvider() {
        return true;
    }

    public boolean isStateManageable() {
        return true;
    }

    public boolean isStatisticsProvider() {
        return true;
    }

    public Stats getStats() {
        stats.threadsInUse.setLowerBound(0);
        stats.threadsInUse.setUpperBound(executor.getMaximumPoolSize());
        int inUse = executor.getPoolSize();
        stats.threadsInUse.setCurrent(inUse);
        if (inUse < stats.threadsInUse.getLowWaterMark()) {
            stats.threadsInUse.setLowWaterMark(inUse);
        }
        if (inUse > stats.threadsInUse.getHighWaterMark()) {
            stats.threadsInUse.setHighWaterMark(inUse);
        }
        if (statsActive) {
            synchronized (this) {
                stats.prepareConsumers(clients);
            }
        } else {
            stats.prepareConsumers(Collections.EMPTY_MAP);
        }
        // set last sapmle time
        stats.setLastSampleTime();
        return stats;
    }

    /**
     * Reset all statistics in PoolStatsImpl object
     */
    public void resetStats() {
        stats.threadsInUse.setLowerBound(0);
        stats.threadsInUse.setUpperBound(0);
        stats.threadsInUse.setCurrent(0);
        stats.threadsInUse.setLowWaterMark(0);
        stats.threadsInUse.setHighWaterMark(0);
        stats.setStartTime();
    }

    public static class PoolStatsImpl extends StatsImpl implements ThreadPoolStats {
        private BoundedRangeStatisticImpl threadsInUse = new BoundedRangeStatisticImpl(
                "Threads In Use", "",
                "The number of threads in use by this thread pool");
        private Map consumers = new HashMap();

        public PoolStatsImpl() {
            addStat(threadsInUse.getName(), threadsInUse);
        }

        public BoundedRangeStatistic getThreadsInUse() {
            return threadsInUse;
        }

        public CountStatistic getCountForConsumer(String consumer) {
            return (CountStatistic) consumers.get(consumer);
        }

        public String[] getThreadConsumers() {
            return (String[]) consumers.keySet().toArray(new String[consumers.size()]);
        }

        public void prepareConsumers(Map clients) {
            Map result = new HashMap();
            for (Iterator it = clients.keySet().iterator(); it.hasNext();) {
                String client = (String) it.next();
                Integer count = (Integer) clients.get(client);
                CountStatisticImpl stat = (CountStatisticImpl) consumers.get(client);
                if (stat == null) {
                    stat = new CountStatisticImpl("Threads for " + client, "", "The number of threads used by the client known as '" + client + "'", count.intValue());
                    addStat(stat.getName(), stat);
                } else {
                    consumers.remove(client);
                    stat.setCount(count.intValue());
                }
                result.put(client, stat);
            }
            for (Iterator it = consumers.keySet().iterator(); it.hasNext();) {
                String client = (String) it.next();
                removeStat(((CountStatisticImpl) consumers.get(client)).getName());
            }
            consumers = result;
        }
    }


    public int getPoolSize() {
        return executor.getPoolSize();
    }

    public int getMaximumPoolSize() {
        return executor.getMaximumPoolSize();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public void execute(Runnable command) {
        execute("Unknown", command);
    }

    public void execute(final String consumerName, final Runnable runnable) {
        Runnable command;
        if (statsActive) {
            command = new Runnable() {
                public void run() {
                    startWork(consumerName);
                    try {
                        runnable.run();
                    } finally {
                        finishWork(consumerName);
                    }
                }
            };
        } else {
            command = runnable;
        }

        ThreadPoolExecutor p;
        synchronized (this) {
            p = executor;
        }
        if (p == null) {
            throw new IllegalStateException("ThreadPool has been stopped");
        }
        Runnable task = new ContextClassLoaderRunnable(command, classLoader);
        p.execute(task);
    }

    private synchronized void startWork(String consumerName) {
        Integer test = (Integer) clients.get(consumerName);
        if (test == null) {
            clients.put(consumerName, new Integer(1));
        } else {
            clients.put(consumerName, new Integer(test.intValue() + 1));
        }
    }

    private synchronized void finishWork(String consumerName) {
        Integer test = (Integer) clients.get(consumerName);
        if (test.intValue() == 1) {
            clients.remove(consumerName);
        } else {
            clients.put(consumerName, new Integer(test.intValue() - 1));
        }
    }
    
    private static class WaitWhenBlockedPolicy
        implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) throws RejectedExecutionException {
            try {
                executor.getQueue().put(r);
            }
            catch (InterruptedException e) {
                throw new RejectedExecutionException(e);
            }
        }
    }
    
    public void setWaitWhenBlocked(boolean wait) {
        waitWhenBlocked = wait;
        if(wait) {
            executor.setRejectedExecutionHandler(new WaitWhenBlockedPolicy());
        } else {
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        }
    }

    public boolean isWaitWhenBlocked() {
        return waitWhenBlocked;
    }

    public void doStart() throws Exception {
    }

    public void doStop() throws Exception {
        ThreadPoolExecutor p;
        synchronized (this) {
            p = executor;
            executor = null;
            classLoader = null;
        }
        if (p != null) {
            p.shutdownNow();
        }
    }

    public void doFail() {
        try {
            doStop();
        } catch (Exception e) {
        }
    }

    private static final class ThreadPoolThreadFactory implements ThreadFactory {
        private final String poolName;
        private final ClassLoader classLoader;

        private int nextWorkerID = 0;

        public ThreadPoolThreadFactory(String poolName, ClassLoader classLoader) {
            this.poolName = poolName;
            this.classLoader = classLoader;
        }

        public Thread newThread(Runnable arg0) {
            Thread thread = new Thread(arg0, poolName + " " + getNextWorkerID());
            thread.setContextClassLoader(classLoader);
            return thread;
        }

        private synchronized int getNextWorkerID() {
            return nextWorkerID++;
        }
    }

    private static final class ContextClassLoaderRunnable implements Runnable {
        private Runnable task;
        private ClassLoader classLoader;

        public ContextClassLoaderRunnable(Runnable task, ClassLoader classLoader) {
            this.task = task;
            this.classLoader = classLoader;
        }

        public void run() {
            Runnable myTask = task;
            ClassLoader myClassLoader = classLoader;

            // clear fields so they can be garbage collected
            task = null;
            classLoader = null;

            if (myClassLoader != null) {
                // we asumme the thread classloader is already set to our final class loader
                // because the only to access the thread is wrapped with the Runnable or via the initial thread pool
                try {
                    myTask.run();
                } finally {
                    Thread.currentThread().setContextClassLoader(myClassLoader);
                }
            }
        }
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(ThreadPool.class, "GBean");

        infoFactory.addAttribute("poolSize", int.class, true);
        infoFactory.addAttribute("poolName", String.class, true);
        infoFactory.addAttribute("keepAliveTime", long.class, true);
        infoFactory.addAttribute("waitWhenBlocked", boolean.class, true);

        infoFactory.addAttribute("objectName", String.class, false);
        infoFactory.addAttribute("classLoader", ClassLoader.class, false);

        infoFactory.addInterface(GeronimoExecutor.class);

        infoFactory.setConstructor(new String[]{"poolSize", "poolName", "keepAliveTime", "classLoader", "objectName"});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
