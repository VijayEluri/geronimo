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
package org.apache.geronimo.tomcat;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Manager;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.ReferenceCollection;
import org.apache.geronimo.gbean.ReferenceCollectionEvent;
import org.apache.geronimo.gbean.ReferenceCollectionListener;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;

/**
 * @version $Rev$ $Date$
 */
public class EngineGBean extends BaseGBean implements GBeanLifecycle, ObjectRetriever {

    private static final Log log = LogFactory.getLog(EngineGBean.class);

    private static final String NAME = "name";
    private static final String DEFAULTHOST = "defaultHost";

    private final Engine engine;

    public EngineGBean(String className,
            Map initParams,
            HostGBean defaultHost,
            Collection hosts,
            ObjectRetriever realmGBean,
            ValveGBean tomcatValveChain,
//            CatalinaClusterGBean clusterGBean,
            ManagerGBean manager) throws Exception {
        super(); // TODO: make it an attribute

        if (className == null){
            className = "org.apache.geronimo.tomcat.TomcatEngine";
        }

        if (initParams == null){
            throw new IllegalArgumentException("Must have 'name' value in initParams.");
        }
        
        //Be sure the defaulthost has been declared.
        if (defaultHost == null){
            throw new IllegalArgumentException("Must have a 'defaultHost' attribute.");
        }

        //Be sure the name has been declared.
        if (!initParams.containsKey(NAME)){
            throw new IllegalArgumentException("Must have a 'name' value initParams.");
        }
        
        //Deprecate the defaultHost initParam
        if (initParams.containsKey(DEFAULTHOST)){
            log.warn("The " + DEFAULTHOST + " initParams value is no longer used and will be ignored.");
            initParams.remove(DEFAULTHOST);
        }

        engine = (Engine)Class.forName(className).newInstance();

        //Set the parameters
        setParameters(engine, initParams);
        
        //Set realm (must be before Hosts)
        if (realmGBean != null){
            engine.setRealm((Realm)realmGBean.getInternalObject());
        }
        
        //Set the default Host
        final String defaultHostName = ((Host)defaultHost.getInternalObject()).getName();
        engine.setDefaultHost(defaultHostName);
        addHost(defaultHost);

        if (manager != null)
            engine.setManager((Manager)manager.getInternalObject());

        //Add the valve list
        if (engine instanceof StandardEngine){
            if (tomcatValveChain != null){
                ValveGBean valveGBean = tomcatValveChain;
                while(valveGBean != null){
                    ((StandardEngine)engine).addValve((Valve)valveGBean.getInternalObject());
                    valveGBean = valveGBean.getNextValve();
                }
            }
        }

        //Add the hosts
        if (hosts instanceof ReferenceCollection) {
            ReferenceCollection refs = (ReferenceCollection)hosts;
            refs.addReferenceCollectionListener(new ReferenceCollectionListener() {

                public void memberAdded(ReferenceCollectionEvent event) {
                    Object o = event.getMember();
                    ObjectRetriever objectRetriever = (ObjectRetriever) o;
                    String hostName = ((Host)objectRetriever.getInternalObject()).getName();
                    if (!hostName.equals(defaultHostName))
                        addHost(objectRetriever);
                }

                public void memberRemoved(ReferenceCollectionEvent event) {
                    Object o = event.getMember();
                    ObjectRetriever objectRetriever = (ObjectRetriever) o;
                    String hostName = ((Host)objectRetriever.getInternalObject()).getName();
                    if (!hostName.equals(defaultHostName))
                        removeHost(objectRetriever);
                }
            });
            Iterator iterator = refs.iterator();
            while (iterator.hasNext()){
                ObjectRetriever objRetriever = (ObjectRetriever)iterator.next();
                String hostName = ((Host)objRetriever.getInternalObject()).getName();
                if (!hostName.equals(defaultHostName))
                    addHost(objRetriever);
            }
        }


        //Add clustering
//        if (clusterGBean != null){
//            engine.setCluster((Cluster)clusterGBean.getInternalObject());
//        }
    }

    private void removeHost(ObjectRetriever objRetriever) {
        Host host = (Host)objRetriever.getInternalObject();
        engine.removeChild(host);
    }

    private void addHost(ObjectRetriever objRetriever) {
        Host host = (Host)objRetriever.getInternalObject();

        //If we didn't set a realm, then use the default
        if (host.getRealm() == null) {
            host.setRealm(engine.getRealm());
        }
        engine.addChild(host);
    }

    public Object getInternalObject() {
        return engine;
    }

    public void doFail() {
        log.warn("Failed");
    }

    public void doStart() throws Exception {
        log.debug("Started");
    }

    public void doStop() throws Exception {
        log.debug("Stopped");
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("TomcatEngine", EngineGBean.class);
        infoFactory.addAttribute("className", String.class, true);
        infoFactory.addAttribute("initParams", Map.class, true);
        infoFactory.addReference("DefaultHost", HostGBean.class, HostGBean.J2EE_TYPE);
        infoFactory.addReference("Hosts", ObjectRetriever.class, HostGBean.J2EE_TYPE);
        infoFactory.addReference("RealmGBean", ObjectRetriever.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addReference("TomcatValveChain", ValveGBean.class, ValveGBean.J2EE_TYPE);
//        infoFactory.addReference("CatalinaCluster", CatalinaClusterGBean.class, CatalinaClusterGBean.J2EE_TYPE);
        infoFactory.addReference("Manager", ManagerGBean.class, ManagerGBean.J2EE_TYPE);
        infoFactory.addOperation("getInternalObject");
        infoFactory.setConstructor(new String[] { 
                "className", 
                "initParams", 
                "DefaultHost",
                "Hosts", 
                "RealmGBean", 
                "TomcatValveChain",
//                "CatalinaCluster",
                "Manager"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
