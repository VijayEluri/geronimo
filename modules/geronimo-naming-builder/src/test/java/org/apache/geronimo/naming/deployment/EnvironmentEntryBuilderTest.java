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

package org.apache.geronimo.naming.deployment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.apache.geronimo.naming.enc.EnterpriseNamingContext;
import org.apache.geronimo.j2ee.deployment.NamingBuilder;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;

/**
 * @version $Rev$ $Date$
 */
public class EnvironmentEntryBuilderTest extends TestCase {
    private Map componentContext = new HashMap();
    private NamingBuilder environmentEntryBuilder = new EnvironmentEntryBuilder(new String[] {AbstractNamingBuilder.J2EE_NAMESPACE});

    private static final String TEST = "<tmp xmlns=\"http://java.sun.com/xml/ns/j2ee\">" +
            "<env-entry>" +
            "<env-entry-name>string</env-entry-name>" +
            "<env-entry-type>java.lang.String</env-entry-type>" +
            "<env-entry-value>Hello World</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>char</env-entry-name>" +
            "<env-entry-type>java.lang.Character</env-entry-type>" +
            "<env-entry-value>H</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>byte</env-entry-name>" +
            "<env-entry-type>java.lang.Byte</env-entry-type>" +
            "<env-entry-value>12</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>short</env-entry-name>" +
            "<env-entry-type>java.lang.Short</env-entry-type>" +
            "<env-entry-value>12345</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>int</env-entry-name>" +
            "<env-entry-type>java.lang.Integer</env-entry-type>" +
            "<env-entry-value>12345678</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>long</env-entry-name>" +
            "<env-entry-type>java.lang.Long</env-entry-type>" +
            "<env-entry-value>1234567890123456</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>float</env-entry-name>" +
            "<env-entry-type>java.lang.Float</env-entry-type>" +
            "<env-entry-value>123.456</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>double</env-entry-name>" +
            "<env-entry-type>java.lang.Double</env-entry-type>" +
            "<env-entry-value>12345.6789</env-entry-value>" +
            "</env-entry>" +

            "<env-entry>" +
            "<env-entry-name>boolean</env-entry-name>" +
            "<env-entry-type>java.lang.Boolean</env-entry-type>" +
            "<env-entry-value>TRUE</env-entry-value>" +
            "</env-entry>" +
            "</tmp>";

    public void testEnvEntries() throws Exception {
        String stringVal = "Hello World";
        Character charVal = new Character('H');
        Byte byteVal = new Byte((byte) 12);
        Short shortVal = new Short((short) 12345);
        Integer intVal = new Integer(12345678);
        Long longVal = new Long(1234567890123456L);
        Float floatVal = new Float(123.456);
        Double doubleVal = new Double(12345.6789);
        Boolean booleanVal = Boolean.TRUE;

        XmlObject doc = XmlObject.Factory.parse(TEST);
        XmlCursor cursor = doc.newCursor();
        try {
            cursor.toFirstChild();
            doc = cursor.getObject();
        } finally {
            cursor.dispose();
        }
        environmentEntryBuilder.buildNaming(doc, null, null, null, null, componentContext);
        Context context = EnterpriseNamingContext.createEnterpriseNamingContext(componentContext);
        Set actual = new HashSet();
        for (NamingEnumeration e = context.listBindings("env"); e.hasMore();) {
            NameClassPair pair = (NameClassPair) e.next();
            actual.add(pair.getName());
        }
        Set expected = new HashSet(Arrays.asList(new String[]{"string", "char", "byte", "short", "int", "long", "float", "double", "boolean"}));
        assertEquals(expected, actual);
        assertEquals(stringVal, context.lookup("env/string"));
        assertEquals(charVal, context.lookup("env/char"));
        assertEquals(byteVal, context.lookup("env/byte"));
        assertEquals(shortVal, context.lookup("env/short"));
        assertEquals(intVal, context.lookup("env/int"));
        assertEquals(longVal, context.lookup("env/long"));
        assertEquals(floatVal, context.lookup("env/float"));
        assertEquals(doubleVal, context.lookup("env/double"));
        assertEquals(booleanVal, context.lookup("env/boolean"));
    }

    public void testEmptyEnvironment() throws NamingException {
        Context context = EnterpriseNamingContext.createEnterpriseNamingContext(componentContext);
        try {
            Context env = (Context) context.lookup("env");
            assertNotNull(env);
        } catch (NamingException e) {
            fail();
        }
    }

}
