package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.TransactionVS

@Transactional
class TransactionVS_BankVSService {

    def messageSource
    def systemService

    @Transactional
    private ResponseVS processTransactionVS(MessageSMIME messageSMIMEReq, JSONObject messageJSON) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        BankVSTransactionVSRequest request = new BankVSTransactionVSRequest(messageJSON)
        BankVS signer = BankVS.findWhere(nif:messageSMIMEReq.userVS.nif)
        if(!(signer)) {
            throw new ExceptionVS(messageSource.getMessage('bankVSPrivilegesErrorMsg',
                    [request.operation.toString()].toArray(), LocaleContextHolder.locale),
                    MetaInfMsg.getErrorMsg(methodName, "bankVSPrivilegesError"))
        }
        TransactionVS transactionParent = new TransactionVS(amount: request.amount, messageSMIME:messageSMIMEReq,
                fromUserVS:signer, fromUserIBAN: request.fromUserIBAN, fromUser:request.fromUser,
                state:TransactionVS.State.OK, validTo:request.validTo, subject:request.subject, currencyCode:
                request.currencyCode, type:TransactionVS.Type.FROM_BANKVS,  tag:request.tag).save()
        TransactionVS triggeredTransaction = TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.amount, request.toUser, request.toUser.IBAN).save();
        //transaction?.errors.each { log.error("processTransactionVSFromBankVS - error - ${it}")}
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${triggeredTransaction.id}")
        log.debug("${metaInfMsg} - from BankVS '${signer.id}' to userVS '${request.toUser.id}' ")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", metaInf:metaInfMsg,
                type:TypeVS.FROM_BANKVS)
    }

    private class BankVSTransactionVSRequest {
        Boolean isTimeLimited;
        BigDecimal amount;
        UserVS toUser;
        TagVS tag;
        String currencyCode, fromUserIBAN, fromUser, subject;
        TypeVS operation;
        Date validTo;
        public BankVSTransactionVSRequest(JSONObject messageJSON) {
            if(TypeVS.FROM_BANKVS != TypeVS.valueOf(messageJSON.operation)) throw ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'FROM_BANKVS' - operation found: " + messageJSON.operation)
            if(messageJSON.toUserIBAN.length() != 1) throw new ExceptionVS(
                    "There can be only one receptor. request.toUserIBAN -> ${messageJSON.toUserIBAN} ")
            toUser = UserVS.findWhere(IBAN:messageJSON.toUserIBAN.get(0))
            if(!toUser) throw new ValidationExceptionVS(this.getClass(), "invalid 'toUserIBAN': '${messageJSON.toUserIBAN}'");
            if(!messageJSON.amount)  throw new ValidationExceptionVS(this.getClass(), "missing param 'amount'");
            amount = new BigDecimal(messageJSON.amount)
            currencyCode = messageJSON.currencyCode
            if(!currencyCode)  throw new ValidationExceptionVS(this.getClass(), "missing param 'currencyCode'");
            fromUserIBAN =  messageJSON.fromUserIBAN
            if(!fromUserIBAN)  throw new ValidationExceptionVS(this.getClass(), "missing param 'fromUserIBAN'");
            fromUser = messageJSON.fromUser
            if(!fromUser)  throw new ValidationExceptionVS(this.getClass(), "missing param 'fromUser'");
            subject = messageJSON.subject
            isTimeLimited = messageJSON.isTimeLimited
            if(isTimeLimited) validTo = DateUtils.getCurrentWeekPeriod().dateTo
            if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
                tag = TagVS.findWhere(name:messageJSON.tags[0])
                if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0]}'")
            } else throw new Exception("Invalid number of tags: '${messageJSON.tags}'")
        }
    }

}
