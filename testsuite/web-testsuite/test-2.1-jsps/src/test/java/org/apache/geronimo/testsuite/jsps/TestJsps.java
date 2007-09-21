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

package org.apache.geronimo.testsuite.jsps;


import org.apache.geronimo.testsupport.SeleniumTestSupport;
import org.testng.annotations.Test;

public class TestJsps extends SeleniumTestSupport
{

    @Test
    public void testDeferral() throws Exception {
        selenium.open("/jsp21/testDeferral.jsp");
        selenium.waitForPageToLoad("30000");
        
        //throw new Exception(selenium.getText("xpath=/html/body"));
        assertEquals("OneTwo", selenium.getText("xpath=/html/body"));
    }

    @Test
    public void testScopes() throws Exception {
        selenium.open("/jsp21/testScopes.jsp");
        selenium.waitForPageToLoad("30000");
        assertEquals("value1 value2 value3 value4", selenium.getText("xpath=/html/body"));
    }

    @Test
    public void testTaglibs() throws Exception {
        selenium.open("/jsp21/testTaglibs.jsp");
        selenium.waitForPageToLoad("30000");
        assertEquals("Hello", selenium.getText("xpath=/html/body"));
    }

    @Test
    public void testTrimWhitespace() throws Exception {
        selenium.open("/jsp21/testTrimWhitespace.jsp");
        selenium.waitForPageToLoad("30000");
        assertEquals("source html of this page should not contain empty lines", selenium.getText("xpath=/html/body"));
    }

}
