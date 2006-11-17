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
package org.apache.geronimo.security.jaas.server;

import java.io.Serializable;
import java.util.Map;
import javax.security.auth.spi.LoginModule;

import org.apache.geronimo.common.GeronimoSecurityException;
import org.apache.geronimo.security.jaas.LoginModuleControlFlag;


/**
 * Describes the configuration of a LoginModule -- its name, class, control
 * flag, options, and the Geronimo extension for whether it should run on
 * the client side or server side.
 *
 * @version $Rev$ $Date$
 */
public class JaasLoginModuleConfiguration implements Serializable {
    private final boolean serverSide;
    private final String loginDomainName;
    private final LoginModuleControlFlag flag;
    private final String loginModuleName;
    private final Map options;
    private final boolean wrapPrincipals;
    private final transient ClassLoader classLoader;

    public JaasLoginModuleConfiguration(String loginModuleName, LoginModuleControlFlag flag, Map options,
                                        boolean serverSide, String loginDomainName, boolean wrapPrincipals, ClassLoader classLoader)
    {
        this.serverSide = serverSide;
        this.flag = flag;
        this.loginModuleName = loginModuleName;
        this.options = options;
        this.loginDomainName = loginDomainName;
        this.wrapPrincipals = wrapPrincipals;
        this.classLoader = classLoader;
    }

    public JaasLoginModuleConfiguration(String loginModuleName, LoginModuleControlFlag flag, Map options, boolean serverSide, ClassLoader classLoader) {
        this(loginModuleName, flag, options, serverSide, null, false, classLoader);
    }

    public String getLoginModuleClassName() {
        return loginModuleName;
    }

    public LoginModule getLoginModule(ClassLoader loader) throws GeronimoSecurityException {
        //TODO determine if this is ever called after serialization: if not the classloader passed in is unnecessary.
        if (classLoader != null) {
            loader = classLoader;
        }
        try {
            return (LoginModule) loader.loadClass(loginModuleName).newInstance();
        } catch (Exception e) {
            throw new GeronimoSecurityException("Unable to instantiate login module", e);
        }
    }

    public boolean isServerSide() {
        return serverSide;
    }

    public LoginModuleControlFlag getFlag() {
        return flag;
    }

    public Map getOptions() {
        return options;
    }

    public String getLoginDomainName() {
        return loginDomainName;
    }

    public boolean isWrapPrincipals() {
        return wrapPrincipals;
    }
}
