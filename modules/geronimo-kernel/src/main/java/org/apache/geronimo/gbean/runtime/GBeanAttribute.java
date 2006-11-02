/**
 *
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

package org.apache.geronimo.gbean.runtime;

import java.lang.reflect.Method;

import org.apache.geronimo.gbean.DynamicGAttributeInfo;
import org.apache.geronimo.gbean.DynamicGBean;
import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.apache.geronimo.kernel.ClassLoading;

/**
 * @version $Rev$ $Date$
 */
public class GBeanAttribute {
    private final GBeanInstance gbeanInstance;

    private final String name;

    private final Class type;

    private final boolean readable;

    private final MethodInvoker getInvoker;

    private final boolean writable;

    private final MethodInvoker setInvoker;

    private final boolean isConstructorArg;

    private final boolean persistent;

    private final boolean manageable;

    private Object persistentValue;

    /**
     * Is this a special attribute like objectName, classLoader or gbeanContext?
     * Special attributes are injected at startup just like persistent attrubutes, but are
     * otherwise unmodifiable.
     */
    private final boolean special;

    private final boolean framework;

    private final boolean dynamic;

    private final GAttributeInfo attributeInfo;

    static GBeanAttribute createSpecialAttribute(GBeanAttribute attribute, GBeanInstance gbeanInstance, String name, Class type, Object value) {
        return new GBeanAttribute(attribute, gbeanInstance, name, type, value);
    }

    private GBeanAttribute(GBeanAttribute attribute, GBeanInstance gbeanInstance, String name, Class type, Object value) {
        this.special = true;
        this.framework = false;
        this.dynamic = false;

        if (gbeanInstance == null || name == null || type == null) {
            throw new IllegalArgumentException("null param(s) supplied");
        }

        // if we have an attribute verify the gbean instance, name and types match
        if (attribute != null) {
            assert (gbeanInstance == attribute.gbeanInstance);
            assert (name.equals(attribute.name));
            if (type != attribute.type) {
                throw new InvalidConfigurationException("Special attribute " + name +
                        " must have the type " + type.getName() + ", but is " +
                        attribute.type.getName() + ": targetClass=" + gbeanInstance.getType().getName());
            }
            if (attribute.isPersistent()) {
                throw new InvalidConfigurationException("Special attributes must not be persistent:" +
                        " name=" + name + ", targetClass=" + gbeanInstance.getType().getName());
            }
        }

        this.gbeanInstance = gbeanInstance;
        this.name = name;
        this.type = type;

        // getter
        this.getInvoker = null;
        this.readable = true;

        // setter
        if (attribute != null) {
            this.setInvoker = attribute.setInvoker;
            this.isConstructorArg = attribute.isConstructorArg;
        } else {
            this.setInvoker = null;
            this.isConstructorArg = false;
        }
        this.writable = false;

        // persistence
        this.persistent = false;
        initializePersistentValue(value);

        // not manageable
        this.manageable = false;

        // create an attribute info for this gbean
        if (attribute != null) {
            GAttributeInfo attributeInfo = attribute.getAttributeInfo();
            this.attributeInfo = new GAttributeInfo(this.name,
                    this.type.getName(),
                    this.persistent,
                    this.manageable,
                    this.readable,
                    this.writable,
                    attributeInfo.getGetterName(),
                    attributeInfo.getSetterName());
        } else {
            this.attributeInfo = new GAttributeInfo(this.name,
                    this.type.getName(),
                    this.persistent,
                    this.manageable,
                    this.readable,
                    this.writable,
                    null,
                    null);
        }
    }

    static GBeanAttribute createFrameworkAttribute(GBeanInstance gbeanInstance, String name, Class type, MethodInvoker getInvoker) {
        return new GBeanAttribute(gbeanInstance, name, type, getInvoker, null, false, null, true);
    }

    static GBeanAttribute createFrameworkAttribute(GBeanInstance gbeanInstance, String name, Class type, MethodInvoker getInvoker, MethodInvoker setInvoker, boolean persistent, Object persistentValue, boolean manageable) {
        return new GBeanAttribute(gbeanInstance, name, type, getInvoker, setInvoker, persistent, persistentValue, manageable);
    }

