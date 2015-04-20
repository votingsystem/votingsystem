package org.votingsystem.web.accesscontrol.servlet;


import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessRequestVS;
import org.votingsystem.signature.util.CsrResponse;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.accesscontrol.ejb.AccessRequestBean;
import org.votingsystem.web.accesscontrol.ejb.CSRBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/accessRequestVS")
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class AccessRequestVSServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(AccessRequestVSServlet.class.getSimpleName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject AccessRequestBean accessRequestBean;
    @Inject CSRBean csrBean;
    @Inject DAOBean dao;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, IOException {
        AccessRequestVS accessRequestVS = null;
        try {
            MultipartRequestVS requestVS = new MultipartRequestVS(req.getParts(), MultipartRequestVS.Type.ACCESS_REQUEST);
            MessageSMIME messageSMIME = signatureBean.validateSMIME(
                    requestVS.getSMIME(), ContentTypeVS.JSON_SIGNED).getMessageSMIME();
            AccessRequestDto requestDto = accessRequestBean.saveRequest(messageSMIME);
            accessRequestVS = requestDto.getAccessRequestVS();
            UserVS signer = accessRequestVS.getUserVS();
            CsrResponse csrResponse = null;
            if(signer.getType() == UserVS.Type.REPRESENTATIVE) {
                csrResponse = csrBean.signCertVoteVS(requestVS.getCSRBytes(), requestDto.getEventVS());
            } else csrResponse = csrBean.signRepresentativeCertVoteVS(
                    requestVS.getCSRBytes(), requestDto.getEventVS(), signer);
            resp.setContentType(ContentTypeVS.TEXT_STREAM.getName());
            resp.setContentLength(csrResponse.getIssuedCert().length);
            resp.getOutputStream().write(csrResponse.getIssuedCert());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            if(accessRequestVS != null && accessRequestVS.getId() != null && accessRequestVS.getState()
                    == AccessRequestVS.State.OK) {
                accessRequestVS.setMetaInf(ex.getMessage());
                dao.merge(accessRequestVS.setState(AccessRequestVS.State.CANCELED));
            }
            resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
            String message = ex.getMessage() != null ? ex.getMessage(): "EXCEPTION: " + ex.getClass();
            resp.getOutputStream().write(message.getBytes());
        }
    }

    @Override
    public String getServletInfo() {
        return "servlet that process currency request";
    }

}