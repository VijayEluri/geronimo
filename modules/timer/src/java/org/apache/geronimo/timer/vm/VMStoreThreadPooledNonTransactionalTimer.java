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

package org.apache.geronimo.timer.vm;

import javax.transaction.TransactionManager;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.timer.NontransactionalExecutorTaskFactory;
import org.apache.geronimo.timer.PersistentTimer;
import org.apache.geronimo.timer.ThreadPooledTimer;

/**
 *
 *
 * @version $Rev$ $Date$
 *
 * */
public class VMStoreThreadPooledNonTransactionalTimer extends ThreadPooledTimer {

    public VMStoreThreadPooledNonTransactionalTimer(TransactionManager transactionManager, Executor threadPool) {
        super(new NontransactionalExecutorTaskFactory(),
                new VMWorkerPersistence(), threadPool, transactionManager);
    }


    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(VMStoreThreadPooledNonTransactionalTimer.class);
        infoFactory.addInterface(PersistentTimer.class);

        infoFactory.addReference("ThreadPool", Executor.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("TransactionManager", TransactionManager.class, NameFactory.TRANSACTION_MANAGER);

        infoFactory.setConstructor(new String[] {"TransactionManager", "ThreadPool"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
