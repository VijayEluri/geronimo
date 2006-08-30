/**
 *
 * Copyright 2005 The Apache Software Foundation
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

package org.apache.geronimo.connector.work;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.transaction.manager.XAWork;
import EDU.oswego.cs.dl.util.concurrent.Executor;

/**
 * 
 * @version $Revision$
 */
public class GeronimoWorkManagerGBean extends GeronimoWorkManager implements GBeanLifecycle {

    public GeronimoWorkManagerGBean() {
    }

    public GeronimoWorkManagerGBean(Executor sync, Executor start, Executor sched, XAWork xaWork) {
        super(sync, start, sched, xaWork);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(GeronimoWorkManagerGBean.class, NameFactory.JCA_WORK_MANAGER);
        infoFactory.addInterface(GeronimoWorkManager.class);

        infoFactory.addReference("SyncPool", Executor.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("StartPool", Executor.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("ScheduledPool", Executor.class, NameFactory.GERONIMO_SERVICE);

        infoFactory.addReference("TransactionManager", XAWork.class, NameFactory.TRANSACTION_MANAGER);

        infoFactory.setConstructor(new String[]{
            "SyncPool",
            "StartPool",
            "ScheduledPool",
            "TransactionManager"});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }



}
