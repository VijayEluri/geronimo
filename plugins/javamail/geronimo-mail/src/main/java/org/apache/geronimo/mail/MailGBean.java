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
package org.apache.geronimo.mail;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.naming.ResourceSource;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.management.JavaMailResource;


/**
 * GBean that provides access to JavaMail Sessions.
 * <p/>
 * This GBean is used to generate JavaMail Sessions.  JavaMail properties that
 * are common to all JavaMail Sessions are provided via member variables of this
 * class.
 *
 * @version $Rev$ $Date$
 * @see ProtocolGBean
 * @see SMTPTransportGBean
 * @see POP3StoreGBean
 * @see IMAPStoreGBean
 */
public class MailGBean implements GBeanLifecycle, JavaMailResource, ResourceSource {

    private static final Logger log = LoggerFactory.getLogger(MailGBean.class);

    private final String objectName;
    private final Collection protocols;
    private Boolean useDefault;
    private Properties properties;
    private Authenticator authenticator;
    private String storeProtocol;
    private String transportProtocol;
    private String host;
    private String user;
    private Boolean debug;
    private String jndiName;


    /**
     * Construct an instance of MailGBean
     * <p/>
     * Values that are set in the individual member variables will override any of
     * the corresponding values that have been set in the properties set.
     *
     * @param protocols         the set of protocol GBeans that contain protocol specific configurations
     * @param useDefault        whether this GBean will return default Sessions or not
     * @param properties        the set of default properties for the protocols
     * @param authenticator     the authenticator object
     * @param storeProtocol     the store protocol that Sessions created from this GBean will return
     * @param transportProtocol the transport protocol that Sessions created from this GBean will return
     * @param host              the default Mail server
     * @param user              the username to provide when connecting to a Mail server
     * @param debug             the debug setting for Sessions created from this GBean
     * @param jndiName          the JNDI name to which the mail Session should be bound
     */
    public MailGBean(String objectName, Collection protocols, Boolean useDefault, Properties properties, Authenticator authenticator,
                     String storeProtocol, String transportProtocol, String host, String user, Boolean debug, String jndiName) {
        this.objectName = objectName;
        this.protocols = protocols;
        setUseDefault(useDefault);
        this.properties = (properties == null ? new Properties() : properties);
        setAuthenticator(authenticator);
        setStoreProtocol(storeProtocol);
        setTransportProtocol(transportProtocol);
        setHost(host);
        setUser(user);
        setDebug(debug);
        setJndiName(jndiName);
    }

    /**
     * Returns the set of protocol GBeans that contain protocol specific configurations.
     */
    public Collection getProtocols() {
        return protocols;
    }

    /**
     * Returns whether this GBean will return default Sessions or not.
     */
    public Boolean getUseDefault() {
        return useDefault;
    }

    /**
     * Sets whether this GBean will return default Sessions or not,
     *
     * @param useDefault whether this GBean will return default Sessions or not
     */
    public void setUseDefault(Boolean useDefault) {
        this.useDefault = useDefault;
    }

    /**
     * Returns the set of default properties for the protocols.
     * <p/>
     * Note: Proerties that are set here will override the properties that are
     * set in the protocol GBeans.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the set of default properties for the protocols.
     * <p/>
     * Note: Proerties that are set here will override the properties that are
     * set in the protocol GBeans.
     *
     * @param properties the set of default properties for the protocols
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Returns the authenticator object.
     * <p/>
     * Used only if a new Session object is created. Otherwise, it must match
     * the Authenticator used to create the Session.
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Sets the authenticator object.
     * <p/>
     * Used only if a new Session object is created. Otherwise, it must match
     * the Authenticator used to create the Session.
     *
     * @param authenticator the authenticator object
     */
    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Returns the store protocol that Sessions created from this GBean will return.
     * <p/>
     * Specifies the default Message Access Protocol. The Session.getStore()
     * method returns a Store object that implements this protocol. The client
     * can override this property and explicitly specify the protocol with the
     * Session.getStore(String protocol) method.
     */
    public String getStoreProtocol() {
        return storeProtocol;
    }

    /**
     * Sets the store protocol that Sessions created from this GBean will return.
     * <p/>
     * Specifies the default Message Access Protocol. The Session.getStore()
     * method returns a Store object that implements this protocol. The client
     * can override this property and explicitly specify the protocol with the
     * Session.getStore(String protocol) method.
     * <p/>
     * Values that are set here will override any of the corresponding value
     * that has been set in the properties.
     *
     * @param storeProtocol the store protocol that Sessions created from this GBean will return
     */
    public void setStoreProtocol(String storeProtocol) {
        this.storeProtocol = storeProtocol;
    }

    /**
     * Returns the transport protocol that Sessions created from this GBean will return.
     * <p/>
     * Specifies the default Transport Protocol. The Session.getTransport()
     * method returns a Transport object that implements this protocol. The
     * client can override this property and explicitly specify the protocol
     * by using Session.getTransport(String protocol) method.
     */
    public String getTransportProtocol() {
        return transportProtocol;
    }

