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
package org.apache.geronimo.tomcat;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.PermissionCollection;
import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.realm.JAASCallbackHandler;
import org.apache.catalina.realm.JAASRealm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.security.ContextManager;
import org.apache.geronimo.security.IdentificationPrincipal;
import org.apache.geronimo.security.SubjectId;
import org.apache.geronimo.security.deploy.DefaultPrincipal;
import org.apache.geronimo.security.jacc.PolicyContextHandlerContainerSubject;
import org.apache.geronimo.security.util.ConfigurationUtil;


public class TomcatGeronimoRealm extends JAASRealm {

    private static final Log log = LogFactory.getLog(TomcatGeronimoRealm.class);

    private final String policyContextID;
    private final Subject defaultSubject;
	private final DefaultPrincipal defaultPrincipal;
    private final PermissionCollection checked;
    private final PermissionCollection excluded;
    private final Map roleDesignates;
    private final String loginDomainName;

    private Context context = null;
    private static ThreadLocal currentRequest = new ThreadLocal();

    /**
     * Descriptive information about this <code>Realm</code> implementation.
     */
    protected static final String info = "org.apache.geronimo.tomcat.TomcatGeronimoRealm/1.0";

    /**
     * Descriptive information about this <code>Realm</code> implementation.
     */
    protected static final String name = "TomcatGeronimoRealm";

    public TomcatGeronimoRealm(String policyContextID,
                               DefaultPrincipal defaultPrincipal,
                               String loginDomainName,
                               PermissionCollection checkedPermissions,
                               PermissionCollection excludedPermissions,
                               Map roleDesignates) {

        assert policyContextID != null;
        assert defaultPrincipal != null;

        this.policyContextID = policyContextID;
        this.defaultPrincipal = defaultPrincipal;
        this.loginDomainName = loginDomainName;
        this.defaultSubject = ConfigurationUtil.generateDefaultSubject(defaultPrincipal);
        this.checked = checkedPermissions;
        this.excluded = excludedPermissions;
        this.roleDesignates = roleDesignates;

        /**
         * Register our default subject with the ContextManager
         */
        ContextManager.registerSubject(defaultSubject);
        SubjectId id = ContextManager.getSubjectId(defaultSubject);
        defaultSubject.getPrincipals().add(new IdentificationPrincipal(id));

     }

