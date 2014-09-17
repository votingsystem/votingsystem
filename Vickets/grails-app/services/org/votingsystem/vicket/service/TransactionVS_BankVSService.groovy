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

    def messageSource
    def systemService

    @Transactional
    private ResponseVS processDeposit(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS messageSigner = messageSMIMEReq.userVS
        String msg;
        UserVS toUser = UserVS.findWhere(IBAN:messageJSON.toUserIBAN)
        //[ fromUser:Implantación proyecto Vickets, fromUserIBAN:ES1877777777450000000050, subject:Ingreso viernes 25, toUserIBAN:ES8378788989450000000015,UUID:693dc9c4-f01c-443d-8934-7eeed0ce582b, operation:VICKET_DEPOSIT_FROM_VICKET_SOURCE, tags:[[name:HIDROGENO]], validTo:2014/07/28 00:00:00]
        if (!messageJSON.amount || !messageJSON.currency || !toUser || ! messageJSON.fromUserIBAN ||
                !messageJSON.fromUser|| (TypeVS.VICKET_DEPOSIT_FROM_BANKVS != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        log.debug("${methodName} - signer: '${messageSigner.nif}'")
        BankVS signer = BankVS.findWhere(nif:messageSigner.nif)
        if(!(signer)) {
            msg = messageSource.getMessage('bankVSPrivilegesErrorMsg', [messageJSON.operation].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_DEPOSIT_ERROR)
        }
        Calendar weekFromCalendar = DateUtils.getMonday(Calendar.getInstance())
        Calendar weekToCalendar = weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)

        Currency currency = Currency.getInstance(messageJSON.currency)
        String subject = messageJSON.subject
        BigDecimal amount = new BigDecimal(messageJSON.amount)

        Date validTo =  DateUtils.getDateFromString(messageJSON.validTo)
        if(validTo.before(Calendar.getInstance().getTime())) {
            throw new Exception(messageSource.getMessage('depositDateRangeERRORMsg', [messageJSON.validTo].toArray(), locale))
        }

        VicketTagVS tag
        if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
            tag = VicketTagVS.findWhere(name:messageJSON.tags[0].name)
            if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0].name}'")
        } else throw new Exception("Invalid number of tags: '${messageJSON.tags}'")

        TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                fromUserVS:signer, fromUserIBAN: messageJSON.fromUserIBAN, fromUser:messageJSON.fromUser,
                state:TransactionVS.State.OK, validTo:validTo,
                subject:subject, currencyCode: currency.getCurrencyCode(),
                type:TransactionVS.Type.BANKVS_INPUT,  tag:tag).save()

        TransactionVS triggeredTransaction = TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.amount, toUser, messageJSON.toUserIBAN).save();

        //transaction?.errors.each { log.error("processDepositFromBankVS - error - ${it}")}

        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${triggeredTransaction.id}")
        log.debug("${metaInfMsg} - from BankVS '${signer.id}' to userVS '${toUser.id}' ")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", metaInf:metaInfMsg,
                type:TypeVS.VICKET_DEPOSIT_FROM_BANKVS)
    }

}
