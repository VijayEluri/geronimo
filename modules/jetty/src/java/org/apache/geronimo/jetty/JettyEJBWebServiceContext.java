/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.apache.geronimo.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.geronimo.security.ContextManager;
import org.apache.geronimo.webservices.WebServiceContainer;
import org.mortbay.http.Authenticator;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.ClientCertAuthenticator;
import org.mortbay.http.DigestAuthenticator;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;

/**
 * Delegates requests to a WebServiceContainer which is presumably for an EJB WebService.
 * <p/>
 * WebServiceContainer delegates to an EJBContainer that will ultimately provide the JNDI,
 * TX, and Security services for this web service.
 * <p/>
 * Nothing stopping us from using this for POJOs or other types of webservices if shared
 * Context (JNDI, tx, security) wasn't required to be supplied by the web context.
 * <p/>
 * From a 10,000 foot view the Jetty architecture has:
 * Container -> Context -> Holder -> Servlet
 * <p/>
 * A Container has multiple Contexts, typically webapps
 * A Context provides the JNDI, TX, and Security for the webapp and has many Holders
 * A Holder simply wraps each Servlet
 * <p/>
 * The POJO Web Service architecture on Jetty looks like this:
 * Container -> WebApp Context -> JettyPOJOWebServiceHolder -> POJOWebServiceServlet
 * <p/>
 * The EJB Web Service architecure, on the other hand, creates one Context for each EJB:
 * Container -> JettyEJBWebServiceContext
 *
 * @version $Rev$ $Date$
 */
public class JettyEJBWebServiceContext extends HttpContext implements HttpHandler {

    private final String contextPath;
    private final WebServiceContainer webServiceContainer;
    private final Authenticator authenticator;
    private final JAASJettyRealm realm;
    private final boolean isConfidentialTransportGuarantee;
    private final boolean isIntegralTransportGuarantee;
    private final ClassLoader classLoader;

    private HttpContext httpContext;

    public JettyEJBWebServiceContext(String contextPath, WebServiceContainer webServiceContainer, InternalJAASJettyRealm internalJAASJettyRealm, String realmName, String transportGuarantee, String authMethod, ClassLoader classLoader) {
        this.contextPath = contextPath;
        this.webServiceContainer = webServiceContainer;
        if (internalJAASJettyRealm != null) {
            JAASJettyRealm realm = new JAASJettyRealm(realmName, internalJAASJettyRealm);
            setRealm(realm);
            this.realm = realm;
            if ("NONE".equals(transportGuarantee)) {
                isConfidentialTransportGuarantee = false;
                isIntegralTransportGuarantee = false;
            } else if ("INTEGRAL".equals(transportGuarantee)) {
                isConfidentialTransportGuarantee = false;
                isIntegralTransportGuarantee = true;
            } else if ("CONFIDENTIAL".equals(transportGuarantee)) {
                isConfidentialTransportGuarantee = true;
                isIntegralTransportGuarantee = false;
            } else {
                throw new IllegalArgumentException("Invalid transport-guarantee: " + transportGuarantee);
            }
            if ("BASIC".equals(authMethod)) {
                authenticator = new BasicAuthenticator();
            } else if ("DIGEST".equals(authMethod)) {
                authenticator = new DigestAuthenticator();
            } else if ("CLIENT-CERT".equals(authMethod)) {
                authenticator = new ClientCertAuthenticator();
            } else if ("NONE".equals(authMethod)) {
                authenticator = null;
            } else {
                throw new IllegalArgumentException("Invalid authMethod: " + authMethod);
            }
        } else {
            realm = null;
            authenticator = null;
            isConfidentialTransportGuarantee = false;
            isIntegralTransportGuarantee = false;
        }
        this.classLoader = classLoader;
    }

