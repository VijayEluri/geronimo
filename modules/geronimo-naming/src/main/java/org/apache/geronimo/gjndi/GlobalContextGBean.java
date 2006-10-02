/**
 *
 * Copyright 2006 The Apache Software Foundation
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
package org.apache.geronimo.gjndi;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.kernel.Kernel;
import org.apache.xbean.naming.global.GlobalContextManager;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import java.util.Collections;

/**
 * @version $Rev$ $Date$
 */
public class GlobalContextGBean extends KernelContextGBean implements GBeanLifecycle {
    public GlobalContextGBean(Kernel kernel) throws NamingException {
        super("", new AbstractNameQuery(null, Collections.EMPTY_MAP, Context.class.getName()), kernel);
    }

    public void doStart() {
        super.doStart();
        GlobalContextManager.setGlobalContext(this);
    }

    public void doStop() {
        GlobalContextManager.setGlobalContext(null);
        super.doStop();
    }

    public void doFail() {
        GlobalContextManager.setGlobalContext(null);
        super.doFail();
    }

    protected Name createBindingName(AbstractName abstractName, Object value) throws NamingException {
        if (value instanceof Context) {
            // don't bind yourself
            if (value == this) return null;

            Context context = (Context) value;
            String nameInNamespace = context.getNameInNamespace();
            return getNameParser().parse(nameInNamespace);
        }
        throw new NamingException("value is not a context: abstractName=" + abstractName + " valueType=" + value.getClass().getName());
    }

    public static final GBeanInfo GBEAN_INFO;

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    static {
        GBeanInfoBuilder builder = GBeanInfoBuilder.createStatic(GlobalContextGBean.class, "GlobalContext");
        builder.setConstructor(new String[]{"kernel"});
        GBEAN_INFO = builder.getBeanInfo();
    }
}
