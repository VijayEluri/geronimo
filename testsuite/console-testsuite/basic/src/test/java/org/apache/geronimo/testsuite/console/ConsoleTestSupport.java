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

import org.apache.geronimo.testsupport.SeleniumTestSupport;

/**
 * ???
 *
 * @version $Rev$ $Date$
 */
public abstract class ConsoleTestSupport
    extends SeleniumTestSupport
{
    protected void login() throws Exception {
        selenium.open("/");
        selenium.waitForPageToLoad("30000");
        assertEquals("Apache Geronimo", selenium.getTitle());
        
        selenium.click("link=Console");
        selenium.waitForPageToLoad("30000");
        assertEquals("Geronimo Console Login", selenium.getTitle());
        
        selenium.type("j_username", "system");
        selenium.type("j_password", "manager");
        selenium.click("submit");
        selenium.waitForPageToLoad("30000");
        assertEquals("Geronimo Console", selenium.getTitle());
    }
    
    protected void logout() throws Exception {
        selenium.click("//a[contains(@href, '/console/logout.jsp')]");
        selenium.waitForPageToLoad("30000");
        assertEquals("Geronimo Console Login", selenium.getTitle());
        
        selenium.removeCookie("JSESSIONID", "/");
    }
}

