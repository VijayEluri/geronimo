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

import java.util.Map;

import org.apache.catalina.Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;

public class ManagerGBean extends BaseGBean implements GBeanLifecycle, ObjectRetriever{

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String J2EE_TYPE = "Manager";
    
    protected final Manager manager;

    // no-arg constructor required for gbean refs
    public ManagerGBean(){
        manager = null;
    }
    
    protected ManagerGBean(String className) throws Exception{
       super();     
       manager = (Manager)Class.forName(className).newInstance();
    }
    
    public ManagerGBean(String className, 
            Map initParams) throws Exception {
        super(); // TODO: make it an attribute
        //Validate
        if (className == null){
            className = "org.apache.catalina.core.StandardHost";
        }
        
        //Create the Manager object
        manager = (Manager)Class.forName(className).newInstance();
        
        //Set the parameters
        setParameters(manager, initParams);
        
    }
    
    public void doStart() throws Exception {
    }

    public void doStop() throws Exception {
    }

    public void doFail() {
    }

    public Object getInternalObject() {
        // TODO Auto-generated method stub
        return manager;
    }
    
    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("TomcatManager", ManagerGBean.class, J2EE_TYPE);
        infoFactory.addAttribute("className", String.class, true);
        infoFactory.addAttribute("initParams", Map.class, true);
        infoFactory.addOperation("getInternalObject");
        infoFactory.setConstructor(new String[] { 
                "className", 
                "initParams"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
