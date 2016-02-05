package org.votingsystem.web.accesscontrol.servlet;


import org.votingsystem.dto.MessageDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CsrResponse;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.accesscontrol.ejb.AccessRequestBean;
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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/accessRequestVS")
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class AccessRequestVSServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(AccessRequestVSServlet.class.getName());

    @Inject SignatureBean signatureBean;
    @Inject AccessRequestBean accessRequestBean;
    @Inject ConfigVS config;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, IOException {
        try {
            MultipartRequestVS requestVS = new MultipartRequestVS(req.getParts(), MultipartRequestVS.Type.ACCESS_REQUEST);
            MessageSMIME messageSMIME = signatureBean.validateSMIME(
                    requestVS.getSMIME(), ContentTypeVS.JSON_SIGNED).getMessageSMIME();
            CsrResponse csrResponse = accessRequestBean.saveRequest(messageSMIME, requestVS.getCSRBytes());
            resp.setContentType(ContentTypeVS.TEXT_STREAM.getName());
            resp.setContentLength(csrResponse.getIssuedCert().length);
            resp.getOutputStream().write(csrResponse.getIssuedCert());
        } catch (Exception ex) {
            if(ex instanceof ExceptionVS && ((ExceptionVS) ex).getMessageDto() != null) {
                MessageDto messageDto = ((ExceptionVS) ex).getMessageDto();
                log.severe(messageDto.toString());
                resp.setStatus(messageDto.getStatusCode());
                resp.setHeader("Content-Type", MediaTypeVS.JSON);
                resp.getOutputStream().write(JSON.getMapper().writeValueAsBytes(messageDto));
            } else {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
                String message = ex.getMessage() != null ? ex.getMessage(): "EXCEPTION: " + ex.getClass();
                resp.getOutputStream().write(message.getBytes());
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "servlet that process anonymous vote certificate request";
    }

}