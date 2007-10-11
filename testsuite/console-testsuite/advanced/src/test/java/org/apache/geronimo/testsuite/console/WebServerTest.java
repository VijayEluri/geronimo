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

package org.apache.geronimo.testsuite.console;

import org.testng.annotations.Test;
import org.apache.geronimo.testsupport.console.ConsoleTestSupport;

@Test
public class WebServerTest extends ConsoleTestSupport {

    String TOMCAT = "Tomcat";
    String JETTY = "Jetty";

    @Test
    public void testNewConnector() throws Exception {
        try {
            login();

            String name = "uniquename";
            addConnector(name, 8081);
            
            deleteConnector(name);
        } finally {
            logout();
        }
    }

    @Test
    public void testEditConnector() throws Exception{
        try {
            login();
            
            String name = "uniquename2";
            addConnector(name, 8082);

            String connectorSelector = "//tr[td[1] = \"" + name + "\"]";
            
            selenium.click(connectorSelector + "/td[5]/a[2]");        
            selenium.waitForPageToLoad("30000");

            selenium.type("port", "8008");
            selenium.click("submit");
            selenium.waitForPageToLoad("30000");
            assertTrue(selenium.isTextPresent("8008"));
            
            selenium.click(connectorSelector + "/td[5]/a[2]"); 

            selenium.waitForPageToLoad("30000");
            selenium.type("port", "8009");
            selenium.click("submit");
            selenium.waitForPageToLoad("30000");
            assertTrue(selenium.isTextPresent("8009"));

            deleteConnector(name);
        } finally {
            logout();
        }
    }
    
    @Test
    public void testStartStopConnector() throws Exception {
        try {
            login();
        
            String name = "uniquename3";
            addConnector(name, 8083);

            String connectorSelector = "//tr[td[1] = \"" + name + "\"]";

            assertEquals("running", selenium.getText(connectorSelector + "/td[4]"));
            selenium.click(connectorSelector + "/td[5]/a[1]");
            selenium.waitForPageToLoad("30000");
            assertEquals("stopped", selenium.getText(connectorSelector + "/td[4]"));
            selenium.click(connectorSelector + "/td[5]/a[1]");
            selenium.waitForPageToLoad("30000");
            assertEquals("running", selenium.getText(connectorSelector + "/td[4]"));

            deleteConnector(name);
        } finally {
            logout();
        }
    }

    private void addConnector(String name, int port) throws Exception {
        selenium.click("link=Web Server");
        selenium.waitForPageToLoad("30000");
        String container = JETTY;
        if(selenium.isTextPresent(TOMCAT)) {
            container = TOMCAT;
        }
        
        selenium.click("link=" + container + " BIO HTTP Connector");
        selenium.waitForPageToLoad("30000");
        selenium.type("uniqueName", name);
        selenium.type("port", String.valueOf(port));
        selenium.click("submit");
        selenium.waitForPageToLoad("30000");
        assertTrue(selenium.isTextPresent(name));
    }

    private void deleteConnector(String name) throws Exception {
        selenium.click("//a[@onclick=\"return confirm('Are you sure you want to delete " + name + "?');\"]");
        selenium.waitForPageToLoad("30000");
        assertTrue(selenium.getConfirmation().matches("^Are you sure you want to delete " + name + "[\\s\\S]$"));
    }

}

