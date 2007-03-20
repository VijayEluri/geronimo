/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geronimo.axis2.ejb;

import org.apache.geronimo.axis2.Axis2WebServiceContainer;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.jaxws.PortInfo;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.openejb.EjbDeployment;
import org.apache.geronimo.webservices.SoapHandler;

import javax.naming.Context;
import java.net.URL;

/**
 * @version $Rev$ $Date$
 */
public class EJBWebServiceGBean implements GBeanLifecycle {

    private SoapHandler soapHandler;
    private String location;

    public EJBWebServiceGBean(EjbDeployment ejbDeploymentContext,
                              PortInfo portInfo,                              
                              Kernel kernel,
                              URL configurationBaseUrl,                              
                              SoapHandler soapHandler,
                              String securityRealmName,
                              String realmName,
                              String transportGuarantee,
                              String authMethod,
                              String[] virtualHosts) throws Exception {        
        if (ejbDeploymentContext == null || soapHandler == null || portInfo == null) {
            return;
        }
                
        this.location = portInfo.getLocation();
        
        assert this.location != null : "null location received";
                
        String beanClassName = ejbDeploymentContext.getBeanClass().getName();    
        Context context = ejbDeploymentContext.getComponentContext();
        
        ClassLoader classLoader = ejbDeploymentContext.getClassLoader();
        
        //TODO: need to invoke the EJB container and forward the call to the EJB container.
        
        Axis2WebServiceContainer container = 
            new EJBWebServiceContainer(portInfo, beanClassName, classLoader, context, configurationBaseUrl);
         
        if (soapHandler != null) {
            soapHandler.addWebService(this.location, 
                                      virtualHosts, 
                                      container, 
                                      securityRealmName, 
                                      realmName, 
                                      transportGuarantee, 
                                      authMethod, 
                                      classLoader);
        }
        
    }

    public void doStart() throws Exception {
    }

    public void doStop() throws Exception {        
        if (this.soapHandler != null) {
            this.soapHandler.removeWebService(this.location);
        }        
    }

    public void doFail() {
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(EJBWebServiceGBean.class, EJBWebServiceGBean.class, NameFactory.WEB_SERVICE_LINK);
        
        infoFactory.addReference("EjbDeployment", EjbDeployment.class);
        infoFactory.addAttribute("portInfo", PortInfo.class, true);       
        infoFactory.addAttribute("kernel", Kernel.class, false);
        infoFactory.addAttribute("configurationBaseUrl", URL.class, true);
        infoFactory.addAttribute("securityRealmName", String.class, true);
        infoFactory.addAttribute("realmName", String.class, true);
        infoFactory.addAttribute("transportGuarantee", String.class, true);
        infoFactory.addAttribute("authMethod", String.class, true);
        infoFactory.addAttribute("virtualHosts", String[].class, true);
        infoFactory.addReference("WebServiceContainer", SoapHandler.class);
        
        infoFactory.setConstructor(new String[]{
                "EjbDeployment",
                "portInfo",
                "kernel",
                "configurationBaseUrl",
                "WebServiceContainer",
                "securityRealmName",
                "realmName",
                "transportGuarantee",
                "authMethod",
                "virtualHosts"
        });

        
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
