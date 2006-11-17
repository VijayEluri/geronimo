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

package org.apache.geronimo.security.util;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.common.GeronimoSecurityException;
import org.apache.geronimo.security.DomainPrincipal;
import org.apache.geronimo.security.PrimaryDomainPrincipal;
import org.apache.geronimo.security.PrimaryPrincipal;
import org.apache.geronimo.security.PrimaryRealmPrincipal;
import org.apache.geronimo.security.RealmPrincipal;
import org.apache.geronimo.security.deploy.DefaultDomainPrincipal;
import org.apache.geronimo.security.deploy.DefaultPrincipal;
import org.apache.geronimo.security.deploy.DefaultRealmPrincipal;
import org.apache.geronimo.security.deploy.PrincipalInfo;


/**
 * A collection of utility functions that assist with the configuration of
 * <code>PolicyConfiguration</code>s.
 *
 * @version $Rev$ $Date$
 * @see javax.security.jacc.PolicyConfiguration
 * @see "JSR 115" Java Authorization Contract for Containers
 */
public class ConfigurationUtil {

    /**
     * Create an X500Principal from a deployment description.
     *
     * @param name the distinguished name of the principal
     * @return an X500Principal from a deployment description
     */
    public static X500Principal generateX500Principal(String name) {
        return new X500Principal(name);
    }

    /**
     * Create a Principal from a deployment description.
     *
     * @param principalInfo the deployment description of the principal to be created.
     * @param classLoader
     * @return a RealmPrincipal from a deployment description
     */
    public static java.security.Principal generatePrincipal(final PrincipalInfo principalInfo, ClassLoader classLoader) {
        return generatePrincipal(principalInfo.getClassName(), principalInfo.getPrincipalName(), classLoader);
    }

