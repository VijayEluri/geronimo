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
package org.apache.geronimo.cxf;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.geronimo.webservices.WebServiceContainer;
import org.apache.geronimo.webservices.saaj.SAAJUniverse;

public abstract class CXFWebServiceContainer implements WebServiceContainer {

    private static final Log LOG = LogFactory.getLog(CXFWebServiceContainer.class);
    
    protected final GeronimoDestination destination;

    protected final Bus bus;

    protected final CXFEndpoint endpoint;

    protected URL configurationBaseUrl;

    public CXFWebServiceContainer(Bus bus,
                                  URL configurationBaseUrl,
                                  Object target) {
        this.bus = bus;
        this.configurationBaseUrl = configurationBaseUrl;
            
        List ids = new ArrayList();
        ids.add("http://schemas.xmlsoap.org/wsdl/soap/");
               
        DestinationFactoryManager destinationFactoryManager = bus
                .getExtension(DestinationFactoryManager.class);
        GeronimoDestinationFactory factory = new GeronimoDestinationFactory(bus);
        factory.setTransportIds(ids);
                
        destinationFactoryManager.registerDestinationFactory(
                "http://cxf.apache.org/transports/http/configuration", factory);
        destinationFactoryManager.registerDestinationFactory(
                "http://cxf.apache.org/bindings/xformat", factory);
        destinationFactoryManager.registerDestinationFactory(
                "http://www.w3.org/2003/05/soap/bindings/HTTP/", factory);
        destinationFactoryManager.registerDestinationFactory(
                "http://schemas.xmlsoap.org/soap/http", factory);
        destinationFactoryManager.registerDestinationFactory(
                "http://schemas.xmlsoap.org/wsdl/http/", factory);
        destinationFactoryManager.registerDestinationFactory(
                "http://schemas.xmlsoap.org/wsdl/soap/http", factory);

        endpoint = publishEndpoint(target);
        destination = (GeronimoDestination) endpoint.getServer().getDestination();
    }

    public void invoke(Request request, Response response) throws Exception {
        if (request.getMethod() == Request.GET) {
            processGET(request, response);
        } else {
            processPOST(request, response);
        }
    }
    
    protected void processGET(Request request, Response response) throws Exception {
        if (request.getParameter("xsd") != null || request.getParameter("XSD") != null) {
            getWsdl(request, response);
        } else {
            EndpointInfo ei = this.destination.getEndpointInfo();
            response.setContentType("text/html");
            PrintWriter pw = new PrintWriter(response.getOutputStream());
            pw.write("<html><title>Web Service</title><body>");
            pw.write("Hi, this is '" + ei.getService().getName().getLocalPart() + "' web service.");
            pw.write("</body></html>");
            pw.flush();
        }
    }
    
    protected void processPOST(Request request, Response response) throws Exception {
        SAAJUniverse universe = new SAAJUniverse();
        universe.set(SAAJUniverse.SUN);
        try {
            destination.invoke(request, response);
        } finally {
            universe.unset();
        }
    }
   
    public void getWsdl(Request request, Response response) throws Exception {
        GeronimoQueryHandler queryHandler = new GeronimoQueryHandler(this.bus);
        URI requestURI = request.getURI();
        EndpointInfo ei = this.destination.getEndpointInfo();
        OutputStream out = response.getOutputStream();
        String baseUri = requestURI.toString();
        response.setContentType("text/xml");
        queryHandler.writeResponse(baseUri, null, ei, out);
    }    
        
    public void destroy() {
        if (this.endpoint != null) {
            this.endpoint.stop();
        }
    }

    abstract protected CXFEndpoint publishEndpoint(Object target);
        
    /*
     * Ensure the bus created is unqiue and non-shared. 
     * The very first bus created is set as a default bus which then can
     * be (re)used in other places.
     */
    public static Bus getBus() {        
        BusFactory.getDefaultBus();
        return new ExtensionManagerBus();
    }

}
