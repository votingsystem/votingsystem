package org.votingsystem.idprovider.servlet;

import org.votingsystem.crypto.CsrResponse;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.AnonCertMultipartRequest;
import org.votingsystem.http.ContentType;
import org.votingsystem.idprovider.ejb.CertIssuerEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
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

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * Servlet that issues the anonymous certificates with which the votes are signed
 */
@WebServlet("/election/validateIdentity")
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*10, maxRequestSize=1024*1024*5*10)
public class VoteCertificateIssuerServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(VoteCertificateIssuerServlet.class.getName());

    @EJB CertIssuerEJB certIssuer;
    @Inject private SignatureService signatureService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    //Called from the mobile with the signed data that identifies the user and the election. If the request is correct
    //the server provides the anonymous certificate with which the vote is signed
    @TransactionAttribute(REQUIRES_NEW)
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SignedDocument signedDocument = null;
        try {
            AnonCertMultipartRequest request = new AnonCertMultipartRequest(req.getParts(),
                    AnonCertMultipartRequest.Type.ELECTION_IDENTIFICATION);
            SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                    SignedDocumentType.ANON_VOTE_CERT_REQUEST).setWithTimeStampValidation(true);
            signedDocument = signatureService.validateXAdESAndSave(request.getDssDocument(), signatureParams);
            CsrResponse csrResponse = certIssuer.processAnonymousCertificateRequest(signedDocument, request.getCSRBytes());
            res.setContentType(ContentType.TEXT_STREAM.getName());
            if(csrResponse.getIssuedCert() != null) {
                res.setContentLength(csrResponse.getIssuedCert().length);
                res.getOutputStream().write(csrResponse.getIssuedCert());
            } else sendErrorResponse(csrResponse.getStatusCode(), csrResponse.getMessage(), res);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            String message = ex.getMessage() != null ? ex.getMessage(): "EXCEPTION: " + ex.getClass();
            sendErrorResponse(ResponseDto.SC_ERROR_REQUEST, message, res);
        }
    }

    private void sendErrorResponse(int statusCode, String message, HttpServletResponse res) throws IOException {
        res.setStatus(statusCode);
        res.getOutputStream().write(message.getBytes());
    }

    @Override
    public String getServletInfo() {
        return "servlet that process anonymous vote certificate request";
    }

}