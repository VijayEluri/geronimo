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

package org.apache.geronimo.naming.java;

import java.net.MalformedURLException;
import java.net.URL;
import javax.management.MalformedObjectNameException;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.apache.geronimo.naming.ReferenceFactory;
import org.apache.geronimo.xbeans.geronimo.naming.GerLocalRefType;
import org.apache.geronimo.xbeans.geronimo.naming.GerRemoteRefType;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class ComponentContextBuilder {
    private static final String ENV = "env/";
    private final ReferenceFactory referenceFactory;
    private final ReadOnlyContext context;

    public ComponentContextBuilder(ReferenceFactory referenceFactory) {
        this.referenceFactory = referenceFactory;
        this.context = new ReadOnlyContext();
        try {
            context.internalBind("env", new ReadOnlyContext());
        } catch (NamingException e) {
            throw new AssertionError();
        }
    }

    public ReadOnlyContext getContext() {
        context.freeze();
        return context;
    }

    public void addUserTransaction(UserTransaction userTransaction) throws NamingException {
        if (context.isFrozen()) {
            throw new IllegalStateException("Context has been frozen");
        }
        context.internalBind("UserTransaction", userTransaction);
    }


    public void bind(String name, Object value) throws NamingException {
        if (context.isFrozen()) {
            throw new IllegalStateException("Context has been frozen");
        }
        context.internalBind(ENV + name, value);
    }


    public void addEnvEntry(String name, String type, String text) throws NamingException, NumberFormatException {
        if (context.isFrozen()) {
            throw new IllegalStateException("Context has been frozen");
        }

        Object value;
        if (text == null) {
            value = null;
        } else if ("java.lang.String".equals(type)) {
            value = text;
        } else if ("java.lang.Character".equals(type)) {
            value = new Character(text.charAt(0));
        } else if ("java.lang.Boolean".equals(type)) {
            value = Boolean.valueOf(text);
        } else if ("java.lang.Byte".equals(type)) {
            value = Byte.valueOf(text);
        } else if ("java.lang.Short".equals(type)) {
            value = Short.valueOf(text);
        } else if ("java.lang.Integer".equals(type)) {
            value = Integer.valueOf(text);
        } else if ("java.lang.Long".equals(type)) {
            value = Long.valueOf(text);
        } else if ("java.lang.Float".equals(type)) {
            value = Float.valueOf(text);
        } else if ("java.lang.Double".equals(type)) {
            value = Double.valueOf(text);
        } else {
            throw new IllegalArgumentException("Invalid class for env-entry " + name + ", " + type);
        }
        context.internalBind(ENV + name, value);
    }

    public void addResourceRef(String name, Class iface, GerLocalRefType localRef) throws NamingException {
        if (localRef.isSetExternalUri()) {
            try {
                context.internalBind(ENV + name, new URL(localRef.getExternalUri()));
            } catch (MalformedURLException e) {
                throw (NamingException) new NamingException("Could not convert " + localRef + " to URL").initCause(e);
            }
        } else if (localRef.isSetResourceLink()) {
            try {
                bind(name, referenceFactory.buildResourceLinkReference(localRef, iface));
            } catch (MalformedObjectNameException e) {
                throw (NamingException) new NamingException("invalid object name").initCause(e);
            }
        } else if (localRef.isSetTargetName()) {
            try {
                bind(name, referenceFactory.buildConnectionFactoryReference(localRef, iface));
            } catch (MalformedObjectNameException e) {
                throw (NamingException) new NamingException("invalid object name").initCause(e);
            }
        }
    }

    public void addResourceEnvRef(String name, Class iface, GerLocalRefType localRef) throws NamingException {
        try {
            bind(name, referenceFactory.buildAdminObjectReference(localRef, iface));
        } catch (MalformedObjectNameException e) {
            throw (NamingException) new NamingException("invalid object name").initCause(e);
        }
    }

    //TODO this works only if there is only one kernel running.
    public void addMessageDestinationRef(String name, String linkName, Class iface) throws NamingException {
        try {
            bind(name, referenceFactory.buildMessageDestinationReference(linkName, iface));
        } catch (MalformedObjectNameException e) {
            throw (NamingException) new NamingException("invalid object name").initCause(e);
        }
    }

    public void addEjbRef(String name, Class iface, GerRemoteRefType remoteRef) throws NamingException {
        try {
            bind(name, referenceFactory.buildEjbReference(remoteRef, iface));
        } catch (MalformedObjectNameException e) {
            throw (NamingException) new NamingException("invalid object name").initCause(e);
        }
    }

    public void addEjbLocalRef(String name, Class iface, GerLocalRefType localRef) throws NamingException {
        try {
            bind(name, referenceFactory.buildEjbLocalReference(localRef, iface));
        } catch (MalformedObjectNameException e) {
            throw (NamingException) new NamingException("invalid object name").initCause(e);
        }
    }

}
