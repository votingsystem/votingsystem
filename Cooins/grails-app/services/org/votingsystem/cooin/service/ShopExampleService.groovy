package org.votingsystem.cooin.service

import grails.transaction.Transactional
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.votingsystem.cooin.TransactionRequest
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.throwable.ExceptionVS

import javax.servlet.AsyncContext

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class ShopExampleService {

    private static Map<String, TransactionRequest> transactionRequestMap = new HashMap<String, TransactionRequest>();

    def messageSource

    public void putTransactionRequest(String sessionId, TransactionRequest transactionRequest) {
        log.debug("putTransactionRequest - sessionId $sessionId")
        transactionRequestMap.put(sessionId, transactionRequest)
    }

    public TransactionRequest getTransactionRequest(String sessionId) {
        transactionRequestMap.get(sessionId)
    }

    public Set<String> getSessionKeys() {
        return transactionRequestMap.keySet()
    }

    public void bindContext(String sessionId, AsyncContext ctx) {
        transactionRequestMap.get(sessionId).setAsyncContext(ctx)
    }

    public ResponseVS sendResponse(String sessionId, SMIMEMessage smimeMessage) {
        log.debug("sendResponse")
        TransactionRequest transactionRequest = transactionRequestMap.remove(sessionId)
        if(transactionRequest) {
            ResponseVS responseVS = null;
            SignedReceiptContent receiptContent = new SignedReceiptContent(smimeMessage.getSignedContent())
            try {
                receiptContent.transactionRequest.checkRequest(transactionRequest)
                JSONObject responseJSON = [status:ResponseVS.SC_OK, message:receiptContent.getMessage()]
                responseVS = new ResponseVS(ResponseVS.SC_OK, receiptContent.getMessage())
                transactionRequest.getAsyncContext().response.getWriter().write(responseJSON.toString())
                transactionRequest.getAsyncContext().complete()
            } catch(ExceptionVS ex)  {
                log.error(ex.getMessage())
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage())
                JSONObject responseJSON = [status:ResponseVS.SC_ERROR, message:ex.getMessage()]
                transactionRequest.getAsyncContext().response.getWriter().write(responseJSON.toString())
                transactionRequest.getAsyncContext().complete()
            } finally {
                return responseVS
            }
        } else {
            log.debug("sendResponse - transactionRequest with sessionId $sessionId has expired  ");
            return new ResponseVS(ResponseVS.SC_ERROR, "sessionId has expired")
        }

    }

    private class SignedReceiptContent {

        TransactionRequest transactionRequest;

        public SignedReceiptContent(String signedContent) throws ExceptionVS {
            transactionRequest = TransactionRequest.parse(JSONSerializer.toJSON(signedContent))
        }

        public String getMessage() {
            messageSource.getMessage('transactionRequestOKMsg', [transactionRequest.getAmount().toString(),
                    transactionRequest.getCurrencyCode()].toArray(), locale)
        }
    }

}
