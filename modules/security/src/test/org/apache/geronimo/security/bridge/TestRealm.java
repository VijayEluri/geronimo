/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */

package org.apache.geronimo.security.bridge;

import javax.security.auth.login.AppConfigurationEntry;

import java.util.HashMap;
import java.util.Set;

import org.apache.geronimo.gbean.GAttributeInfo;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GConstructorInfo;
import org.apache.geronimo.gbean.GOperationInfo;
import org.apache.geronimo.security.GeronimoSecurityException;
import org.apache.geronimo.security.realm.providers.AbstractSecurityRealm;
import org.apache.regexp.RE;


/**
 * @version $Revision: 1.2 $ $Date: 2004/02/17 00:05:40 $
 */
public class TestRealm extends AbstractSecurityRealm {
    public final static String REALM_NAME = "bridge-realm";
    public final static String JAAS_NAME = "bridge";
    private static final GBeanInfo GBEAN_INFO;

    public TestRealm() {
    }

    public TestRealm(String realmName) {
        super(realmName);
    }

    public Set getGroupPrincipals() throws GeronimoSecurityException {
        return null;
    }

    public Set getGroupPrincipals(RE regexExpression) throws GeronimoSecurityException {
        return null;
    }

    public Set getUserPrincipals() throws GeronimoSecurityException {
        return null;
    }

    public Set getUserPrincipals(RE regexExpression) throws GeronimoSecurityException {
        return null;
    }

    public void refresh() throws GeronimoSecurityException {
    }

    public AppConfigurationEntry getAppConfigurationEntry() {
        return new AppConfigurationEntry(TestLoginModule.class.getName(),
                                         AppConfigurationEntry.LoginModuleControlFlag.REQUISITE,
                                         new HashMap());
    }

    public boolean isLoginModuleLocal() {
        return true;
    }

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(TestRealm.class.getName(), AbstractSecurityRealm.getGBeanInfo());
        infoFactory.addAttribute(new GAttributeInfo("debug", true));
        infoFactory.addOperation(new GOperationInfo("isLoginModuleLocal"));
        infoFactory.setConstructor(new GConstructorInfo(new String[]{"RealmName"},
                                                        new Class[]{String.class}));
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