    public String getName() {
        //need a better name
        return contextPath;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public void initialize(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    public void handle(HttpRequest req, HttpResponse res) throws HttpException, IOException {
        req.setContentType("text/xml");
        RequestAdapter request = new RequestAdapter(req);
        ResponseAdapter response = new ResponseAdapter(res);

        if (req.getParameter("wsdl") != null) {
            try {
                webServiceContainer.getWsdl(request, response);
                //WHO IS RESPONSIBLE FOR CLOSING OUT?
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw (HttpException) new HttpException(500, "Could not fetch wsdl!").initCause(e);
            }
        } else {
            if (isConfidentialTransportGuarantee) {
                if (!req.isConfidential()) {
                    throw new HttpException(403);
                }
            } else if (isIntegralTransportGuarantee) {
                if (!req.isIntegral()) {
                    throw new HttpException(403);
                }
            }
            Thread currentThread = Thread.currentThread();
            ClassLoader oldClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(classLoader);
            //hard to imagine this could be anything but null, but....
//            Subject oldSubject = ContextManager.getCurrentCaller();
            try {
                if (authenticator != null) {
                    String pathInContext = org.mortbay.util.URI.canonicalPath(req.getPath());
                    if (authenticator.authenticate(realm, pathInContext, req, res) == null) {
                        throw new HttpException(403);
                    }
                } else {
                    //EJB will figure out correct defaultSubject shortly
                    //TODO consider replacing the GenericEJBContainer.DefaultSubjectInterceptor with this line
                    //setting the defaultSubject.
                    ContextManager.popCallers(null);
                }
                try {
                    webServiceContainer.invoke(request, response);
                    req.setHandled(true);
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw (HttpException) new HttpException(500, "Could not process message!").initCause(e);
                }
            } finally {
//                ContextManager.setCurrentCaller(oldSubject);
                currentThread.setContextClassLoader(oldClassLoader);
            }
        }

    }

    public String getContextPath() {
        return contextPath;
    }

    public String getSecurityRealmName() {
        if (realm == null) {
            return null;
        } else {
            return realm.getSecurityRealmName();
        }
    }

    public static class RequestAdapter implements WebServiceContainer.Request {
        private final HttpRequest request;
        private URI uri;

        public RequestAdapter(HttpRequest request) {
            this.request = request;
        }

        public String getHeader(String name) {
            return request.getField(name);
        }

        public java.net.URI getURI() {
            if (uri == null) {
                try {
                    String uriString = request.getScheme() + "://" + request.getHost() + ":" + request.getPort() + request.getURI();
                    //return new java.net.URI(uri.getScheme(),uri.getHost(),uri.getPath(),uri.);
                    uri = new java.net.URI(uriString);
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
            return uri;
        }

        public int getContentLength() {
            return request.getContentLength();
        }

        public String getContentType() {
            return request.getContentType();
        }

        public InputStream getInputStream() throws IOException {
            return request.getInputStream();
        }

        public int getMethod() {
            Integer method = (Integer) methods.get(request.getMethod());
            return method == null ? UNSUPPORTED : method.intValue();
        }

        public String getParameter(String name) {
            return request.getParameter(name);
        }

        public Map getParameters() {
            return request.getParameters();
        }

        public Object getAttribute(String name) {
            return request.getAttribute(name);
        }

        public void setAttribute(String name, Object value) {
            request.setAttribute(name, value);
        }


        private static final Map methods = new HashMap();

        static {
            methods.put("OPTIONS", new Integer(OPTIONS));
            methods.put("GET", new Integer(GET));
            methods.put("HEAD", new Integer(HEAD));
            methods.put("POST", new Integer(POST));
            methods.put("PUT", new Integer(PUT));
            methods.put("DELETE", new Integer(DELETE));
            methods.put("TRACE", new Integer(TRACE));
            methods.put("CONNECT", new Integer(CONNECT));
        }

    }

    public static class ResponseAdapter implements WebServiceContainer.Response {
        private final HttpResponse response;

        public ResponseAdapter(HttpResponse response) {
            this.response = response;
        }

        public void setHeader(String name, String value) {
            response.setField(name, value);
        }

        public String getHeader(String name) {
            return response.getField(name);
        }

        public OutputStream getOutputStream() {
            return response.getOutputStream();
        }

        public void setStatusCode(int code) {
            response.setStatus(code);
        }

        public int getStatusCode() {
            return response.getStatus();
        }

        public void setContentType(String type) {
            response.setContentType(type);
        }

        public String getContentType() {
            return response.getContentType();
        }

        public void setStatusMessage(String responseString) {
            response.setStatus(response.getStatus(), responseString);
        }
    }

}
