package org.votingsystem.vicket.service

import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketTagVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount

//@Transactional
class TransactionVS_UserVSService {

    private static final CLASS_NAME = TransactionVS_UserVSService.class.getSimpleName()

    def walletVSService
    def messageSource
    def systemService
    def signatureVSService


  //  @Transactional
    private ResponseVS processTransactionVS(MessageSMIME messageSMIMEReq, JSONObject messageJSON) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS fromUserVS = messageSMIMEReq.userVS
        UserVS toUserVS
        log.debug("====== $methodName - messageJSON: $messageJSON")
        //Validate request
        ResponseVS responseVS
        String msg
        if(!messageJSON.fromUserIBAN?.equals(fromUserVS.IBAN)) throw new ExceptionVS("User '${fromUserVS.nif}' doesn't" +
                " have an account with IBAN '${messageJSON.fromUserIBAN}'")
        TypeVS operationType = TypeVS.valueOf(messageJSON.operation)
        Currency currency = Currency.getInstance(messageJSON.currency)
        BigDecimal transactionVSAmount = new BigDecimal(messageJSON.amount)
        if(messageJSON.toUserIBAN?.size() != 1) throw new ExceptionVS("'TRANSACTIONVS_FROM_USERVS' can only have one " +
                "receptor and request has '${messageJSON.toUserIBAN}'")
        toUserVS = UserVS.findWhere(IBAN:messageJSON.toUserIBAN[0])
        if(!toUserVS) throw new ExceptionVS("User not found for IBAN '${messageJSON.toUserIBAN[0]}'")
        if (!transactionVSAmount || !currency  || !messageJSON.subject || operationType != TypeVS.TRANSACTIONVS_FROM_USERVS) {
            msg = messageSource.getMessage('paramsErrorMsg', null, LocaleContextHolder.getLocale())
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "params"))
        }
        Date validTo = null
        if(messageJSON.isTimeLimited == true) validTo = DateUtils.getCurrentWeekPeriod().dateTo

        VicketTagVS tag
        if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
            tag = VicketTagVS.findWhere(name:messageJSON.tags[0])
            if(!tag) throw new ExceptionVS("Unknown tag '${messageJSON.tags[0]}'")
        } else throw new ExceptionVS("Invalid number of tags: '${messageJSON.tags}'")
        //Check cash available for user
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                walletVSService.getAccountMovementsForTransaction(
                messageJSON.fromUserIBAN, tag, transactionVSAmount, messageJSON.currency)
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) {
            log.error "${methodName} - ${accountFromMovements.getMessage()}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    reason:accountFromMovements.getMessage(), message:accountFromMovements.getMessage(),
                    metaInf: MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "lowBalance"))
        }

        String metaInfMsg
        TransactionVS.Type transactionVSType = TransactionVS.Type.FROM_USERVS
        TransactionVS transactionParent = new TransactionVS(amount: transactionVSAmount, messageSMIME:messageSMIMEReq,
                fromUserVS:fromUserVS, fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK,
                validTo: validTo, subject:messageJSON.subject, type:transactionVSType,
                accountFromMovements: accountFromMovements.data, currencyCode: currency.getCurrencyCode(), tag:tag).save()
        TransactionVS transaction = TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionVSAmount, toUserVS, toUserVS.IBAN).save()
        metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}_${operationType.toString()}")
        log.debug("${metaInfMsg} - ${transactionVSAmount} ${messageJSON.currency} - from group '${fromUserVS.id}' to userVS '${toUserVS.id}' ")
        metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transactionParent.id}_${operationType.toString()}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:operationType)
    }

}