    /**
     * Enforce any user data constraint required by the security constraint
     * guarding this request URI.  Return <code>true</code> if this constraint
     * was not violated and processing should continue, or <code>false</code>
     * if we have created a response already.
     *
     * @param request     Request we are processing
     * @param response    Response we are creating
     * @param constraints Security constraint being checked
     * @throws IOException if an input/output error occurs
     */
    public boolean hasUserDataPermission(Request request,
                                         Response response,
                                         SecurityConstraint[] constraints)
            throws IOException {

        //Get an authenticated subject, if there is one
        Subject subject = null;
        try {

            //We will use the PolicyContextHandlerContainerSubject.HANDLER_KEY to see if a user
            //has authenticated, since a request.getUserPrincipal() will not pick up the user
            //unless its using a cached session.
            subject = (Subject) PolicyContext.getContext(PolicyContextHandlerContainerSubject.HANDLER_KEY);

        } catch (PolicyContextException e) {
            log.error(e);
        }

        //If nothing has authenticated yet, do the normal
        if (subject == null)
            return super.hasUserDataPermission(request, response, constraints);

        ContextManager.setCurrentCaller(subject);

        try {

            AccessControlContext acc = ContextManager.getCurrentContext();

            /**
             * JACC v1.0 secion 4.1.1
             */
            WebUserDataPermission wudp = new WebUserDataPermission(request);
            acc.checkPermission(wudp);

        } catch (AccessControlException ace) {
            response.sendError(Response.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    /**
     * Perform access control based on the specified authorization constraint.
     * Return <code>true</code> if this constraint is satisfied and processing
     * should continue, or <code>false</code> otherwise.
     *
     * @param request    Request we are processing
     * @param response   Response we are creating
     * @param constraint Security constraint we are enforcing
     * @param context    The Context to which client of this class is attached.
     * @throws java.io.IOException if an input/output error occurs
     */
    public boolean hasResourcePermission(Request request,
                                         Response response,
                                         SecurityConstraint[] constraint,
                                         Context context)
            throws IOException {

        //Set the current request (for hasRole)
        currentRequest.set(request);

        // Specifically allow access to the form login and form error pages
        // and the "j_security_check" action
        LoginConfig config = context.getLoginConfig();
        if ((config != null) &&
            (org.apache.catalina.realm.Constants.FORM_METHOD.equals(config.getAuthMethod()))) {
            String requestURI = request.getDecodedRequestURI();
            String loginPage = context.getPath() + config.getLoginPage();
            if (loginPage.equals(requestURI)) {
                if (log.isDebugEnabled())
                    log.debug(" Allow access to login page " + loginPage);
                return (true);
            }
            String errorPage = context.getPath() + config.getErrorPage();
            if (errorPage.equals(requestURI)) {
                if (log.isDebugEnabled())
                    log.debug(" Allow access to error page " + errorPage);
                return (true);
            }
            if (requestURI.endsWith(org.apache.catalina.realm.Constants.FORM_ACTION)) {
                if (log.isDebugEnabled())
                    log.debug(" Allow access to username/password submission");
                return (true);
            }
        }

        // Which user principal have we already authenticated?
        Principal principal = request.getUserPrincipal();

        //If we have no principal, then we should use the default.
        if (principal == null) {
            ContextManager.setCurrentCaller(defaultSubject);
        } else {
            ContextManager.setCurrentCaller(((JAASTomcatPrincipal) principal).getSubject());
        }

        try {

            AccessControlContext acc = ContextManager.getCurrentContext();


            /**
             * JACC v1.0 secion 4.1.2
             */
            acc.checkPermission(new WebResourcePermission(request));

        } catch (AccessControlException ace) {
            response.sendError(Response.SC_FORBIDDEN);
            return false;
        }

        return true;

    }

    private String getServletName(Request request) {

        String contextPath = ((HttpServletRequest) request.getRequest()).getContextPath();
        String requestURI = request.getDecodedRequestURI();
        String relativeURI = requestURI.substring(contextPath.length());
        String servletPath = relativeURI;
        String name = null;

        //Try exact match
        if (!(relativeURI.equals("/")))
            name = context.findServletMapping(relativeURI);

        //Try prefix match (i.e. xyz/* )
        if (name == null) {
            servletPath = relativeURI;
            while (true) {
                name = context.findServletMapping(servletPath + "/*");
                if (name != null) {
                    break;
                }
                int slash = servletPath.lastIndexOf('/');
                if (slash < 0)
                    break;
                servletPath = servletPath.substring(0, slash);
            }
        }

        //Try extension match (i.e. *.do )
        if (name == null) {
            int slash = relativeURI.lastIndexOf('/');
            if (slash >= 0) {
                String last = relativeURI.substring(slash);
                int period = last.lastIndexOf('.');
                if (period >= 0) {
                    String pattern = "*" + last.substring(period);
                    name = context.findServletMapping(pattern);
                }
            }
        }

        //Try default match
        if (name == null) {
            name = context.findServletMapping("/");
        }

        /**
         * JACC v1.0 secion B.19
         */
        if (name.equals("jsp")) {
            name = "";
        }

        return (name == null ? "" : name);
    }

    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>.
     *
     * @param principal Principal for whom the role is to be checked
     * @param role      Security role to be checked
     */
    public boolean hasRole(Principal principal, String role) {

        if ((principal == null) || (role == null) || !(principal instanceof JAASTomcatPrincipal)) {
            return false;
        }

        Request request = (Request) currentRequest.get();
        if (currentRequest == null) {
            log.error("No currentRequest found.");
            return false;
        }

        String name = getServletName(request);

        //Set the caller
        ContextManager.setCurrentCaller(((JAASTomcatPrincipal) principal).getSubject());

        AccessControlContext acc = ContextManager.getCurrentContext();

        try {
            /**
             * JACC v1.0 secion 4.1.3
             */
            acc.checkPermission(new WebRoleRefPermission(name, role));
        } catch (AccessControlException e) {
            return false;
        }

        return true;
    }

    /**
     * Return the <code>Principal</code> associated with the specified
     * username and credentials, if there is one; otherwise return
     * <code>null</code>.
     * <p/>
     * If there are any errors with the JDBC connection, executing the query or
     * anything we return null (don't authenticate). This event is also logged,
     * and the connection will be closed so that a subsequent request will
     * automatically re-open it.
     *
     * @param username    Username of the <code>Principal</code> to look up
     * @param credentials Password or other credentials to use in authenticating this
     *                    username
     */
    public Principal authenticate(String username, String credentials) {

        // Establish a LoginContext to use for authentication
        try {
            LoginContext loginContext = null;
            if (appName == null)
                appName = "Tomcat";

            if (log.isDebugEnabled())
                log.debug(sm.getString("jaasRealm.beginLogin", username, appName));

            // What if the LoginModule is in the container class loader ?
            ClassLoader ocl = null;

            if (isUseContextClassLoader()) {
                ocl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            }

            try {
                loginContext = new LoginContext(loginDomainName, new JAASCallbackHandler(this, username, credentials));
            } catch (Throwable e) {
                log.error(sm.getString("jaasRealm.unexpectedError"), e);
                return (null);
            } finally {
                if (isUseContextClassLoader()) {
                    Thread.currentThread().setContextClassLoader(ocl);
                }
            }

            if (log.isDebugEnabled())
                log.debug("Login context created " + username);

            // Negotiate a login via this LoginContext
            Subject subject = null;
            try {
                loginContext.login();
                Subject tempSubject = loginContext.getSubject();
                if (tempSubject == null) {
                    if (log.isDebugEnabled())
                        log.debug(sm.getString("jaasRealm.failedLogin", username));
                    return (null);
                }

                subject = ContextManager.getServerSideSubject(tempSubject);
                if (subject == null) {
                    if (log.isDebugEnabled())
                        log.debug(sm.getString("jaasRealm.failedLogin", username));
                    return (null);
                }

                ContextManager.setCurrentCaller(subject);

            } catch (AccountExpiredException e) {
                if (log.isDebugEnabled())
                    log.debug(sm.getString("jaasRealm.accountExpired", username));
                return (null);
            } catch (CredentialExpiredException e) {
                if (log.isDebugEnabled())
                    log.debug(sm.getString("jaasRealm.credentialExpired", username));
                return (null);
            } catch (FailedLoginException e) {
                if (log.isDebugEnabled())
                    log.debug(sm.getString("jaasRealm.failedLogin", username));
                return (null);
            } catch (LoginException e) {
                log.warn(sm.getString("jaasRealm.loginException", username), e);
                return (null);
            } catch (Throwable e) {
                log.error(sm.getString("jaasRealm.unexpectedError"), e);
                return (null);
            }

            if (log.isDebugEnabled())
                log.debug(sm.getString("jaasRealm.loginContextCreated", username));

            // Return the appropriate Principal for this authenticated Subject
/*            Principal principal = createPrincipal(username, subject);
            if (principal == null) {
                log.debug(sm.getString("jaasRealm.authenticateFailure", username));
                return (null);
            }
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("jaasRealm.authenticateSuccess", username));
            }
*/
            JAASTomcatPrincipal jaasPrincipal = new JAASTomcatPrincipal(username);
            jaasPrincipal.setSubject(subject);

            return (jaasPrincipal);

        } catch (Throwable t) {
            log.error("error ", t);
            return null;
        }
    }


    /**
     * Prepare for active use of the public methods of this <code>Component</code>.
     *
     * @throws org.apache.catalina.LifecycleException
     *          if this component detects a fatal error
     *          that prevents it from being started
     */
    public void start() throws LifecycleException {

        // Perform normal superclass initialization
        super.start();

    }


    /**
     * Gracefully shut down active use of the public methods of this <code>Component</code>.
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Perform normal superclass finalization
        super.stop();

        // Remove the defaultSubject
        ContextManager.unregisterSubject(defaultSubject);
    }

    public void setContext(Context context) {
        this.context = context;
    }

}
