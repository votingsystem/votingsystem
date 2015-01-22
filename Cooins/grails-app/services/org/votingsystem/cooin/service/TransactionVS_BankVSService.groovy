package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.model.BankVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.MetaInfMsg

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class TransactionVS_BankVSService {

    def messageSource
    def systemService

    private ResponseVS processTransactionVS(TransactionVSService.TransactionVSRequest request) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        BankVS bankVS = BankVS.findWhere(nif:request.fromUserVS.nif)
        if(!(bankVS)) throw new ExceptionVS(messageSource.getMessage('bankVSPrivilegesErrorMsg',
                    [request.operation.toString()].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "bankVSPrivilegesError"))
        TransactionVS transactionParent = new TransactionVS(amount: request.amount, messageSMIME:request.messageSMIME,
                fromUserVS:bankVS, fromUserIBAN: request.fromUserIBAN, fromUser:request.fromUser,
                state:TransactionVS.State.OK, validTo:request.validTo, subject:request.subject, currencyCode:
                request.currencyCode, type:TransactionVS.Type.FROM_BANKVS,  tag:request.tag).save()
        TransactionVS triggeredTransaction = TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.amount, request.toUserVS, request.toUserVS.IBAN).save();
        //transaction?.errors.each { log.error("processTransactionVSFromBankVS - error - ${it}")}
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${triggeredTransaction.id}")
        log.debug("${metaInfMsg} - from BankVS '${bankVS.id}' to userVS '${request.toUserVS.id}' ")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", metaInf:metaInfMsg,
                type:TypeVS.FROM_BANKVS)
    }

}
