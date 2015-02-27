package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.cooin.model.Payment
import org.votingsystem.cooin.TransactionRequest
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage

import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener

/**
 * Controller to show how to add this payment system to your website. Requirements:
 * - The web site must have an active account as a 'group' on the Cooin system
 * - The web site doesn't need to be built with grails but it has to provide the mechanism to show the QR code, fetch the
 * transaction data, process the payment receipt and notify the user
 */
class ShopExampleController {

    public static final int SESSION_TIMEOUT = 180000; //3 minutes

    def shopExampleService
    def grailsLinkGenerator;

    //After user interaction we have the data of the service the user wants to buy, with that we create a TransactionRequest
    //and show the QR code with the URL of the transaction data to offer the user the possibility to check the order with the mobile.
    def index() {
        TransactionRequest transactionRequest = new TransactionRequest(type: TypeVS.PAYMENT_REQUEST,
                userToType: UserVS.Type.GROUP, subject: "shop example payment - ${Calendar.getInstance().getTime()}",
                toUserName:"cooin shop example", amount: new BigDecimal(1), currencyCode: "EUR", tagVS:TagVS.WILDTAG,
                date:Calendar.getInstance().getTime(), IBAN: "ES0878788989450000000007",
                UUID:java.util.UUID.randomUUID().toString())
        transactionRequest.setPaymentOptions(Arrays.asList(Payment.SIGNED_TRANSACTION,
                Payment.ANONYMOUS_SIGNED_TRANSACTION, Payment.CASH_SEND))
        String shopSessionID = transactionRequest.getUUID().substring(0, 8)
        String paymentInfoServiceURL = grailsLinkGenerator.link(controller: 'shop', absolute:true) + "/${shopSessionID}"
        shopExampleService.putTransactionRequest(shopSessionID, transactionRequest)
        render(view:'index', model:[paymentInfoServiceURL:paymentInfoServiceURL, transactionRequest:transactionRequest,
                shopSessionID:shopSessionID])
    }

    //Called with async Javascript from the web page that shows the QR code, we store an AsyncContext in order to notify
    //the web client of any change in the requested transaction state
    def listenTransactionChanges() {
        final AsyncContext ctx = startAsync()
        ctx.addListener(new AsyncListener() {
            @Override public void onComplete(AsyncEvent event) throws IOException {}
            @Override public void onTimeout(AsyncEvent event) throws IOException {
                log.debug("On timeout");
                ctx.response.getWriter().write("session expired")
                ctx.response.getWriter().flush()
            }
            @Override public void onError(AsyncEvent event) throws IOException { }
            @Override public void onStartAsync(AsyncEvent event) throws IOException { }
        });
        ctx.setTimeout(SESSION_TIMEOUT);
        shopExampleService.bindContext(params.shopSessionID, ctx)
        ctx.start { }
    }

    //Called from the mobile after reading the QR code. The mobile fetch transaction data with the payment data
    def paymentInfo() {
        TransactionRequest transactionRequest = shopExampleService.getTransactionRequest(params.uuid)
        if(transactionRequest) {
            render transactionRequest.toJSON() as JSON
        } else {
            response.status = ResponseVS.SC_NOT_FOUND
            render message(code:'sessionExpiredMsg')
        }
    }

    //Called from the mobile after the payment. The mobile sends the signed receipt with the completed transaction data.
    //here you must check with the tools of your choice the validity of the receipt. The receipt is an standard
    //S/MIME document (http://en.wikipedia.org/wiki/S/MIME)
    def payment() {
        SMIMEMessage smimeMessage = new SMIMEMessage(request.getInputStream())
        ResponseVS responseVS = null;
        if (smimeMessage?.isValidSignature()) { //check with your tools if it's signed with valid certificates!!!
            responseVS = shopExampleService.sendResponse(params.uuid, smimeMessage)
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR, "invalid receipt")
        response.status = responseVS.getStatusCode()
        render responseVS.getMessage()
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}