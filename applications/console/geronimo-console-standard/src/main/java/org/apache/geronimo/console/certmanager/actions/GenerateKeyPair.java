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

package org.apache.geronimo.console.certmanager.actions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.geronimo.console.certmanager.CertManagerPortlet;
import org.apache.geronimo.kernel.KernelRegistry;
import org.apache.geronimo.gbean.AbstractName;

public class GenerateKeyPair {
    public static void action(CertManagerPortlet portlet,
            ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        response.setRenderParameter("action", request.getParameter("action"));

        String action = request.getParameter("action");

        if (action == null) {
            return;
        }

        if (action.equals("generate-key-pair")) {

            String submit = request.getParameter("submit");
            String alias = request.getParameter("alias");
            String keyalg = request.getParameter("keyalg");
            String keysize = request.getParameter("keysize");
            String sigalg = request.getParameter("sigalg");
            String validity = request.getParameter("validity");

            String cn = request.getParameter("cn");
            String ou = request.getParameter("ou");
            String o = request.getParameter("o");
            String l = request.getParameter("l");
            String st = request.getParameter("st");
            String c = request.getParameter("c");

            if (submit.equals("cancel")) {
                return;
            }

            try {
                Integer ikeysize = new Integer(Integer.parseInt(keysize));
                Integer ivalidity = new Integer(Integer.parseInt(validity));

                KernelRegistry.getSingleKernel().invoke(
                        portlet.getKeyStoreObjectName(),
                        "generateKeyPair",
                        new Object[] { alias, keyalg, ikeysize, sigalg,
                                ivalidity, cn, ou, o, l, st, c },
                        new String[] { "java.lang.String", "java.lang.String",
                                "java.lang.Integer", "java.lang.String",
                                "java.lang.Integer", "java.lang.String",
                                "java.lang.String", "java.lang.String",
                                "java.lang.String", "java.lang.String",
                                "java.lang.String" });
            } catch (Exception ex) {
                throw new PortletException(ex);
            }
        }
    }

    public static void render(CertManagerPortlet portlet,
            RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        String action = request.getParameter("action");

        PortletRequestDispatcher rd = null;

        if (action.equals("tools-generate-key-pair")) {
            rd = portlet.getPortletContext().getRequestDispatcher(
                    "/WEB-INF/view/certmanager/generateKeyPairNormal.jsp");
        } else {
            try {
                AbstractName name = portlet.getKeyStoreObjectName();

                String keyStoreType = (String) KernelRegistry.getSingleKernel()
                        .getAttribute(name, "keyStoreType");
                String keyStoreProvider = (String) KernelRegistry
                        .getSingleKernel().getAttribute(name,
                                "keyStoreProvider");
                String keyStoreLocation = (String) KernelRegistry
                        .getSingleKernel().getAttribute(name,
                                "keyStoreLocation");

                request.setAttribute("org.apache.geronimo.console.keystore.type",
                        keyStoreType);
                request.setAttribute("org.apache.geronimo.console.keystore.provider",
                        keyStoreProvider);
                request.setAttribute("org.apache.geronimo.console.keystore.location",
                        keyStoreLocation);

                List storelist = (List) KernelRegistry.getSingleKernel()
                        .invoke(name, "getKeyStoreEntries");

                Iterator iter = storelist.iterator();
                while (iter.hasNext()) {
                    portlet.getPortletContext().log(
                            "store-item = " + iter.next());
                }

                request
                        .setAttribute("org.apache.geronimo.console.keystore.list",
                                storelist);
                request.setAttribute("org.apache.geronimo.console.keystore.size", String
                        .valueOf(storelist.size()));
            } catch (Exception e) {
                throw new PortletException(e);
            }

            rd = portlet.getPortletContext().getRequestDispatcher(
                    "/WEB-INF/view/certmanager/viewKeyStoreNormal.jsp");
        }

        rd.include(request, response);
    }
}
