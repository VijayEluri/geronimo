/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.cxf.ejb;

import java.lang.reflect.Method;
import java.util.List;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.jaxws.handler.LogicalHandlerInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Element;

public class EJBInterceptor {

    private static final Log LOG = LogFactory.getLog(EJBInterceptor.class);

    private Exchange exchange;
    private Bus bus;
    private EJBEndpoint ejbEndpoint;
    private List<Object> params;
    private Method method;

    public EJBInterceptor(List<Object> params,
                          Method method,
                          EJBEndpoint endpoint,
                          Bus bus,
                          Exchange exchange) {
        this.params = params;
        this.method = method;
        this.ejbEndpoint = endpoint;
        this.bus = bus;
        this.exchange = exchange;
    }

    private static void copyDataBindingInterceptors(PhaseInterceptorChain newChain,
                                                    InterceptorChain oldChain) {
        for (Interceptor interceptor : oldChain) {
            if (interceptor instanceof AbstractInDatabindingInterceptor) {
                LOG.debug("Added data binding interceptor: " + interceptor);
                newChain.add(interceptor);
            }
        }
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Endpoint endpoint = this.exchange.get(Endpoint.class);
        Service service = endpoint.getService();
        Binding binding = ((JaxWsEndpointImpl) endpoint).getJaxwsBinding();

        this.exchange.put(InvocationContext.class, context);

        if (binding.getHandlerChain() == null || binding.getHandlerChain().isEmpty()) {
            // no handlers so let's just directly invoke the bean
            LOG.debug("No handlers found.");
            
            EJBMethodInvoker invoker = (EJBMethodInvoker) service.getInvoker();
            return invoker.directEjbInvoke(this.exchange, this.method, this.params);

        } else {
            // have handlers so have to run handlers now and redo data binding
            // as handlers can change the soap message
            LOG.debug("Handlers found.");
                        
            // inject handlers (on first call only)
            this.ejbEndpoint.injectHandlers();

            Message inMessage = this.exchange.getInMessage();
            PhaseInterceptorChain chain = 
                new PhaseInterceptorChain(this.bus.getExtension(PhaseManager.class).getInPhases());

            chain.setFaultObserver(endpoint.getOutFaultObserver());

            /*
             * Since we have to re-do data binding and the XMLStreamReader
             * contents are already consumed by prior data binding step
             * we have to reinitialize the XMLStreamReader from the SOAPMessage
             * created by SAAJInInterceptor. 
             */
            if (inMessage instanceof SoapMessage) {
                try {
                    reserialize((SoapMessage)inMessage);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to reserialize soap message", e);
                }
            } else {
                // TODO: how to handle XML/HTTP binding?
            }
            
            this.exchange.setOutMessage(null);

            // install default interceptors
            chain.add(new ServiceInvokerInterceptor());
            chain.add(new OutgoingChainInterceptor());

            // install interceptors for handler processing
            chain.add(new MustUnderstandInterceptor());
            chain.add(new LogicalHandlerInterceptor(binding));
            chain.add(new SOAPHandlerInterceptor(binding));

            // install data binding interceptors
            copyDataBindingInterceptors(chain, inMessage.getInterceptorChain());

            InterceptorChain oldChain = inMessage.getInterceptorChain();
            inMessage.setInterceptorChain(chain);
            try {
                chain.doIntercept(inMessage);
            } finally {
                inMessage.setInterceptorChain(oldChain);
            }

            // TODO: the result should be deserialized from SOAPMessage
            Object result = getResult();

            return result;
        }
    }

    private Object getResult() {
        Message outMessage = this.exchange.getOutMessage();
        if (outMessage == null) {
            return null;
        } else {
            List<?> result = outMessage.getContent(List.class);
            if (result == null) {
                return outMessage.get(Object.class);
            } else if (result.isEmpty()) {
                return null;
            } else {
                return result.get(0);
            }
        }
    }
    
    private void reserialize(SoapMessage message) throws Exception {
        SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
        if (soapMessage == null) {
            return;
        }
        
        //replace header element if necessary
        if (message.hasHeaders(Element.class)) {
            Element headerElements = soapMessage.getSOAPHeader();    
            message.setHeaders(Element.class, headerElements);
        }
        
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        StaxUtils.readDocElements(soapMessage.getSOAPBody(), xmlReader, true);
        DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
        xmlReader = StaxUtils.createXMLStreamReader(bodySource);
        xmlReader.nextTag();
        xmlReader.nextTag(); // move past body tag
        message.setContent(XMLStreamReader.class, xmlReader);
    }
        
}