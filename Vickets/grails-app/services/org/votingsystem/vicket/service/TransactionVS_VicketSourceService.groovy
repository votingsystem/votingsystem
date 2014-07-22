package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.util.MetaInfMsg

@Transactional
class TransactionVS_VicketSourceService {

    def messageSource
    def systemService

    @Transactional
    private ResponseVS processDeposit(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS messageSigner = messageSMIMEReq.userVS
        String msg;
        UserVS toUser = UserVS.findWhere(IBAN:messageJSON.toUserIBAN)
        if (!messageJSON.amount || !messageJSON.currency || !messageJSON.toUserIBAN || !toUser || ! messageJSON.fromUserIBAN ||
                !messageJSON.fromUser|| (TypeVS.VICKET_DEPOSIT_FROM_VICKET_SOURCE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        log.debug("${methodName} - signer: '${messageSigner.nif}'")
        VicketSource signer = VicketSource.findWhere(nif:messageSigner.nif)
        if(!(signer)) {
            msg = messageSource.getMessage('vicketSourcePrivilegesErrorMsg', [messageJSON.operation].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_DEPOSIT_ERROR)
        }
        Calendar weekFromCalendar = DateUtils.getMonday(Calendar.getInstance())
        Calendar weekToCalendar = weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)

        Currency currency = Currency.getInstance(messageJSON.currency)
        String subject = messageJSON.subject
        BigDecimal amount = new BigDecimal(messageJSON.amount)

        Date validTo =  DateUtils.getDateFromString(messageJSON.validTo)
        if(!validTo.after(Calendar.getInstance().getTime())) {
            throw new Exception(messageSource.getMessage('depositDateRangeERRORMsg', [messageJSON.validTo].toArray(), locale))
        }

        VicketTagVS tag
        if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
            tag = VicketTagVS.findWhere(id:Long.valueOf(messageJSON.tags[0].id), name:messageJSON.tags[0].name)
            if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0].name}'")
        } else throw new Exception("Invalid number of tags: '${messageJSON.tags}'")

        UserVS systemUser = systemService.getSystemUser()
        TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo:validTo,
                subject:subject, fromUserVS:signer, currencyCode: currency.getCurrencyCode(),
                type:TransactionVS.Type.VICKET_SOURCE_INPUT, toUserIBAN:systemUser.getIBAN(), toUserVS: systemUser,
                tag:tag).save()

        TransactionVS transaction = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                fromUserIBAN: systemUser.IBAN, toUserIBAN:messageJSON.toUserIBAN,
                state:TransactionVS.State.OK, validTo:validTo, subject:subject, fromUserVS:systemUser, toUserVS: toUser,
                transactionParent: transactionParent, currencyCode: currency.getCurrencyCode(),
                type:TransactionVS.Type.VICKET_SOURCE_INPUT, tag:tag).save()
        //transaction?.errors.each { log.error("processDepositFromVicketSource - error - ${it}")}

        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}")
        log.debug("${metaInfMsg} - from VicketSource '${signer.id}' to userVS '${toUser.id}' ")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", metaInf:metaInfMsg,
                type:TypeVS.VICKET_DEPOSIT_FROM_VICKET_SOURCE)
    }

}
