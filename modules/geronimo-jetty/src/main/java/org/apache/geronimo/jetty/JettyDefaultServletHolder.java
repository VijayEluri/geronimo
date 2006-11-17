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

package org.apache.geronimo.jetty;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * @version $Rev$ $Date$
 */
public class JettyDefaultServletHolder extends JettyServletHolder {

    public JettyDefaultServletHolder() {
    }

    public JettyDefaultServletHolder(String objectName, String servletName, String servletClassName, String jspFile, Map initParams, Integer loadOnStartup, Set servletMappings, Subject runAsSubject, ServletHolder previous, JettyServletRegistration context) throws Exception {
        super(objectName, servletName, servletClassName, jspFile, initParams, loadOnStartup, servletMappings, runAsSubject, previous, context);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(JettyDefaultServletHolder.class, JettyServletHolder.GBEAN_INFO, NameFactory.SERVLET_TEMPLATE);

        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }


}
