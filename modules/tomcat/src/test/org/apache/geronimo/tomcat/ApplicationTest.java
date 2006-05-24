/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.apache.geronimo.tomcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * @version $Rev: 387050 $ $Date$
 */
public class ApplicationTest extends AbstractWebModuleTest {

    public void testApplication() throws Exception {
        setUpInsecureAppContext(new File("target/var/catalina/webapps/war1/").toURI(),
                new File("target/var/catalina/webapps/war1/WEB-INF/web.xml").toURL(),
                null,
                null,
                null,
                null);

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8181/test/hello.txt")
                .openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
        assertEquals("Hello World", reader.readLine());
        connection.disconnect();
    }

    protected void setUp() throws Exception {
        super.setUp();
        super.init(null);
    }
}
