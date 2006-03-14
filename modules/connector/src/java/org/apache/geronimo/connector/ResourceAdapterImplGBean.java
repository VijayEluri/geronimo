/**
 *
 * Copyright 2005 The Apache Software Foundation
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

package org.apache.geronimo.connector;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.management.ResourceAdapter;

public class ResourceAdapterImplGBean {

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(ResourceAdapterImplGBean.class, ResourceAdapterImpl.class, NameFactory.RESOURCE_ADAPTER);
        infoBuilder.addAttribute("objectName", String.class, false);
        infoBuilder.addReference("JCAResourceImpl", JCAResourceImpl.class, NameFactory.JCA_RESOURCE);
        infoBuilder.addAttribute("JCAResource", String.class, false);
        infoBuilder.addInterface(ResourceAdapter.class);

        infoBuilder.setConstructor(new String[]{
            "objectName",
            "JCAResourceImpl"
        });

        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
