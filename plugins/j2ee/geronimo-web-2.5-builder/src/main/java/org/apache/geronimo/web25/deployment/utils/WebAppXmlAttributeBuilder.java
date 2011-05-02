/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.geronimo.web25.deployment.utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.stream.XMLStreamReader;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.service.XmlAttributeBuilder;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.web25.deployment.DefaultWebAppInfoFactory;
import org.apache.geronimo.web25.deployment.WebAppInfoBuilder;
import org.apache.openejb.jee.JAXBContextFactory;
import org.apache.openejb.jee.WebApp;
import org.osgi.framework.Bundle;

/**
 * @version $Rev:$ $Date:$
 */

@GBean(j2eeType = "XmlAttributeBuilder")
public class WebAppXmlAttributeBuilder implements XmlAttributeBuilder {

    @Override
    public String getNamespace() {
        return "http://java.sun.com/xml/ns/javaee";
    }

    @Override
    public Object getValue(Object xmlObject, Object enclosing, String s, Bundle bundle) throws DeploymentException {
        return new WebAppInfoBuilder((WebApp)xmlObject, new DefaultWebAppInfoFactory()).build();
//        try {
//            XMLStreamReader reader = enclosing.newXMLStreamReader();
//            while (reader.hasNext() && reader.next() != 1);
//            if (s.endsWith("WebAppInfo")) {
//                WebApp webApp = (WebApp) unmarshalJavaee(WebApp.class, reader);
//                return new WebAppInfoBuilder(webApp, new DefaultWebAppInfoFactory()).build();
//            }
//            throw new DeploymentException("Unrecognized xml: " + enclosing.xmlText());
//        } catch (XMLStreamException e) {
//            throw new DeploymentException("xml problem", e);
//        }
    }

    public static <T>Object unmarshalJavaee(Class<T> type, XMLStreamReader in) throws DeploymentException {

        try {
            JAXBContext ctx = JAXBContextFactory.newInstance(type);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshaller.setEventHandler(new ValidationEventHandler(){
                public boolean handleEvent(ValidationEvent validationEvent) {
                    return false;
                }
            });


            JAXBElement<T> element = unmarshaller.unmarshal(in, type);
            return element.getValue();
        } catch (JAXBException e) {
            throw new DeploymentException("parsing problem", e);
        }
    }

}
