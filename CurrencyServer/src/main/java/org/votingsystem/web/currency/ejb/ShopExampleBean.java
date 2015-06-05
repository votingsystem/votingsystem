package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.currency.util.AsyncRequestShopBundle;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class ShopExampleBean {

    private static Logger log = Logger.getLogger(ShopExampleBean.class.getName());

    private static final Map<String, AsyncRequestShopBundle> transactionRequestMap = new HashMap<>();

    @Inject ConfigVS config;


    public void putTransactionRequest(String sessionId, TransactionVSDto transactionRequest) {
        log.info("putTransactionRequest - sessionId $sessionId");
        transactionRequestMap.put(sessionId, new AsyncRequestShopBundle(transactionRequest, null));
    }

    public AsyncRequestShopBundle getRequestBundle(String sessionId) {
        if(transactionRequestMap.containsKey(sessionId)) {
            transactionRequestMap.get(sessionId).getAsyncResponse().setTimeout(180, TimeUnit.SECONDS);
            return transactionRequestMap.get(sessionId);
        } else return null;
    }

    public TransactionVSDto getTransactionRequest(String sessionId) {
        if(transactionRequestMap.containsKey(sessionId)) {
            transactionRequestMap.get(sessionId).getAsyncResponse().setTimeout(180, TimeUnit.SECONDS);
            return transactionRequestMap.get(sessionId).getTransactionDto();
        } else return null;
    }

    public Set<String> getSessionKeys() {
        return transactionRequestMap.keySet();
    }

    public int bindContext(String sessionId, AsyncResponse asyncResponse) {
        if(transactionRequestMap.get(sessionId) != null) {
            transactionRequestMap.get(sessionId).setAsyncResponse(asyncResponse);
            return ResponseVS.SC_OK;
        } else return ResponseVS.SC_ERROR;
    }

    public void sendResponse(String sessionId, SMIMEMessage smimeMessage) throws ExceptionVS, IOException, ParseException {
        log.info("sendResponse");
        AsyncRequestShopBundle AsyncRequestShopBundle = transactionRequestMap.remove(sessionId);
        if(AsyncRequestShopBundle != null) {
            try {
                AsyncRequestShopBundle.getTransactionDto().validateReceipt(smimeMessage, true);
                MessageDto<TransactionVSDto> messageDto = MessageDto.OK("OK");
                messageDto.setData(AsyncRequestShopBundle.getTransactionDto());
                AsyncRequestShopBundle.getAsyncResponse().resume(Response.ok().entity(JSON.getMapper()
                        .writeValueAsBytes(messageDto)).type(MediaTypeVS.JSON).build());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                AsyncRequestShopBundle.getAsyncResponse().resume(Response.status(ResponseVS.SC_OK).entity(
                        JSON.getMapper().writeValueAsBytes(MessageDto.ERROR(ex.getMessage()))).build());
            }
        } else throw new ExceptionVS("transactionRequest with sessionId:" + sessionId + " has expired");
    }

}