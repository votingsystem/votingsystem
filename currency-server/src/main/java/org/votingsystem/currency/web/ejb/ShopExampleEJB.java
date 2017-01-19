package org.votingsystem.currency.web.ejb;


import org.votingsystem.currency.web.util.AsyncRequestShopBundle;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.currency.TransactionResponseDto;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class ShopExampleEJB {

    private static Logger log = Logger.getLogger(ShopExampleEJB.class.getName());

    private static final Map<String, AsyncRequestShopBundle> transactionRequestMap = new HashMap<>();

    @Inject private ConfigCurrencyServer config;

    public void putTransactionRequest(String sessionId, TransactionDto transactionRequest) {
        log.info("putTransactionRequest - sessionId $sessionId");
        transactionRequestMap.put(sessionId, new AsyncRequestShopBundle(transactionRequest, null));
    }

    public AsyncRequestShopBundle getRequestBundle(String sessionId) {
        if(transactionRequestMap.containsKey(sessionId)) {
            transactionRequestMap.get(sessionId).getAsyncResponse().setTimeout(180, TimeUnit.SECONDS);
            return transactionRequestMap.get(sessionId);
        } else return null;
    }

    public TransactionDto getTransactionRequest(String sessionId) {
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
            return ResponseDto.SC_OK;
        } else return ResponseDto.SC_ERROR;
    }

    public void sendResponse(String sessionId, TransactionResponseDto responseDto) throws Exception {
        log.info("sessionId: " + sessionId);
        /*CMSSignedMessage cmsMessage = responseDto.getCMS();
        AsyncRequestShopBundle requestBundle = transactionRequestMap.remove(sessionId);
        if(requestBundle != null) {
            try {
                if(responseDto.getCurrencyChangeCert() != null) {
                    X509Certificate currencyCert = PEMUtils.fromPEMToX509Cert(responseDto.getCurrencyChangeCert().getBytes());
                    CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                            currencyCert, Constants.CURRENCY_OID);
                    Currency currency = requestBundle.getCurrency(certExtensionDto.getHashCertVS());
                    currency.initSigner(responseDto.getCurrencyChangeCert().getBytes());
                    log.info("TODO - currency OK save to wallet: " + currency.getAmount() + " " +
                            currency.getCurrencyCode() + " - " + currency.getTag().getName());
                }
                requestBundle.getTransactionDto().validateReceipt(cmsMessage, true);
                MessageDto<TransactionDto> messageDto = MessageDto.OK("OK");
                messageDto.setData(requestBundle.getTransactionDto());
                requestBundle.getAsyncResponse().resume(Response.ok().entity(JSON.getMapper()
                        .writeValueAsBytes(messageDto)).type(MediaType.JSON).build());
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                requestBundle.getAsyncResponse().resume(Response.status(ResponseDto.SC_OK).entity(
                        JSON.getMapper().writeValueAsBytes(MessageDto.ERROR(ex.getMessage()))).build());
            }
        } else throw new ValidationException("transactionRequest with sessionId:" + sessionId + " has expired");*/
    }

}