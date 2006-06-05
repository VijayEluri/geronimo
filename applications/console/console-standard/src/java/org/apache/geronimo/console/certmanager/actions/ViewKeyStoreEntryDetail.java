/**
 *
 * Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable.
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

package org.apache.geronimo.console.certmanager.actions;

import java.io.IOException;
import java.security.cert.Certificate;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.geronimo.console.certmanager.CertManagerPortlet;
import org.apache.geronimo.console.core.keystore.KeyEntryInfo;
import org.apache.geronimo.console.util.ObjectNameConstants;
import org.apache.geronimo.kernel.KernelRegistry;
import org.apache.geronimo.gbean.AbstractName;

public class ViewKeyStoreEntryDetail {

    public static void render(CertManagerPortlet portlet,
            RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        String alias = request.getParameter("alias");

        portlet.getPortletContext().log(
                "view-key-store-entry-detail: key-store-alias = " + alias);

        try {
            // entry info
            AbstractName name = ObjectNameConstants.KEYSTORE_OBJ_NAME;
            KeyEntryInfo kinfo = (KeyEntryInfo) KernelRegistry
                    .getSingleKernel().invoke(name, "getKeyEntryInfo",
                            new Object[] { alias },
                            new String[] { "java.lang.String" });

            request.setAttribute("org.apache.geronimo.console.keystore.entry.info", kinfo);

            // get keystore certificate chain by the alias
            Certificate[] chain = (Certificate[]) KernelRegistry
                    .getSingleKernel().invoke(name, "getCertificateChain",
                            new Object[] { alias },
                            new String[] { "java.lang.String" });

            // set attributes
            request.setAttribute("org.apache.geronimo.console.certs", chain);
        } catch (Exception e) {
            throw new PortletException(e);
        }

        // display entry detail
        PortletRequestDispatcher rd = portlet.getPortletContext()
                .getRequestDispatcher(
                        "/WEB-INF/view/certmanager/viewCertificateNormal.jsp");

        rd.include(request, response);
    }
}
