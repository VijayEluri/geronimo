/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.geronimo.testsupport;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * @version $Rev: 514087 $ $Date: 2007-03-03 01:13:40 -0500 (Sat, 03 Mar 2007) $
 */
public class DOMUtils {

    public static Document load(String xml) throws Exception {
        DocumentBuilder builder = getDocumentBuilder();         
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        return document;
    }

    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder;
    }
    
    public static void compareNodes(Node expected, Node actual) throws Exception {
        if (expected.getNodeType() != actual.getNodeType()) {
            throw new Exception("Different types of nodes: " + expected + " " + actual);
        }
        if (expected instanceof Document) {
            Document expectedDoc = (Document)expected;
            Document actualDoc = (Document)actual;
            compareNodes(expectedDoc.getDocumentElement(), actualDoc.getDocumentElement());            
        } else if (expected instanceof Element) {
            Element expectedElement = (Element)expected;
            Element actualElement = (Element)actual;
            
            // compare element names
            if (!expectedElement.getLocalName().equals(actualElement.getLocalName())) {
                throw new Exception("Element names do match: " + expectedElement.getLocalName() + " " + actualElement.getLocalName());
            }   
            // compare element ns
            String expectedNS = expectedElement.getNamespaceURI();
            String actualNS = actualElement.getNamespaceURI();
            if ((expectedNS != actualNS) || (expectedNS != null && !expectedNS.equals(actualNS))) {               
                throw new Exception("Element namespaces names do match: " + expectedNS + " " + actualNS);
            }
            
            String elementName = "{" + expectedElement.getNamespaceURI() + "}" + actualElement.getLocalName();
            
            // compare attributes
            NamedNodeMap expectedAttrs = expectedElement.getAttributes();
            NamedNodeMap actualAttrs = actualElement.getAttributes();
            if (expectedAttrs.getLength() != actualAttrs.getLength()) {
                throw new Exception(elementName + ": Number of attributes do not match up: " + expectedAttrs.getLength() + " " + actualAttrs.getLength());
            }
            for (int i = 0; i < expectedAttrs.getLength(); i++) {
                Attr expectedAttr = (Attr)expectedAttrs.item(i);
                Attr actualAttr = null;
                if (expectedAttr.getNamespaceURI() == null) {
                    actualAttr = (Attr)actualAttrs.getNamedItem(expectedAttr.getName());
                } else {
                    actualAttr = (Attr)actualAttrs.getNamedItemNS(expectedAttr.getNamespaceURI(), expectedAttr.getLocalName());
                }
                if (actualAttr == null) {
                    throw new Exception(elementName + ": No attribute found:" + expectedAttr);
                }
                if (!expectedAttr.getValue().equals(actualAttr.getValue())) {
                    throw new Exception(elementName + ": Attribute values do not match: " + expectedAttr.getValue() + " " + actualAttr.getValue());
                }
            }            
            
            // compare children
            NodeList expectedChildren = expectedElement.getChildNodes();
            NodeList actualChildren = actualElement.getChildNodes();
            if (expectedChildren.getLength() != actualChildren.getLength()) {
                throw new Exception(elementName + ": Number of children do not match up: " + expectedChildren.getLength() + " " + actualChildren.getLength());
            }
            for (int i = 0; i < expectedChildren.getLength(); i++) {
                Node expectedChild = expectedChildren.item(i);
                Node actualChild = actualChildren.item(i);
                compareNodes(expectedChild, actualChild);
            }
        } else if (expected instanceof Text) {
            String expectedData = ((Text)expected).getData();
            String actualData = ((Text)actual).getData();
            
            if (!expectedData.equals(actualData)) {
                throw new Exception("Text does not match: " + expectedData + " " + actualData);
            }
        }
    }       

}
