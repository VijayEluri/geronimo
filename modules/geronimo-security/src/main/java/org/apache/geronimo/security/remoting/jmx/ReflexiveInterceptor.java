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

package org.apache.geronimo.security.remoting.jmx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.geronimo.interceptor.Interceptor;
import org.apache.geronimo.interceptor.Invocation;
import org.apache.geronimo.interceptor.InvocationResult;
import org.apache.geronimo.interceptor.SimpleInvocationResult;

/**
 * @version $Rev$ $Date$
 */
public class ReflexiveInterceptor implements Interceptor {

    Object target;

    public ReflexiveInterceptor(Object target) {
        this.target = target;
    }

    public InvocationResult invoke(Invocation invocation) throws Throwable {
        try {
            SerializableInvocation proxyInvocation = (SerializableInvocation) invocation;
            Method m = proxyInvocation.getMethod();
            Object args[] = proxyInvocation.getArgs();
            Object rc = m.invoke(target, args);
            return new SimpleInvocationResult(true, rc);

        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception && t instanceof RuntimeException == false) {
                return new SimpleInvocationResult(false, (Exception)t);
            } else {
                throw t;
            }
        }
    }

}