    public static java.security.Principal generatePrincipal(final String className, final String principalName, final ClassLoader classLoader) {
        try {
            return (java.security.Principal) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    Class clazz = classLoader.loadClass(className);
                    Constructor constructor = clazz.getDeclaredConstructor(new Class[]{String.class});
                    return (java.security.Principal) constructor.newInstance(new Object[]{principalName});
                }
            });
        } catch (PrivilegedActionException e) {
            e.printStackTrace();
            if (e.getException() != null) {
                e.getException().printStackTrace();
            }
            return null;
        }
    }

    /**
     * Create a RealmPrincipal from a deployment description.
     *
     * @param principalInfo the deployment description of the principal to be created.
     * @param classLoader
     * @return a RealmPrincipal from a deployment description
     */
    public static RealmPrincipal generateRealmPrincipal(final String realm, final String loginDomain, final PrincipalInfo principalInfo, ClassLoader classLoader) {
        return generateRealmPrincipal(realm, loginDomain, principalInfo.getClassName(), principalInfo.getPrincipalName(), classLoader);
    }

    public static RealmPrincipal generateRealmPrincipal(final String realm, final String loginDomain, final String className, final String principalName,
                                                        ClassLoader classLoader)
    {
        return new RealmPrincipal(realm, loginDomain, generatePrincipal(className, principalName, classLoader));
    }

    /**
     * Create a DomainPrincipal from a deployment description.
     *
     * @param principalInfo the deployment description of the principal to be created.
     * @param classLoader
     * @return a RealmPrincipal from a deployment description
     */
    public static DomainPrincipal generateDomainPrincipal(final String loginDomain, final PrincipalInfo principalInfo, ClassLoader classLoader) {
        return generateDomainPrincipal(loginDomain, principalInfo.getClassName(), principalInfo.getPrincipalName(), classLoader);
    }

    public static DomainPrincipal generateDomainPrincipal(final String loginDomain, final String className, final String principalName, ClassLoader classLoader) {
        return new DomainPrincipal(loginDomain, generatePrincipal(className, principalName, classLoader));
    }

    /**
     * Create a RealmPrincipal from a deployment description.
     *
     * @param principalInfo the deployment description of the principal to be created.
     * @param classLoader
     * @return a PrimaryRealmPrincipal from a deployment description
     */
    public static PrimaryRealmPrincipal generatePrimaryRealmPrincipal(final String realm, final String domain, final PrincipalInfo principalInfo, ClassLoader classLoader) throws DeploymentException {
        return generatePrimaryRealmPrincipal(realm, domain, principalInfo.getClassName(), principalInfo.getPrincipalName(), classLoader);
    }

    public static PrimaryRealmPrincipal generatePrimaryRealmPrincipal(final String realm, final String domain, final String className, final String principalName,
                                                                      final ClassLoader classLoader) throws DeploymentException
    {
        try {
            return (PrimaryRealmPrincipal) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    java.security.Principal p = null;
                    Class clazz = classLoader.loadClass(className);
                    Constructor constructor = clazz.getDeclaredConstructor(new Class[]{String.class});
                    p = (java.security.Principal) constructor.newInstance(new Object[]{principalName});

                    return new PrimaryRealmPrincipal(realm, domain, p);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new DeploymentException("Unable to create realm principal", pae.getException());
        }
    }

    /**
     * Create a DomainPrincipal from a deployment description.
     *
     * @param principalInfo the deployment description of the principal to be created.
     * @param classLoader
     * @return a PrimaryDomainPrincipal from a deployment description
     */
    public static PrimaryDomainPrincipal generatePrimaryDomainPrincipal(final String domain, final PrincipalInfo principalInfo, ClassLoader classLoader) throws DeploymentException {
        return generatePrimaryDomainPrincipal(domain, principalInfo.getClassName(), principalInfo.getPrincipalName(), classLoader);
    }

    public static PrimaryDomainPrincipal generatePrimaryDomainPrincipal(final String domain, final String className, final String principalName,
                                                                        final ClassLoader classLoader) throws DeploymentException
    {
        try {
            return (PrimaryDomainPrincipal) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    java.security.Principal p = null;
                    Class clazz = classLoader.loadClass(className);
                    Constructor constructor = clazz.getDeclaredConstructor(new Class[]{String.class});
                    p = (java.security.Principal) constructor.newInstance(new Object[]{principalName});

                    return new PrimaryDomainPrincipal(domain, p);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new DeploymentException("Unable to create domain principal", pae.getException());
        }
    }

    /**
     * Create a Principal from a deployment description.
     *
     * @param principalInfo the deployment description of the principal to be created.
     * @param classLoader
     * @return a Principal from a deployment description
     */
    public static PrimaryPrincipal generatePrimaryPrincipal(final PrincipalInfo principalInfo, ClassLoader classLoader) throws DeploymentException {
        return generatePrimaryPrincipal(principalInfo.getClassName(), principalInfo.getPrincipalName(), classLoader);
    }

    public static PrimaryPrincipal generatePrimaryPrincipal(final String className, final String principalName, final ClassLoader classLoader) throws DeploymentException {
        try {
            return (PrimaryPrincipal) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    java.security.Principal p = null;
                    Class clazz = classLoader.loadClass(className);
                    Constructor constructor = clazz.getDeclaredConstructor(new Class[]{String.class});
                    p = (java.security.Principal) constructor.newInstance(new Object[]{principalName});

                    return new PrimaryPrincipal(p);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new DeploymentException("Unable to create principal", pae.getException());
        }
    }

    /**
     * Generate the default principal from the security config.
     *
     * @param defaultPrincipal
     * @param classLoader
     * @return the default principal
     */
    public static Subject generateDefaultSubject(DefaultPrincipal defaultPrincipal, ClassLoader classLoader) throws DeploymentException {
        if (defaultPrincipal == null) {
            throw new GeronimoSecurityException("No DefaultPrincipal configuration supplied");
        }
        Subject defaultSubject = new Subject();
        java.security.Principal principal;
        java.security.Principal primaryPrincipal;

        if (defaultPrincipal instanceof DefaultRealmPrincipal) {
            DefaultRealmPrincipal defaultRealmPrincipal = (DefaultRealmPrincipal) defaultPrincipal;
            principal = generateRealmPrincipal(defaultRealmPrincipal.getRealm(), defaultRealmPrincipal.getDomain(), defaultRealmPrincipal.getPrincipal(), classLoader);
            primaryPrincipal = generatePrimaryRealmPrincipal(defaultRealmPrincipal.getRealm(), defaultRealmPrincipal.getDomain(), defaultRealmPrincipal.getPrincipal(), classLoader);
        } else if (defaultPrincipal instanceof DefaultDomainPrincipal) {
            DefaultDomainPrincipal defaultDomainPrincipal = (DefaultDomainPrincipal) defaultPrincipal;
            principal = generateDomainPrincipal(defaultDomainPrincipal.getDomain(), defaultDomainPrincipal.getPrincipal(), classLoader);
            primaryPrincipal = generatePrimaryDomainPrincipal(defaultDomainPrincipal.getDomain(), defaultDomainPrincipal.getPrincipal(), classLoader);
        } else {
            principal = generatePrincipal(defaultPrincipal.getPrincipal(), classLoader);
            primaryPrincipal = generatePrimaryPrincipal(defaultPrincipal.getPrincipal(), classLoader);

        }
        defaultSubject.getPrincipals().add(principal);
        defaultSubject.getPrincipals().add(primaryPrincipal);

        Set namedUserPasswordCredentials = defaultPrincipal.getNamedUserPasswordCredentials();
        if (namedUserPasswordCredentials != null) {
            defaultSubject.getPrivateCredentials().addAll(namedUserPasswordCredentials);
        }

        return defaultSubject;
    }


    /**
     * A simple helper method to register PolicyContextHandlers
     *
     * @param handler an object that implements the <code>PolicyContextHandler</code>
     *                interface. The value of this parameter must not be null.
     * @param replace this boolean value defines the behavior of this method
     *                if, when it is called, a <code>PolicyContextHandler</code> has already
     *                been registered to handle the same key. In that case, and if the value
     *                of this argument is true, the existing handler is replaced with the
     *                argument handler. If the value of this parameter is false the existing
     *                registration is preserved and an exception is thrown.
     */
    public static void registerPolicyContextHandler(PolicyContextHandler handler, boolean replace) throws PolicyContextException {
        String[] keys = handler.getKeys();

        for (int i = 0; i < keys.length; i++) {
            PolicyContext.registerHandler(keys[i], handler, replace);
        }
    }


}
