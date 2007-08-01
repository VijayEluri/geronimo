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
package org.apache.geronimo.tomcat.connector;

import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.management.geronimo.WebManager;
import org.apache.geronimo.system.serverinfo.ServerInfo;
import org.apache.geronimo.tomcat.TomcatContainer;

public abstract class AbstractHttp11ConnectorGBean extends BaseHttp11ConnectorGBean implements Http11Protocol {
    private String keystoreFileName;

    private String truststoreFileName;

    private String algorithm;
    
    public AbstractHttp11ConnectorGBean(String name, Map initParams, String tomcatProtocol, String host, int port, TomcatContainer container, ServerInfo serverInfo) throws Exception {
        super(name, initParams, tomcatProtocol, host, port, container, serverInfo);
    }

    @Override
    public int getDefaultPort() {
        return 80;
    }

    @Override
    public String getGeronimoProtocol() {
        return WebManager.PROTOCOL_HTTP;
    }
    
    // Generic SSL
    public String getAlgorithm() {
        return algorithm;
    }

    public String getCiphers() {
        return (String) connector.getAttribute("ciphers");
    }

    public boolean getClientAuth() {
        Object value = connector.getAttribute("clientAuth");
        return value == null ? false : new Boolean(value.toString()).booleanValue();
    }

    public String getKeyAlias() {
        return (String) connector.getAttribute("keyAlias");
    }

    public String getKeystoreFile() {
        return keystoreFileName;
    }

    public String getKeystoreType() {
        return (String) connector.getAttribute("keystoreType");
    }

    public String getSslProtocol() {
        return (String) connector.getAttribute("sslProtocol");
    }

    public String getTruststoreFile() {
        return truststoreFileName;
    }

    public String getTruststoreType() {
        return (String) connector.getAttribute("truststoreType");
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        if ("default".equalsIgnoreCase(algorithm)) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }
        connector.setAttribute("algorithm", algorithm);
    }

    public void setCiphers(String ciphers) {
        connector.setAttribute("ciphers", ciphers);
    }

    public void setClientAuth(boolean clientAuth) {
        connector.setAttribute("clientAuth", new Boolean(clientAuth));
    }

    public void setKeyAlias(String keyAlias) {
        if (keyAlias.equals(""))
            keyAlias = null;
        connector.setAttribute("keyAlias", keyAlias);
    }

    public void setKeystoreFile(String keystoreFile) {
        if (keystoreFile!= null && keystoreFile.equals("")) 
            keystoreFile = null;
        keystoreFileName = keystoreFile;
        if (keystoreFileName == null)
            connector.setAttribute("keystoreFile", null);
        else
            connector.setAttribute("keystoreFile", serverInfo.resolveServerPath(keystoreFileName));
    }

    public void setKeystorePass(String keystorePass) {
        if (keystorePass!= null && keystorePass.equals("")) 
            keystorePass = null;
        connector.setAttribute("keystorePass", keystorePass);
    }

    public void setKeystoreType(String keystoreType) {
        if (keystoreType!= null && keystoreType.equals("")) 
            keystoreType = null;
        connector.setAttribute("keystoreType", keystoreType);
    }

    public void setSslProtocol(String sslProtocol) {
        if (sslProtocol!= null && sslProtocol.equals("")) 
            sslProtocol = null;
        connector.setAttribute("sslProtocol", sslProtocol);
    }

    public void setTruststoreFile(String truststoreFile) {
        if (truststoreFile!= null && truststoreFile.equals("")) 
            truststoreFile = null;
        truststoreFileName = truststoreFile;
        if (truststoreFileName == null)
            connector.setAttribute("truststoreFile", null);
        else
            connector.setAttribute("truststoreFile", serverInfo.resolveServerPath(truststoreFileName));
    }

    public void setTruststorePass(String truststorePass) {
        if (truststorePass!= null && truststorePass.equals("")) 
            truststorePass = null;
        connector.setAttribute("truststorePass", truststorePass);
    }

    public void setTruststoreType(String truststoreType) {
        if (truststoreType!= null && truststoreType.equals("")) 
            truststoreType = null;
        connector.setAttribute("truststoreType", truststoreType);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("Tomcat Connector", AbstractHttp11ConnectorGBean.class, BaseHttp11ConnectorGBean.GBEAN_INFO);
        infoFactory.addInterface(Http11Protocol.class, 
                new String[] {
                    //SSL Attributes
                    "algorithm",
                    "clientAuth",
                    "keystoreFile",
                    "keystorePass",
                    "keystoreType",
                    "sslProtocol",
                    "ciphers",
                    "keyAlias",
                    "truststoreFile",
                    "truststorePass",
                    "truststoreType"
                },
                new String[] {
                    //SSL Attributes
                    "algorithm",
                    "clientAuth",
                    "keystoreFile",
                    "keystorePass",
                    "keystoreType",
                    "sslProtocol",
                    "ciphers",
                    "keyAlias",
                    "truststoreFile",
                    "truststorePass",
                    "truststoreType"
                }
        );
        infoFactory.setConstructor(new String[] { "name", "initParams", "tomcatProtocol", "host", "port", "TomcatContainer", "ServerInfo"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }
    
    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
    
}
