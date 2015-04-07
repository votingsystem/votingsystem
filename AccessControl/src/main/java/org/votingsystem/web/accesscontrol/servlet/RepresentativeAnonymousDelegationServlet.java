package org.votingsystem.web.accesscontrol.servlet;


import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeDelegationBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.MultipartRequestVS;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/representative/anonymousDelegation")
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class RepresentativeAnonymousDelegationServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(RepresentativeAnonymousDelegationServlet.class.getSimpleName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject RepresentativeDelegationBean representativeDelegationBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, IOException {
        try {
            MultipartRequestVS requestVS = new MultipartRequestVS(req.getParts(), MultipartRequestVS.Type.ANONYMOUS_DELEGATION);
            MessageSMIME messageSMIME = signatureBean.processSMIMERequest(requestVS.getSMIME(), ContentTypeVS.JSON_SIGNED);
            X509Certificate anonymousIssuedCert = representativeDelegationBean.validateAnonymousRequest(messageSMIME, requestVS.getCSRBytes());
            byte[] issuedCertPEMBytes = CertUtils.getPEMEncoded(anonymousIssuedCert);
            resp.setContentType(ContentTypeVS.PEM.getName());
            resp.setContentLength(issuedCertPEMBytes.length);
            resp.getOutputStream().write(issuedCertPEMBytes);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
            resp.getOutputStream().write(ex.getMessage().getBytes());
        }
    }

    @Override
    public String getServletInfo() {
        return "servlet that process currency request";
    }

}