package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.util.MetaInfMsg

@Transactional
class TransactionVS_BankVSService {

    private static final CLASS_NAME = TransactionVS_BankVSService.class.getSimpleName()

    def messageSource
    def systemService

    @Transactional
    private ResponseVS processDeposit(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS messageSigner = messageSMIMEReq.userVS
        String msg;
        UserVS toUser = UserVS.findWhere(IBAN:messageJSON.toUserIBAN)
        if (!messageJSON.amount || !messageJSON.currency || !toUser || ! messageJSON.fromUserIBAN ||
                !messageJSON.fromUser|| (TypeVS.VICKET_DEPOSIT_FROM_BANKVS != TypeVS.valueOf(messageJSON.operation))) {
            if(!toUser) msg = messageSource.getMessage('userNotFoundForIBANErrorMsg', [messageJSON.toUserIBAN].toArray(), locale)
            else msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        log.debug("${methodName} - signer: '${messageSigner.nif}'")
        BankVS signer = BankVS.findWhere(nif:messageSigner.nif)
        if(!(signer)) {
            msg = messageSource.getMessage('bankVSPrivilegesErrorMsg', [messageJSON.operation].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, reason:msg,
                    type:TypeVS.ERROR,metaInf: MetaInfMsg.getErrorMsg(methodName, "bankVSPrivilegesError") )
        }

        Currency currency = Currency.getInstance(messageJSON.currency)
        String subject = messageJSON.subject
        BigDecimal amount = new BigDecimal(messageJSON.amount)

        Date validTo = null
        if(messageJSON.isTimeLimited == true) validTo = DateUtils.getCurrentWeekPeriod().dateTo

        VicketTagVS tag
        if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
            tag = VicketTagVS.findWhere(name:messageJSON.tags[0].name)
            if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0].name}'")
        } else throw new Exception("Invalid number of tags: '${messageJSON.tags}'")

        TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                fromUserVS:signer, fromUserIBAN: messageJSON.fromUserIBAN, fromUser:messageJSON.fromUser,
                state:TransactionVS.State.OK, validTo:validTo, subject:subject, currencyCode: currency.getCurrencyCode(),
                type:TransactionVS.Type.FROM_BANKVS,  tag:tag).save()

        TransactionVS triggeredTransaction = TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.amount, toUser, messageJSON.toUserIBAN).save();

        //transaction?.errors.each { log.error("processDepositFromBankVS - error - ${it}")}

        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${triggeredTransaction.id}")
        log.debug("${metaInfMsg} - from BankVS '${signer.id}' to userVS '${toUser.id}' ")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", metaInf:metaInfMsg,
                type:TypeVS.VICKET_DEPOSIT_FROM_BANKVS)
    }

}
