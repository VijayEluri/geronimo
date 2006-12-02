/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.geronimo.jetty6.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.handler.AbstractHandler;

/**
 * @version $Rev$ $Date$
 */
public class ThreadClassloaderHandler extends AbstractImmutableHandler {

    private final ClassLoader classLoader;

    public ThreadClassloaderHandler(AbstractHandler next, ClassLoader classLoader) {
        super(next);
        this.classLoader = classLoader;
    }

    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        Thread thread = Thread.currentThread();
        ClassLoader oldClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            next.handle(target, request, response, dispatch);
        } finally {
            thread.setContextClassLoader(oldClassLoader);
        }
    }

    public void lifecycleCommand(LifecycleCommand lifecycleCommand) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader oldClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            super.lifecycleCommand(lifecycleCommand);
        } finally {
            thread.setContextClassLoader(oldClassLoader);
        }
    }
}
