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
package org.apache.geronimo.console.configcreator;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.console.MultiPageModel;

/**
 * A handler for ...
 * 
 * @version $Rev$ $Date$
 */
public class EnvironmentHandler extends AbstractHandler {
    private static final Log log = LogFactory.getLog(EnvironmentHandler.class);

    public EnvironmentHandler() {
        super(ENVIRONMENT_MODE, "/WEB-INF/view/configcreator/environment.jsp");
    }

    public String actionBeforeView(ActionRequest request, ActionResponse response, MultiPageModel model)
            throws PortletException, IOException {
        return getMode();
    }

    public void renderView(RenderRequest request, RenderResponse response, MultiPageModel model)
            throws PortletException, IOException {
        WARConfigData data = getSessionData(request);
        request.setAttribute(DATA_PARAMETER, data);
    }

    public String actionAfterView(ActionRequest request, ActionResponse response, MultiPageModel model)
            throws PortletException, IOException {
        WARConfigData data = getSessionData(request);
        data.readEnvironmentData(request);
        if (data.getEjbRefs().size() > 0 || data.getEjbLocalRefs().size() > 0 || data.getJdbcPoolRefs().size() > 0
                || data.getJmsConnectionFactoryRefs().size() > 0 || data.getJmsDestinationRefs().size() > 0) {
            return REFERENCES_MODE + "-before";
        }
        if (data.getSecurity() != null) {
            return SECURITY_MODE + "-before";
        }
        return DEPENDENCIES_MODE + "-before";
    }
}
