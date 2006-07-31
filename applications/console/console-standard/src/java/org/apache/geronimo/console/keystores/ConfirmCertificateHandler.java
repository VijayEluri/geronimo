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
package org.apache.geronimo.console.keystores;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.console.MultiPageModel;
import org.apache.geronimo.util.CertificateUtil;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collection;

/**
 * Handler for entering a password to unlock a keystore
 *
 * @version $Rev$ $Date$
 */
public class ConfirmCertificateHandler extends BaseKeystoreHandler {
    private final static Log log = LogFactory.getLog(ConfirmCertificateHandler.class);

    public ConfirmCertificateHandler() {
        super(CONFIRM_CERTIFICATE, "/WEB-INF/view/keystore/confirmCertificate.jsp");
    }

    public String actionBeforeView(ActionRequest request, ActionResponse response, MultiPageModel model) throws PortletException, IOException {
        return getMode();
    }

    public void renderView(RenderRequest request, RenderResponse response, MultiPageModel model) throws PortletException, IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        request.setAttribute("id", request.getParameter("id"));
        request.setAttribute("alias", request.getParameter("alias"));
        /*  // Uploading certificate using a disk file fails on Windows.  Certificate text is used instead.
        String certFile = request.getParameter("certificate");
        request.setAttribute("certificate", certFile);
        InputStream is = new FileInputStream(certFile);
        */
        String certificate = request.getParameter("certificate");
        request.setAttribute("certificate", certificate);
        InputStream is = new ByteArrayInputStream(certificate.getBytes());
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection certificates = cf.generateCertificates(is);
            X509Certificate cert = (X509Certificate) certificates.iterator().next();
            request.setAttribute("fingerprint", CertificateUtil.generateFingerprint(cert, "MD5"));
            request.setAttribute("issuer", cert.getIssuerDN().getName());
            request.setAttribute("subject", cert.getSubjectDN().getName());
            request.setAttribute("serial", cert.getSerialNumber());
            request.setAttribute("validStart", sdf.format(cert.getNotBefore()));
            request.setAttribute("validEnd", sdf.format(cert.getNotAfter()));
        } catch (CertificateException e) {
            log.error("Unable to process uploaded certificate", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to process uploaded certificate", e);
        }
    }

    public String actionAfterView(ActionRequest request, ActionResponse response, MultiPageModel model) throws PortletException, IOException {
        String id = request.getParameter("id");
        String alias = request.getParameter("alias");
        String certificate = request.getParameter("certificate");
        if(id == null || id.equals("") || alias == null || alias.equals("") || certificate == null || certificate.equals("")) {
            return LIST_MODE+BEFORE_ACTION; //todo: better handling
        }
        KeystoreData data = ((KeystoreData) request.getPortletSession(true).getAttribute(KEYSTORE_DATA_PREFIX + id));
        try {
            if(!data.importTrustCert(certificate, alias)) {
                log.error("Unable to import certificate");
            }
        } catch (CertificateException e) {
            log.error("Unable to import certificate", e);
        }
        response.setRenderParameter("id", id);
        return VIEW_KEYSTORE+BEFORE_ACTION;
    }
}
