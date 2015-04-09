package org.votingsystem.web.currency.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyRequestBatch;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.CurrencyBean;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/currency/request") //old_url -> /cooin/request
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class CurrencyRequestServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(CurrencyRequestServlet.class.getSimpleName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject CurrencyBean currencyBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, IOException {
        try {
            MultipartRequestVS requestVS = new MultipartRequestVS(req.getParts(), MultipartRequestVS.Type.CURRENCY_REQUEST);
            MessageSMIME messageSMIME = signatureBean.validateSMIME(
                    requestVS.getSMIME(), ContentTypeVS.JSON_SIGNED).getMessageSMIME();
            CurrencyRequestBatch currencyBatch = new CurrencyRequestBatch(requestVS.getCSRBytes(),
                    messageSMIME, config.getContextURL());
            currencyBatch.setTagVS(config.getTag(currencyBatch.getTag()));
            Map result = currencyBean.processCurrencyRequest(currencyBatch);
            resp.setContentType(ContentTypeVS.JSON.getName());
            resp.getOutputStream().write(new ObjectMapper().writeValueAsBytes(result));
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
