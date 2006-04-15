/**
 *
 * Copyright 2006 The Apache Software Foundation
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

package org.apache.geronimo.upgrade;

import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlCursor;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.ImportType;
import org.apache.geronimo.deployment.xbeans.EnvironmentType;
import org.apache.geronimo.deployment.service.EnvironmentBuilder;

/**
 * @version $Rev:$ $Date:$
 */
public class Upgrade1_0To1_1 {

    private static final Map NAMESPACE_UPDATES = new HashMap();

    static {
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/application-client", "http://geronimo.apache.org/xml/ns/j2ee/application-client-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/application-client-1.0", "http://geronimo.apache.org/xml/ns/j2ee/application-client-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/application", "http://geronimo.apache.org/xml/ns/j2ee/application-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/application-1.0", "http://geronimo.apache.org/xml/ns/j2ee/application-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/deployment", "http://geronimo.apache.org/xml/ns/deployment-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/deployment-1.0", "http://geronimo.apache.org/xml/ns/deployment-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/connector", "http://geronimo.apache.org/xml/ns/j2ee/connector-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/j2ee/connector-1.0", "http://geronimo.apache.org/xml/ns/j2ee/connector-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/deployment/javabean", "http://geronimo.apache.org/xml/ns/deployment/javabean-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/loginconfig", "http://geronimo.apache.org/xml/ns/loginconfig-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/loginconfig-1.0", "http://geronimo.apache.org/xml/ns/loginconfig-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/naming", "http://geronimo.apache.org/xml/ns/naming-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/naming-1.0", "http://geronimo.apache.org/xml/ns/naming-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/security", "http://geronimo.apache.org/xml/ns/security-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/security-1.0", "http://geronimo.apache.org/xml/ns/security-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web", "http://geronimo.apache.org/xml/ns/j2ee/web-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web-1.0", "http://geronimo.apache.org/xml/ns/j2ee/web-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/jetty", "http://geronimo.apache.org/xml/ns/j2ee/web/jetty-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/jetty-1.0", "http://geronimo.apache.org/xml/ns/j2ee/web/jetty-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/jetty/config", "http://geronimo.apache.org/xml/ns/j2ee/web/jetty/config-1.0");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/tomcat", "http://geronimo.apache.org/xml/ns/j2ee/web/tomcat-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/tomcat-1.0", "http://geronimo.apache.org/xml/ns/j2ee/web/tomcat-1.1");
        NAMESPACE_UPDATES.put("http://geronimo.apache.org/xml/ns/web/tomcat/config", "http://geronimo.apache.org/xml/ns/j2ee/web/tomcat/config-1.0");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/openejb-jar", "http://www.openejb.org/xml/ns/openejb-jar-2.1");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/openejb-jar-2.0", "http://www.openejb.org/xml/ns/openejb-jar-2.1");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/pkgen", "http://www.openejb.org/xml/ns/pkgen-2.0");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/corba-css-config_1_0", "http://www.openejb.org/xml/ns/corba-css-config-2.0");
        NAMESPACE_UPDATES.put("http://www.openejb.org/xml/ns/corba-tss-config_1_0", "http://www.openejb.org/xml/ns/corba-tss-config-2.0");
    }

    private static final QName ENVIRONMENT_QNAME = new QName("http://geronimo.apache.org/xml/ns/deployment-1.1", "environment");
    private static final String DEFAULT_GROUPID = "default";
    private static final String DEFAULT_VERSION = "1-default";
    private static final QName CLIENT_ENVIRONMENT_QNAME = new QName("http://geronimo.apache.org/xml/ns/deployment-1.1", "client-environment");
    private static final QName SERVER_ENVIRONMENT_QNAME = new QName("http://geronimo.apache.org/xml/ns/deployment-1.1", "server-environment");

    public static void upgrade(InputStream source, Writer target) throws IOException, XmlException {
        XmlObject xmlObject = parse(source);
        XmlCursor cursor = xmlObject.newCursor();
        XmlCursor.TokenType token;
        while ((token = cursor.toNextToken()) != XmlCursor.TokenType.ENDDOC) {
            if (token == XmlCursor.TokenType.START) {
                Artifact configId = extractArtifact("configId", cursor);
                Artifact parentId = extractArtifact("parentId", cursor);
                Artifact clientConfigId = extractArtifact("clientConfigId", cursor);
                Artifact clientParentId = extractArtifact("clientParentId", cursor);
                boolean suppressDefaultEnvironment = extractSuppressDefaultEnvironment(cursor);
                if (clientConfigId != null) {

                    insertEnvironment(clientConfigId, clientParentId, cursor, CLIENT_ENVIRONMENT_QNAME, suppressDefaultEnvironment);

                    insertEnvironment(configId, parentId, cursor, SERVER_ENVIRONMENT_QNAME, false);

                } else if (configId != null) {

                    insertEnvironment(configId, parentId, cursor, ENVIRONMENT_QNAME, suppressDefaultEnvironment);
                }
            }
        }

        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        xmlObject.save(target, xmlOptions);

    }

    private static void insertEnvironment(Artifact configId, Artifact parentId, XmlCursor cursor, QName environmentQname, boolean suppressDefaultEnvironment) {
        positionEnvironment(cursor);
        Environment environment = new Environment();
        environment.setConfigId(configId);
        if (parentId != null ) {
            environment.addDependency(parentId, ImportType.ALL);
        }
        environment.setSuppressDefaultEnvironment(suppressDefaultEnvironment);
        EnvironmentType environmentType = EnvironmentBuilder.buildEnvironmentType(environment);
        cursor.beginElement(environmentQname);
        XmlCursor element = environmentType.newCursor();
        try {
            element.copyXmlContents(cursor);
        } finally {
            element.dispose();
        }
    }

    private static void positionEnvironment(XmlCursor cursor) {
        XmlCursor.TokenType token;
        while ((token = cursor.toNextToken()) != XmlCursor.TokenType.START && token != XmlCursor.TokenType.END) {
            //keep going
        }
    }

    private static Artifact extractArtifact(String attrName, XmlCursor cursor) {
        String attrValue;
        QName attrQName = new QName(null, attrName);
        if ((attrValue = cursor.getAttributeText(attrQName)) != null) {
            cursor.removeAttribute(attrQName);
            try {
                return Artifact.create(attrValue);
            } catch (Exception e) {
                return new Artifact(DEFAULT_GROUPID, attrValue.replace('/', '_'), DEFAULT_VERSION, "car");
            }
        }
        return null;
    }

    private static boolean extractSuppressDefaultEnvironment(XmlCursor cursor) {
        String attrValue;
        QName attrQName = new QName(null, "suppressDefaultParentId");
        if ((attrValue = cursor.getAttributeText(attrQName)) != null) {
            cursor.removeAttribute(attrQName);
                return true;
        }
        return false;
    }

    public static XmlObject parse(InputStream is) throws IOException, XmlException {
        ArrayList errors = new ArrayList();
        XmlObject parsed = XmlObject.Factory.parse(is, createXmlOptions(errors));
        if (errors.size() != 0) {
            throw new XmlException(errors.toArray().toString());
        }
        return parsed;
    }

    public static XmlOptions createXmlOptions(Collection errors) {
        XmlOptions options = new XmlOptions();
        options.setLoadLineNumbers();
        options.setErrorListener(errors);
        options.setLoadSubstituteNamespaces(NAMESPACE_UPDATES);
        return options;
    }

}
