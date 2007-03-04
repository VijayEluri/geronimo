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
package org.apache.geronimo.jetty6.cluster.wadi;

import org.apache.geronimo.clustering.wadi.WADISessionManager;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.jetty6.PreHandler;
import org.apache.geronimo.jetty6.PreHandlerFactory;
import org.codehaus.wadi.core.manager.ClusteredManager;


/**
 * 
 * @version $Rev$ $Date$
 */
public class WADIClusteredPreHandlerFactory implements PreHandlerFactory, GBeanLifecycle {

    private final WADISessionManager sessionManager;
    private ClusteredManager wadiManager;

    public WADIClusteredPreHandlerFactory(WADISessionManager sessionManager) {
        if (null == sessionManager) {
            throw new IllegalArgumentException("sessionManager is required");
        }
        this.sessionManager = sessionManager;
    }
    
    public void doStart() throws Exception {
        wadiManager = sessionManager.getManager();
    }
    
    public void doStop() throws Exception {
    }
    
    public void doFail() {
    }
    
    public PreHandler createHandler() {
        return new WADIClusteredPreHandler(wadiManager);
    }
    
    public static final GBeanInfo GBEAN_INFO;
    public static final String GBEAN_REF_WADI_SESSION_MANAGER = "WADISessionManager";

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("WADI Pre-Handler Factory",
                WADIClusteredPreHandlerFactory.class, NameFactory.GERONIMO_SERVICE);

        infoFactory.addReference(GBEAN_REF_WADI_SESSION_MANAGER, WADISessionManager.class, 
                NameFactory.GERONIMO_SERVICE);

        infoFactory.addInterface(PreHandlerFactory.class);

        infoFactory.setConstructor(new String[]{GBEAN_REF_WADI_SESSION_MANAGER});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
