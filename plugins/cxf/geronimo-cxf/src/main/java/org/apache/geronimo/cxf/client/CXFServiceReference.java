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

package org.apache.geronimo.cxf.client;

import java.net.URI;
import java.util.Map;

import javax.naming.NamingException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.HandlerResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.javaee.HandlerChainsType;
import org.apache.geronimo.cxf.CXFHandlerResolver;
import org.apache.geronimo.cxf.CXFWebServiceContainer;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.jaxws.HandlerChainsUtils;
import org.apache.geronimo.jaxws.JAXWSAnnotationProcessor;
import org.apache.geronimo.jaxws.JNDIResolver;
import org.apache.geronimo.jaxws.client.EndpointInfo;
import org.apache.geronimo.jaxws.client.JAXWSServiceReference;

public class CXFServiceReference extends JAXWSServiceReference {

    private static final Log LOG = LogFactory.getLog(CXFServiceReference.class);

    public CXFServiceReference(String serviceClassName,
                               String referenceClassName,
                               URI wsdlURI,
                               QName serviceQName,
                               AbstractName name,
                               String handlerChainsXML,
                               Map<Object, EndpointInfo> seiInfoMap) {
        super(handlerChainsXML, seiInfoMap, name, serviceQName, wsdlURI, referenceClassName, serviceClassName);        
    }
       
    public Object getContent() throws NamingException {
        CXFWebServiceContainer.getDefaultBus();
        Object reference = super.getContent();   
        SAAJInterceptor.registerInterceptors();        
        return reference;
    }
    
    protected HandlerChainsType getHandlerChains() {
        try {
            return HandlerChainsUtils.toHandlerChains(this.handlerChainsXML, HandlerChainsType.class);
        } catch (JAXBException e) {          
            // this should not happen
            LOG.warn("Failed to deserialize handler chains", e);
            return null;
        }
    }

    protected HandlerResolver getHandlerResolver(Class serviceClass) {
        JAXWSAnnotationProcessor annotationProcessor =
                new JAXWSAnnotationProcessor(new JNDIResolver(), new WebServiceContextImpl());
        CXFHandlerResolver handlerResolver =
                new CXFHandlerResolver(classLoader, serviceClass, getHandlerChains(), annotationProcessor);
        return handlerResolver;
    }
}
