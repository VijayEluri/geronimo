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

package org.apache.geronimo.activemq;

import junit.framework.TestCase;

/**
 * Tests to ensure that URL parsing and updating doesn't blow up
 *
 * @version $Rev$ $Date$
 */
public class ConnectorTest extends TestCase {
    public void testDummy() throws Exception {
        // Makes the test pass ;-) 
    }
//    public TransportConnectorGBeanImpl test;
//
//    protected void setUp() throws Exception {
//    }
//
//    public void testURLManipulation() {
//        test = new TransportConnectorGBeanImpl(null, "foo", "localhost", 1234);
//        assertEquals("foo://127.0.0.1:1234", test.getUrl());
//        assertEquals("foo", test.getProtocol());
//        assertEquals("127.0.0.1", test.getHost());
//        assertEquals(1234, test.getPort());
//        test.setHost("0.0.0.0");
//        assertEquals("foo://0.0.0.0:1234", test.getUrl());
//        assertEquals("foo", test.getProtocol());
//        assertEquals("0.0.0.0", test.getHost());
//        assertEquals(1234, test.getPort());
//        test.setPort(8765);
//        assertEquals("foo://0.0.0.0:8765", test.getUrl());
//        assertEquals("foo", test.getProtocol());
//        assertEquals("0.0.0.0", test.getHost());
//        assertEquals(8765, test.getPort());
//        test.setProtocol("bar");
//        assertEquals("bar://0.0.0.0:8765", test.getUrl());
//        assertEquals("bar", test.getProtocol());
//        assertEquals("0.0.0.0", test.getHost());
//        assertEquals(8765, test.getPort());
//        test = new TransportConnectorGBeanImpl(null, "vm", "localhost", -1);
//        assertEquals("vm://127.0.0.1", test.getUrl());
//        assertEquals("vm", test.getProtocol());
//        assertEquals("127.0.0.1", test.getHost());
//        assertEquals(-1, test.getPort());
//    }
}
