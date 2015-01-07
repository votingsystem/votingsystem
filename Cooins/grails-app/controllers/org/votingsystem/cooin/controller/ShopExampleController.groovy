package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.votingsystem.cooin.Payment
import org.votingsystem.cooin.TransactionRequest
import org.votingsystem.model.ResponseVS

class ShopExampleController {

    def shopExampleService
    def grailsLinkGenerator;

    def index() {
        String randomUUID = UUID.randomUUID().toString();
        TransactionRequest transactionRequest = new TransactionRequest(type: TransactionRequest.Type.PAYMENT_REQUEST,
                subject: "ShopExampleController", toUser:"GroupVS From TESTS - lun 08 dic 17:13",
                amount: new BigDecimal(100), date:Calendar.getInstance().getTime(), currency: "EUR",
                IBAN: "ES8978788989450000000004", UUID: randomUUID)
        transactionRequest.setPaymentOptions(Arrays.asList(Payment.SIGNED_TRANSACTION,
                Payment.ANONYMOUS_SIGNED_TRANSACTION, Payment.COOIN_SEND))
        String serviceURLParam = randomUUID.substring(0, 8)
        String paymentInfoServiceURL = grailsLinkGenerator.link(controller: 'shop', absolute:true) + "/${serviceURLParam}"
        shopExampleService.putTransactionRequest(serviceURLParam, transactionRequest)
        render(view:'index', model:[paymentInfoServiceURL:paymentInfoServiceURL, transactionRequest:transactionRequest])
    }

    def paymentInfo() {
        TransactionRequest transactionRequest = shopExampleService.getTransactionRequest(params.uuid)
        if(transactionRequest) {
            render transactionRequest.toJSON() as JSON
        } else {
            response.status = ResponseVS.SC_NOT_FOUND
            render "transactionRequest not found - keySet: ${shopExampleService.keySet()}"
        }
    }

    def payment() {

    }

}