package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
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

    private static Logger log = Logger.getLogger(ShopExampleBean.class.getName());

    private static final Map<String, AsyncRequestBundle> transactionRequestMap = new HashMap<>();

    @Inject ConfigVS config;
    @Inject MessagesBean messages;


    public void putTransactionRequest(String sessionId, TransactionVSDto transactionRequest) {
        log.info("putTransactionRequest - sessionId $sessionId");
        transactionRequestMap.put(sessionId, new AsyncRequestBundle(transactionRequest, null));
    }

    public TransactionVSDto getTransactionRequest(String sessionId) {
        return transactionRequestMap.get(sessionId).dto;
    }

    public Set<String> getSessionKeys() {
        return transactionRequestMap.keySet();
    }

    public void bindContext(String sessionId, AsyncResponse asyncResponse) {
        transactionRequestMap.get(sessionId).asyncResponse = asyncResponse;
    }

    public void sendResponse(String sessionId, SMIMEMessage smimeMessage) throws ExceptionVS, IOException, ParseException {
        log.info("sendResponse");
        AsyncRequestBundle asyncRequestBundle = transactionRequestMap.remove(sessionId);
        if(asyncRequestBundle != null) {
            try {
                asyncRequestBundle.dto.validateReceipt(smimeMessage);
                asyncRequestBundle.asyncResponse.resume(Response.ok().entity(JSON.getMapper()
                        .writeValueAsBytes(asyncRequestBundle.dto)).type(MediaTypeVS.JSON).build());
            } catch (Exception ex) {
                asyncRequestBundle.asyncResponse.resume(Response.status(ResponseVS.SC_ERROR).entity(ex.getMessage()).build());
            }
        } else throw new ExceptionVS("transactionRequest with sessionId:" + sessionId + " has expired");
    }

    private static class AsyncRequestBundle {
        TransactionVSDto dto;
        AsyncResponse asyncResponse;
        public AsyncRequestBundle(TransactionVSDto dto, AsyncResponse asyncResponse) {
            this.dto = dto;
            this.asyncResponse = asyncResponse;
        }
    }

}