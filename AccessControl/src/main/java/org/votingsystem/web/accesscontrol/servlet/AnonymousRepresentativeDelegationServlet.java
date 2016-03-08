package org.votingsystem.web.accesscontrol.servlet;

import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeDelegationBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MultipartRequestVS;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/representative/anonymousDelegationRequest")
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class AnonymousRepresentativeDelegationServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(AnonymousRepresentativeDelegationServlet.class.getName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject RepresentativeDelegationBean representativeDelegationBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Transactional
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, IOException {
        try {
            MultipartRequestVS requestVS = new MultipartRequestVS(req.getParts(), MultipartRequestVS.Type.ANONYMOUS_DELEGATION);
            MessageCMS messageCMS = signatureBean.validateCMS(
                    requestVS.getCMS(), ContentTypeVS.JSON_SIGNED).getMessageCMS();
            X509Certificate anonymousIssuedCert = representativeDelegationBean.validateAnonymousRequest(
                    messageCMS, requestVS.getCSRBytes());
            byte[] issuedCertPEMBytes = PEMUtils.getPEMEncoded(anonymousIssuedCert);
            resp.setContentType(MediaTypeVS.PEM);
            resp.setContentLength(issuedCertPEMBytes.length);
            resp.getOutputStream().write(issuedCertPEMBytes);
        } catch (ExceptionVS ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            if(ex.getMessageDto() != null) {
                resp.setStatus(ex.getMessageDto().getStatusCode());
                resp.setContentType(MediaTypeVS.JSON);
                resp.getOutputStream().write(JSON.getMapper().writeValueAsBytes(ex.getMessageDto()));
            } else writeException(resp, ex);
        } catch (Exception ex) {
            writeException(resp, ex);
        }
    }

    private void writeException(HttpServletResponse resp, Exception ex) throws IOException {
        log.log(Level.SEVERE, ex.getMessage(), ex);
        resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
        String message = ex.getMessage() != null ? ex.getMessage(): "EXCEPTION: " + ex.getClass();
        resp.getOutputStream().write(message.getBytes());
    }

    @Override
    public String getServletInfo() {
        return "servlet that process anonymous representative delegation";
    }

}