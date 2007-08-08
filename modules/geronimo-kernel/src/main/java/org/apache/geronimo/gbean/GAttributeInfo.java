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

package org.apache.geronimo.gbean;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.geronimo.kernel.KernelRegistry;

/**
 * Describes an attibute of a GBean.
 *
 * @version $Rev$ $Date$
 */
public class GAttributeInfo implements Serializable {
    /**
     * Name of this attribute.
     */
    private final String name;

    /**
     * Type of this attribute.
     */
    private final String type;

    /**
     * Is this attribute persistent?
     */
    private final boolean persistent;

    /**
     * Is this attribute manageable?
     */
    private final boolean manageable;

    /**
     * Is this attribute readable?
     */
    private final boolean readable;

    /**
     * Is this attribute writiable?
     */
    private final boolean writable;

    /**
     * Name of the getter method.
     * The default is "get" + name.  In the case of a defualt value we do a caseless search for the name.
     */
    private final String getterName;

    /**
     * Name of the setter method.
     * The default is "set" + name.  In the case of a defualt value we do a caseless search for the name.
     */
    private final String setterName;

    public GAttributeInfo(String name, String type, boolean persistent, boolean manageable, String getterName, String setterName) {
        this(name, type, persistent, manageable, getterName != null, setterName != null, getterName, setterName);
    }

    public GAttributeInfo(String name, String type, boolean persistent, boolean manageable, boolean readable, boolean writable, String getterName, String setterName) {
        this.name = name;
        this.type = type;
        this.persistent = persistent;
        //non persistent attributes cannot be manageable
        this.manageable = manageable & persistent;
        this.readable = readable;
        this.writable = writable;
        this.getterName = getterName;
        this.setterName = setterName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isManageable() {
        return manageable;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public String getGetterName() {
        return getterName;
    }

    public String getSetterName() {
        return setterName;
    }

    public String toString() {
        return "[GAttributeInfo: name=" + name +
                 " type=" + type +
                 " persistent=" + persistent +
                 " manageable=" + manageable +
                 " readable=" + readable +
                 " writable=" + writable +
                 " getterName=" + getterName +
                 " setterName=" + setterName +
                 "]";
    }

    public String toXML(AbstractName abstractName) {
        String xml = "";
        
        xml += "<gAttributeInfo ";
        xml += "name='" + name + "' ";
        xml += "type='" + type + "' ";
        xml += "persistent='" + persistent + "' ";
        xml += "manageable='" + manageable + "' ";
        xml += "readable='" + readable + "' ";
        xml += "writable='" + writable + "' ";
        xml += ">";
        
        xml += "<getterName>" + getterName + "</getterName>";
        xml += "<setterName>" + setterName + "</setterName>";

        if (readable) {
            try {
                Object value = KernelRegistry.getSingleKernel().getAttribute(abstractName, name);
                if (value != null) {
                    if (value instanceof String[]) {
                        for (String valueString : Arrays.asList((String[]) value))
                            xml += "<value>" + valueString + "</value>";            
                    } else {
                        xml += "<value>" + value + "</value>";
                    }
                }
            } catch (Exception e) {
                
            }
        }
        
        xml += "</gAttributeInfo>";

        return xml;
    }
}
