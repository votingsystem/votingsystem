package org.votingsystem.cooin.service

import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.cooin.model.UserVSAccount

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

//@Transactional
class TransactionVS_UserVSService {

    def walletVSService
    def messageSource
    def systemService
    def signatureVSService
    def grailsApplication

  //@Transactional
    private ResponseVS processTransactionVS(TransactionVSService.TransactionVSRequest request) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        Map<UserVSAccount, BigDecimal> accountFromMovements = walletVSService.getAccountMovementsForTransaction(
                request.fromUserVS.IBAN, request.tag, request.amount, request.currencyCode)
        //Transactions from users doesn't need parent transaction
        TransactionVS transactionVS = new TransactionVS(amount:request.amount, messageSMIME:request.messageSMIME,
                toUserVS:request.toUserVS, toUserIBAN:request.toUserVS.IBAN,
                fromUserVS:request.fromUserVS, state:TransactionVS.State.OK, validTo: request.validTo,
                subject:request.subject, type:request.transactionType, currencyCode: request.getCurrencyCode(),
                accountFromMovements: accountFromMovements, tag:request.tag).save()
        String fromUser = grailsApplication.config.mail.error.to
        String toUser = request.fromUserVS.getNif()
        SMIMEMessage receipt = signatureVSService.getSMIMEMultiSigned(fromUser, toUser,
                request.messageSMIME.getSMIME(), request.messageSMIME.getSMIME().subject)
        request.messageSMIME.setSMIME(receipt)
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transactionVS.id}_${request.operation.toString()}")
        log.debug(metaInfMsg)
        String msg = messageSource.getMessage('transactionVSFromUserVSOKMsg',
                ["${request.amount} ${request.currencyCode}", request.toUserVS.name].toArray(), locale)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:request.operation)
    }


}
