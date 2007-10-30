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
package org.apache.geronimo.jetty6.deployment;

import junit.framework.TestCase;
import org.apache.geronimo.xbeans.javaee.ServletType;

/**
 * @version $Rev$ $Date$
 */
public class StartupOrderComparatorTest extends TestCase {

    private final JettyModuleBuilder.StartupOrderComparator c = new JettyModuleBuilder.StartupOrderComparator();

    public void testNoOrders() throws Exception {
        ServletType s1 = makeServletType("a", -1);
        ServletType s2 = makeServletType("b", -1);
        ServletType s3 = makeServletType("c", -1);
        checkOrdering(s1, s2, s3);
    }

    public void testIdenticalOrders() throws Exception {
        ServletType s1 = makeServletType("a", 1);
        ServletType s2 = makeServletType("b", 1);
        ServletType s3 = makeServletType("c", 1);
        checkOrdering(s1, s2, s3);
    }

    public void testDistinctOrders() throws Exception {
        ServletType s1 = makeServletType("c", 1);
        ServletType s2 = makeServletType("b", 2);
        ServletType s3 = makeServletType("a", 3);
        checkOrdering(s1, s2, s3);
    }

    public void testMixedOrders1() throws Exception {
        ServletType s1 = makeServletType("c", 1);
        ServletType s2 = makeServletType("b", 2);
        ServletType s3 = makeServletType("a", -1);
        checkOrdering(s1, s2, s3);
    }

    public void testMixedOrders2() throws Exception {
        ServletType s1 = makeServletType("c", 1);
        ServletType s2 = makeServletType("a", -1);
        ServletType s3 = makeServletType("b", -1);
        checkOrdering(s1, s2, s3);
    }

    private void checkOrdering(ServletType s1, ServletType s2, ServletType s3) {
        //symmetric
        assertTrue(c.compare(s1, s2) < 0);
        assertTrue(c.compare(s2, s1) > 0);
        //reflexive
        assertTrue(c.compare(s1, s1) == 0);
        //transitive
        assertTrue(c.compare(s2, s3) < 0);
        assertTrue(c.compare(s1, s3) < 0);
    }

    private ServletType makeServletType(String servletName, int order) {
        ServletType s1 = ServletType.Factory.newInstance();
        s1.addNewServletName().setStringValue(servletName);
        if (order > -1) {
            s1.setLoadOnStartup(Integer.valueOf(order));
        }
        return s1;
    }
}
