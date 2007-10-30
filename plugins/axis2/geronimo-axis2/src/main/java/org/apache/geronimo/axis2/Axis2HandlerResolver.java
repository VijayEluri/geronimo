/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geronimo.axis2;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;

import org.apache.geronimo.jaxws.annotations.AnnotationException;
import org.apache.geronimo.jaxws.annotations.AnnotationProcessor;
import org.apache.geronimo.xbeans.javaee.HandlerChainType;
import org.apache.geronimo.xbeans.javaee.HandlerChainsType;

/**
 * @version $Rev$ $Date$
 */
public class Axis2HandlerResolver implements HandlerResolver {

    private HandlerChainsType handlerChains;

    private ClassLoader classLoader;

    private Class serviceClass;

    private AnnotationProcessor annotationProcessor;

    public Axis2HandlerResolver(ClassLoader classLoader,
                                Class serviceClass,
                                HandlerChainsType handlerChains,
                                AnnotationProcessor annotationProcessor) {
        this.classLoader = classLoader;
        this.serviceClass = serviceClass;
        this.handlerChains = handlerChains;
        this.annotationProcessor = annotationProcessor;
    }

    public List<Handler> getHandlerChain(javax.xml.ws.handler.PortInfo portInfo) {

        GeronimoHandlerChainBuilder builder =
                new GeronimoHandlerChainBuilder(this.classLoader, portInfo);

        List<Handler> handlers = null;
        if (this.handlerChains == null) {
            handlers = builder.buildHandlerChainFromClass(this.serviceClass);
        } else {
            handlers = new ArrayList<Handler>();
            for (HandlerChainType handlerChain : this.handlerChains.getHandlerChainArray()) {
                handlers.addAll(builder.buildHandlerChainFromConfiguration(handlerChain));
            }
            handlers = builder.sortHandlers(handlers);
        }

        if (this.annotationProcessor != null) {
            try {
                for (Handler handler : handlers) {
                    this.annotationProcessor.processAnnotations(handler);
                    this.annotationProcessor.invokePostConstruct(handler);
                }
            } catch (AnnotationException e) {
                throw new WebServiceException("Handler annotation failed", e);
            }
        }

        return handlers;
    }

}
