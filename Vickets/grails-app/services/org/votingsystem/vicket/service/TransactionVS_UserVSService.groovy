package org.votingsystem.vicket.service

import org.votingsystem.model.ResponseVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

//@Transactional
class TransactionVS_UserVSService {

    def walletVSService
    def messageSource
    def systemService
    def signatureVSService

  //  @Transactional
    private ResponseVS processTransactionVS(TransactionVSService.TransactionVSRequest request) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements = walletVSService.getAccountMovementsForTransaction(
                request.fromUserVS.IBAN, request.tag, request.amount, request.currencyCode)
        signatureVSService.getSMIMEMultiSigned()
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) throw new ValidationExceptionVS(this.getClass(),
                accountFromMovements.getMessage(), MetaInfMsg.getErrorMsg(methodName, "lowBalance"))
        TransactionVS transactionParent = new TransactionVS(amount:request.amount, messageSMIME:request.messageSMIME,
                fromUserVS:request.fromUserVS, state:TransactionVS.State.OK, validTo: request.validTo,
                subject:request.subject, type:TransactionVS.Type.FROM_USERVS, currencyCode: request.getCurrencyCode(),
                accountFromMovements: accountFromMovements.data, tag:request.tag).save()
        TransactionVS transaction = TransactionVS.generateTriggeredTransaction(
                transactionParent, request.amount, request.toUserVS,  request.toUserVS.IBAN).save()
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}_${request.operation.toString()}")
        log.debug(metaInfMsg)
        String msg = messageSource.getMessage('transactionVSFromUserVSOKMsg',
                ["${request.amount} ${request.currencyCode}", request.toUserVS.name].toArray(), locale)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:request.operation)
    }


}