    /**
     * Sets the transport protocol that Sessions created from this GBean will return.
     * <p/>
     * Specifies the default Transport Protocol. The Session.getTransport()
     * method returns a Transport object that implements this protocol. The
     * client can override this property and explicitly specify the protocol
     * by using Session.getTransport(String protocol) method.
     * <p/>
     * Values that are set here will override any of the corresponding value
     * that has been set in the properties.
     *
     * @param transportProtocol the transport protocol that Sessions created from this GBean will return
     */
    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    /**
     * Returns the default Mail server.
     * <p/>
     * Specifies the default Mail server. The Store and Transport object’s
     * connect methods use this property, if the protocolspecific host property
     * is absent, to locate the target host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the default Mail server.
     * <p/>
     * Specifies the default Mail server. The Store and Transport object’s
     * connect methods use this property, if the protocolspecific host property
     * is absent, to locate the target host.
     * <p/>
     * Values that are set here will override any of the corresponding value
     * that has been set in the properties.
     *
     * @param host the default Mail server
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the username to provide when connecting to a Mail server.
     * <p/>
     * Specifies the username to provide when connecting to a Mail server. The
     * Store and Transport object’s connect methods use this property, if the
     * protocolspecific username property is absent, to obtain the username.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the username to provide when connecting to a Mail server.
     * <p/>
     * Specifies the username to provide when connecting to a Mail server. The
     * Store and Transport object’s connect methods use this property, if the
     * protocolspecific username property is absent, to obtain the username.
     * <p/>
     * Values that are set here will override any of the corresponding value
     * that has been set in the properties.
     *
     * @param user the username to provide when connecting to a Mail server
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the debug setting for Sessions created from this GBean.
     */
    public Boolean getDebug() {
        return debug;
    }

    /**
     * Sets the debug setting for Sessions created from this GBean.
     * <p/>
     * Values that are set here will override any of the corresponding value
     * that has been set in the properties.
     *
     * @param debug the debug setting for Sessions created from this GBean
     */
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    /**
     * Gets the JNDI name to which the mail Session should be bound
     * @return the JNDI name to which the mail Session should be bound
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * Sets the JNDI name to which the mail Session should be bound
     * @param jndiName the JNDI name to which the mail Session should be bound
     */
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public Object $getResource() {
        Properties props = new Properties(properties);

        if (protocols != null) {
            for (Iterator iter = protocols.iterator(); iter.hasNext();) {
                ProtocolGBean protocol = (ProtocolGBean) iter.next();
                protocol.addOverrides(props);
            }
        }

        props.putAll(properties);

        if (storeProtocol != null) props.put("mail.store.protocol", storeProtocol);
        if (transportProtocol != null) props.put("mail.transport.protocol", transportProtocol);
        if (host != null) props.put("mail.host", host);
        if (user != null) props.put("mail.user", user);
        // this needs to be translated into a string version.
        if (debug != null) props.put("mail.debug", debug.toString());

        if (Boolean.TRUE.equals(useDefault)) {
            if (authenticator == null) {
                return Session.getDefaultInstance(props);
            } else {
                return Session.getDefaultInstance(props, authenticator);
            }
        } else {
            if (authenticator == null) {
                return Session.getInstance(props);
            } else {
                return Session.getInstance(props, authenticator);
            }
        }
    }

    public void doStart() throws Exception {
        log.debug("Started " + objectName + " - will return "
                 + (Boolean.TRUE.equals(useDefault) ? "default" : "new")
                 + " JavaMail Session "
                 + (authenticator == null ? "without" : "with")
                 + " authenticator");

        String jndiName = getJndiName();
        if (jndiName != null && jndiName.length() > 0) {
            // first get the resource incase there are exceptions
            Object value = $getResource();

            // get the initial context
            Context context = new InitialContext();
            Name parsedName = context.getNameParser("").parse(jndiName);

            // create intermediate contexts
            for (int i = 1; i < parsedName.size(); i++) {
                Name contextName = parsedName.getPrefix(i);
                if (!bindingExists(context, contextName)) {
                    context.createSubcontext(contextName);
                }
            }

            // bind
            context.bind(jndiName, value);
            log.info("JavaMail session bound to " + jndiName);
        }
    }

    public void doStop() throws Exception {
        log.debug("Stopped " + objectName);
        stop();
    }

    public void doFail() {
        log.warn("Failed " + objectName);
        stop();
    }

    private void stop() {
        String jndiName = getJndiName();
        if (jndiName != null && jndiName.length() > 0) {
            try {
                Context context = new InitialContext();
                context.unbind(jndiName);
                log.info("JavaMail session unbound from " + jndiName);
            } catch (NamingException e) {
                // we tried... this is a common error which occurs during shutdown due to ordering
            }
        }
    }

    private static boolean bindingExists(Context context, Name contextName) {
        try {
            return context.lookup(contextName) != null;
        } catch (NamingException e) {
        }
        return false;
    }

    /**
     * Returns the GBean name of this Mail GBean
     */
    public String getObjectName() {
        return objectName;
    }

    public boolean isStateManageable() {
        return false;
    }

    public boolean isStatisticsProvider() {
        return false;
    }

    public boolean isEventProvider() {
        return false;
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(MailGBean.class, NameFactory.JAVA_MAIL_RESOURCE);

        infoFactory.addAttribute("objectName", String.class, false);
        infoFactory.addReference("Protocols", ProtocolGBean.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addAttribute("useDefault", Boolean.class, true);
        infoFactory.addAttribute("properties", Properties.class, true);
        infoFactory.addReference("Authenticator", Authenticator.class, NameFactory.GERONIMO_SERVICE);
        infoFactory.addAttribute("storeProtocol", String.class, true);
        infoFactory.addAttribute("transportProtocol", String.class, true);
        infoFactory.addAttribute("host", String.class, true);
        infoFactory.addAttribute("user", String.class, true);
        infoFactory.addAttribute("debug", Boolean.class, true);
        infoFactory.addAttribute("jndiName", String.class, true);
        infoFactory.addOperation("$getResource");
        infoFactory.addOperation("getProtocols");
        infoFactory.addInterface(JavaMailResource.class);

        infoFactory.setConstructor(new String[]{"objectName",
                                                "Protocols",
                                                "useDefault",
                                                "properties",
                                                "Authenticator",
                                                "storeProtocol",
                                                "transportProtocol",
                                                "host",
                                                "user",
                                                "debug",
                                                "jndiName"});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
