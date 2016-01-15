package org.votingsystem.web.currency.servlet;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.currency.ejb.CurrencyBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MultipartRequestVS;

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
@MultipartConfig(location="/tmp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*50, maxRequestSize=1024*1024*5*50)
public class CurrencyRequestServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(CurrencyRequestServlet.class.getSimpleName());

    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CurrencyBean currencyBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        MessageSMIME messageSMIME = null;
        try {
            MultipartRequestVS requestVS = new MultipartRequestVS(req.getParts(), MultipartRequestVS.Type.CURRENCY_REQUEST);
            messageSMIME = signatureBean.validateSMIME(
                    requestVS.getSMIME(), ContentTypeVS.JSON_SIGNED).getMessageSMIME();
            CurrencyRequestDto requestDto = CurrencyRequestDto.validateRequest(requestVS.getCSRBytes(),
                    messageSMIME, config.getContextURL());
            requestDto.setTagVS(config.getTag(requestDto.getTagVS().getName()));
            ResultListDto<String> dto = currencyBean.processCurrencyRequest(requestDto);
            resp.setContentType(MediaTypeVS.JSON);
            resp.getOutputStream().write(JSON.getMapper().writeValueAsBytes(dto));
        } catch (ExceptionVS ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            if(messageSMIME != null) {
                messageSMIME.setType(TypeVS.EXCEPTION).setReason(ex.getMessage());
                dao.merge(messageSMIME);
            }
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
        return "servlet that process currency request";
    }

}
