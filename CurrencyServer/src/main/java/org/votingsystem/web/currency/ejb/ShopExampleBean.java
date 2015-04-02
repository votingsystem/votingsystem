package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.util.TransactionRequest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class ShopExampleBean {

    private static Logger log = Logger.getLogger(ShopExampleBean.class.getSimpleName());

    private static Map<String, TransactionRequest> transactionRequestMap = new HashMap<String, TransactionRequest>();

    @Inject ConfigVS config;
    @Inject MessagesBean messages;


    public void putTransactionRequest(String sessionId, TransactionRequest transactionRequest) {
        log.info("putTransactionRequest - sessionId $sessionId");
        transactionRequestMap.put(sessionId, transactionRequest);
    }

    public TransactionRequest getTransactionRequest(String sessionId) {
        return transactionRequestMap.get(sessionId);
    }

    public Set<String> getSessionKeys() {
        return transactionRequestMap.keySet();
    }

    public void bindContext(String sessionId, AsyncContext ctx) {
        transactionRequestMap.get(sessionId).setAsyncContext(ctx);
    }

    public void sendResponse(String sessionId, SMIMEMessage smimeMessage) throws ExceptionVS, IOException, ParseException {
        log.info("sendResponse");
        TransactionRequest transactionRequest = transactionRequestMap.remove(sessionId);
        if(transactionRequest != null) {
            SignedReceiptContent receiptContent = new SignedReceiptContent(smimeMessage.getSignedContent());
            receiptContent.transactionRequest.checkRequest(transactionRequest);
            Map dataMap = new HashMap<>();
            dataMap.put("status", ResponseVS.SC_OK);
            dataMap.put("message", receiptContent.getMessage());
            transactionRequest.getAsyncContext().getResponse().getWriter().write(
                    new ObjectMapper().writeValueAsString(dataMap));
            transactionRequest.getAsyncContext().complete();
        } else throw new ExceptionVS("transactionRequest with sessionId:" + sessionId + " has expired");
    }

    private class SignedReceiptContent {

        TransactionRequest transactionRequest;

        public SignedReceiptContent(String signedContent) throws ExceptionVS, IOException, ParseException {
            transactionRequest = TransactionRequest.parse(new ObjectMapper().readValue(
                    signedContent, new TypeReference<HashMap<String, Object>>() {}));
        }

        public String getMessage() {
            return messages.get("transactionRequestOKMsg", transactionRequest.getAmount().toString(),
                    transactionRequest.getCurrencyCode());
        }
    }

}