    private GBeanAttribute(GBeanInstance gbeanInstance, String name, Class type, MethodInvoker getInvoker, MethodInvoker setInvoker, boolean persistent, Object persistentValue, boolean manageable) {
        this.special = false;
        this.framework = true;
        this.dynamic = false;

        if (gbeanInstance == null || name == null || type == null) {
            throw new IllegalArgumentException("null param(s) supplied");
        }

        this.gbeanInstance = gbeanInstance;
        this.name = name;
        this.type = type;

        // getter
        this.getInvoker = getInvoker;
        this.readable = (this.getInvoker != null);

        // setter
        this.setInvoker = setInvoker;
        this.isConstructorArg = false;
        this.writable = (this.setInvoker != null);

        // persistence
        this.persistent = persistent;
        initializePersistentValue(persistentValue);

        // manageable
        this.manageable = manageable;

        // create an attribute info for this gbean
        attributeInfo = new GAttributeInfo(this.name,
                this.type.getName(),
                this.persistent,
                this.manageable,
                this.readable,
                this.writable,
                null,
                null);
    }

    public GBeanAttribute(GBeanInstance gbeanInstance, GAttributeInfo attributeInfo, boolean isConstructorArg) throws InvalidConfigurationException {
        this.special = false;
        this.framework = false;

        if (gbeanInstance == null || attributeInfo == null) {
            throw new IllegalArgumentException("null param(s) supplied");
        }
        if (!attributeInfo.isReadable() && !attributeInfo.isWritable() && !attributeInfo.isPersistent() && !isConstructorArg)
        {
            throw new InvalidConfigurationException("An attribute must be readable, writable, persistent or a constructor arg: " +
                    " name=" + attributeInfo.getName() + " targetClass=" + gbeanInstance.getType().getName());
        }
        this.gbeanInstance = gbeanInstance;
        this.attributeInfo = attributeInfo;
        this.name = attributeInfo.getName();
        this.isConstructorArg = isConstructorArg;
        try {
            this.type = ClassLoading.loadClass(attributeInfo.getType(), gbeanInstance.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new InvalidConfigurationException("Could not load attribute class: " + attributeInfo.getType());
        }
        this.persistent = attributeInfo.isPersistent();
        this.manageable = attributeInfo.isManageable();

        readable = attributeInfo.isReadable();
        writable = attributeInfo.isWritable();

        // If attribute is persistent or not tagged as unreadable, search for a
        // getter method
        if (attributeInfo instanceof DynamicGAttributeInfo) {
            this.dynamic = true;
            if (readable) {
                getInvoker = new DynamicGetterMethodInvoker(name);
            } else {
                getInvoker = null;
            }
            if (writable) {
                setInvoker = new DynamicSetterMethodInvoker(name);
            } else {
                setInvoker = null;
            }
        } else {
            this.dynamic = false;
            if (attributeInfo.getGetterName() != null) {
                try {
                    String getterName = attributeInfo.getGetterName();
                    Method getterMethod = gbeanInstance.getType().getMethod(getterName, null);

                    if (!getterMethod.getReturnType().equals(type)) {
                        if (getterMethod.getReturnType().getName().equals(type.getName())) {
                            throw new InvalidConfigurationException("Getter return type in wrong classloader: type: " + type + " wanted in classloader: " + type.getClassLoader() + " actual: " + getterMethod.getReturnType().getClassLoader());
                        } else {
                            throw new InvalidConfigurationException("Getter method of wrong type: " + getterMethod.getReturnType() + " expected " + getDescription());
                        }
                    }
                    if (AbstractGBeanReference.NO_PROXY) {
                        getInvoker = new ReflectionMethodInvoker(getterMethod);
                    } else {
                        getInvoker = new FastMethodInvoker(getterMethod);
                    }
                } catch (NoSuchMethodException e) {
                    throw new InvalidConfigurationException("Getter method not found " + getDescription());
                }
            } else {
                getInvoker = null;
            }

            // If attribute is persistent or not tagged as unwritable, search
            // for a setter method
            if (attributeInfo.getSetterName() != null) {
                try {
                    String setterName = attributeInfo.getSetterName();
                    Method setterMethod = gbeanInstance.getType().getMethod(setterName, new Class[]{type});
                    if (AbstractGBeanReference.NO_PROXY) {
                        setInvoker = new ReflectionMethodInvoker(setterMethod);
                    } else {
                        setInvoker = new FastMethodInvoker(setterMethod);
                    }
                } catch (NoSuchMethodException e) {
                    throw new InvalidConfigurationException("Setter method not found " + getDescription());
                }
            } else {
                setInvoker = null;
            }
        }

        initializePersistentValue(null);
    }

    private void initializePersistentValue(Object value) {
        if (persistent || special) {
            if (value == null && type.isPrimitive() && isConstructorArg) {
                if (type == Boolean.TYPE) {
                    value = Boolean.FALSE;
                } else if (type == Byte.TYPE) {
                    value = new Byte((byte) 0);
                } else if (type == Short.TYPE) {
                    value = new Short((short) 0);
                } else if (type == Integer.TYPE) {
                    value = new Integer(0);
                } else if (type == Long.TYPE) {
                    value = new Long(0);
                } else if (type == Character.TYPE) {
                    value = new Character((char) 0);
                } else if (type == Float.TYPE) {
                    value = new Float(0);
                } else /** if (type == Double.TYPE) */ {
                    value = new Double(0);
                }
            }
            persistentValue = value;
        }
    }

    public String getName() {
        return name;
    }

    public GAttributeInfo getAttributeInfo() {
        return attributeInfo;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public Class getType() {
        return type;
    }

    public boolean isFramework() {
        return framework;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isManageable() {
        return manageable;
    }

    public boolean isSpecial() {
        return special;
    }

    public void inject(Object target) throws Exception {
        if ((persistent || special) && !isConstructorArg && writable && persistentValue != null) {
            setValue(target, persistentValue);
        }
    }

    public Object getPersistentValue() {
        if (!persistent && !special) {
            throw new IllegalStateException("Attribute is not persistent " + getDescription());
        }
        return persistentValue;
    }

    public void setPersistentValue(Object persistentValue) {
        if (!persistent && !special) {
            throw new IllegalStateException("Attribute is not persistent " + getDescription());
        }

        if (persistentValue == null && type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot assign null to a primitive attribute. " + getDescription());
        }

        // @todo actually check type
        this.persistentValue = persistentValue;
    }

    public Object getValue(Object target) throws Exception {
        if (!readable) {
            if (persistent) {
                return persistentValue;
            } else {
                throw new IllegalStateException("This attribute is not readable. " + getDescription());
            }
        }

        if (special) {
            return persistentValue;
        }

        // get the target to invoke
        if (target == null && !framework) {
            throw new IllegalStateException("GBean does not have a target instance to invoke. " + getDescription());
        }

        // call the getter
        Object value = getInvoker.invoke(target, null);
        return value;
    }

    public void setValue(Object target, Object value) throws Exception {
        if (!writable) {
            if (persistent) {
                throw new IllegalStateException("This persistent attribute is not modifable while the gbean is running. " + getDescription());
            } else {
                throw new IllegalStateException("This attribute is not writable. " + getDescription());
            }
        }

        // the value can not be null for primitives
        if (value == null && type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot assign null to a primitive attribute. " + getDescription());
        }

        // @todo actually check type

        // get the target to invoke
        if (target == null && !framework) {
            throw new IllegalStateException("GBean does not have a target instance to invoke. " + getDescription());
        }

        // call the setter
        setInvoker.invoke(target, new Object[]{value});
    }

    public String getDescription() {
        return "Attribute Name: " + getName() + ", Type: " + getType() + ", GBeanInstance: " + gbeanInstance.getName();
    }

    private static final class DynamicGetterMethodInvoker implements MethodInvoker {
        private final String name;

        public DynamicGetterMethodInvoker(String name) {
            this.name = name;
        }

        public Object invoke(Object target, Object[] arguments) throws Exception {
            return ((DynamicGBean) target).getAttribute(name);
        }
    }

    private static final class DynamicSetterMethodInvoker implements MethodInvoker {
        private final String name;

        public DynamicSetterMethodInvoker(String name) {
            this.name = name;
        }

        public Object invoke(Object target, Object[] arguments) throws Exception {
            ((DynamicGBean) target).setAttribute(name, arguments[0]);
            return null;
        }
    }
}
