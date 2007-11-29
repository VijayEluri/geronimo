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
package org.apache.geronimo.console.keystores;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.console.MultiPageModel;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.io.IOException;

/**
 * Handler for changing keystore and private key passwords.
 *
 * @version $Rev$ $Date$
 */
public class ChangePasswordHandler extends BaseKeystoreHandler {
    private final static Log log = LogFactory.getLog(ChangePasswordHandler.class);
    public ChangePasswordHandler() {
        super(CHANGE_PASSWORD, "/WEB-INF/view/keystore/changePassword.jsp");
    }

    public String actionBeforeView(ActionRequest request, ActionResponse response, MultiPageModel model) throws PortletException, IOException {
        String[] params = {ERROR_MSG, INFO_MSG, "keystore", "alias"};
        for(int i = 0; i < params.length; ++i) {
            String value = request.getParameter(params[i]);
            if(value != null) response.setRenderParameter(params[i], value);
        }
        return getMode();
    }

    public void renderView(RenderRequest request, RenderResponse response, MultiPageModel model) throws PortletException, IOException {
        String[] params = {ERROR_MSG, INFO_MSG, "keystore", "alias"};
        for(int i = 0; i < params.length; ++i) {
            String value = request.getParameter(params[i]);
            if(value != null) request.setAttribute(params[i], value);
        }
        request.setAttribute("mode", getMode()+AFTER_ACTION);
    }

    public String actionAfterView(ActionRequest request, ActionResponse response, MultiPageModel model) throws PortletException, IOException {
        String keystore = request.getParameter("keystore");
        String alias = request.getParameter("alias");
        String password = request.getParameter("password");
        String newPassword = request.getParameter("newPassword");
        if(keystore == null || keystore.equals("")) {
            return getMode();
        } else if(password == null) {
            response.setRenderParameter("keystore", keystore);
            if(alias != null && !alias.equals("")) {
                response.setRenderParameter("alias", alias);
            }
            return getMode();
        }
        KeystoreData data = ((KeystoreData) request.getPortletSession(true).getAttribute(KEYSTORE_DATA_PREFIX + keystore));
        try {
            if(alias == null || alias.equals("")) {
                // Keystore password is to be changed.
                data.changeKeystorePassword(password.toCharArray(), newPassword.toCharArray());
                response.setRenderParameter("id", keystore);
                response.setRenderParameter(INFO_MSG, "Password changed for keystore '"+keystore+"'.");
                return VIEW_KEYSTORE+BEFORE_ACTION;
            } else {
                // Private key password is to be changed.
                data.changeKeyPassword(alias, password.toCharArray(), newPassword.toCharArray());
                response.setRenderParameter("id", keystore);
                response.setRenderParameter("alias", alias);
                response.setRenderParameter(INFO_MSG, "Password changed for private key '"+alias+"'.");
                return CERTIFICATE_DETAILS;
            }
        } catch (Exception e) {
            String message = "Unable to change password for "+ (alias == null || alias.equals("") ? "keystore "+keystore : "private key "+alias) + ".";
            response.setRenderParameter(ERROR_MSG, message+" "+e.toString());
            log.error(message, e);
            return getMode()+BEFORE_ACTION;
        }
    }
}
