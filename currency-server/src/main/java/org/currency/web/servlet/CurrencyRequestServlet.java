package org.currency.web.servlet;

import org.currency.web.ejb.ConfigCurrencyServer;
import org.currency.web.ejb.CurrencyEJB;
import org.currency.web.ejb.CurrencySignatureEJB;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.util.OperationType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.http.AnonCertMultipartRequest;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.util.JSON;

import javax.inject.Inject;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/currency/request")
@MultipartConfig(location="D:\\temp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class CurrencyRequestServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(CurrencyRequestServlet.class.getName());

    @Inject private ConfigCurrencyServer config;
    @Inject private CurrencySignatureEJB signatureService;
    @Inject private CurrencyEJB currencyBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SignedDocument signedDocument = null;
        try {
            AnonCertMultipartRequest request = new AnonCertMultipartRequest(req.getParts(),
                    AnonCertMultipartRequest.Type.CURRENCY_REQUEST);
            SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                    OperationType.CURRENCY_REQUEST).setWithTimeStampValidation(true);
            signedDocument = signatureService.validateXAdESAndSave(request.getDssDocument(), signatureParams);
            CurrencyRequestDto requestDto = CurrencyRequestDto.validateRequest(request.getRequestCSRSet(),
                    signedDocument, config.getEntityId());
            ResultListDto<String> dto = currencyBean.processCurrencyRequest(requestDto);
            res.setStatus(dto.getStatusCode());
            res.setContentType(MediaType.JSON);
            res.getOutputStream().write(new JSON().getMapper().writeValueAsBytes(dto));
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
        return "servlet that process currency request";
    }

}
