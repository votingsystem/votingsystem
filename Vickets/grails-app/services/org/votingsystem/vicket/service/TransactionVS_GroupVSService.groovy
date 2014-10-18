package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount

import java.math.RoundingMode

@Transactional
class TransactionVS_GroupVSService {

    private static final CLASS_NAME = TransactionVS_GroupVSService.class.getSimpleName()

    def walletVSService
    def messageSource
    def systemService
    def signatureVSService

    @Transactional
    private ResponseVS processTransactionVS(MessageSMIME messageSMIMEReq, JSONObject messageJSON) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        GroupVSTransactionVSRequest request = new GroupVSTransactionVSRequest(messageJSON, messageSMIMEReq.userVS)
        String msg
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements = walletVSService.getAccountMovementsForTransaction(
                request.groupVS.IBAN, request.tag, request.amount, request.currencyCode)
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) {
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    reason:accountFromMovements.getMessage(), message:accountFromMovements.getMessage(),
                    metaInf: MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "lowBalance"))
        }
        if(request.operation == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionVSForAllMembers(messageSMIMEReq, request, accountFromMovements.data)
        } else {
            BigDecimal numUsersBigDecimal = new BigDecimal(request.toUserVSList.size())
            BigDecimal userPart = request.amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)
            String metaInfMsg
            if(request.operation == TransactionVS.Type.FROM_GROUP_TO_MEMBER) {
                msg = messageSource.getMessage('transactionVSFromGroupToMemberOKMsg',
                        ["${request.amount} ${request.currencyCode}", request.toUserVSList.iterator().next().nif].toArray(),
                        locale)
            } else if (request.operation == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                msg = messageSource.getMessage('transactionVSFromGroupToMemberGroupOKMsg',
                        ["${request.amount} ${request.currencyCode}"].toArray(), locale)
            }
            TransactionVS transactionParent = new TransactionVS(amount: messageJSON.amount, messageSMIME:messageSMIMEReq,
                    fromUserVS:request.groupVS, fromUserIBAN: request.groupVS.IBAN, state:TransactionVS.State.OK,
                    validTo: request.validTo, subject:request.subject, type:request.operation,
                    accountFromMovements: accountFromMovements.data, currencyCode: request.currencyCode,
                    tag:request.tag).save()
            for(UserVS toUser: request.toUserVSList) {
                TransactionVS transaction = TransactionVS.generateTriggeredTransaction(
                        transactionParent, userPart,toUser, toUser.IBAN).save()
                metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}_${request.operation.toString()}")
                log.debug("${metaInfMsg} - ${userPart} ${request.currencyCode} - from group '${request.groupVS.name}' " +
                        "to userVS '${toUser.id}' ")
            }
            metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transactionParent.id}_${request.operation.toString()}")
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg,
                    type:TypeVS.valueOf(request.operation.toString()))
        }
    }

    @Transactional
    private UserVS getUserFromGroup (GroupVS groupVS, String IBAN) {
        List subscriptionList = SubscriptionVS.createCriteria().list(offset: 0) {
            eq("groupVS", groupVS)
            eq("state", SubscriptionVS.State.ACTIVE)
            userVS { eq("IBAN", IBAN)}
        }
        if(subscriptionList.isEmpty()) return null
        else return subscriptionList?.iterator()?.next().userVS
    }

    @Transactional
    private ResponseVS processTransactionVSForAllMembers(MessageSMIME messageSMIMEReq,
            GroupVSTransactionVSRequest request, Map<UserVSAccount, BigDecimal> accountFromMovements) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        def subscriptionList = SubscriptionVS.createCriteria().list(offset: 0) {
            eq("groupVS", request.groupVS)
            eq("state", SubscriptionVS.State.ACTIVE)
        }
        BigDecimal numUsersBigDecimal = new BigDecimal(subscriptionList.totalCount)
        BigDecimal userPart = request.amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)
        TransactionVS.Type transactionVSType = TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS
        String msg = messageSource.getMessage('transactionVSFromGroupToAllMembersGroupOKMsg',
                ["${request.amount.toString()} ${request.currencyCode}"].toArray(), locale)
        TransactionVS transactionParent = new TransactionVS(amount: request.amount, messageSMIME:messageSMIMEReq,
                fromUserVS:request.groupVS, fromUserIBAN: request.groupVS.IBAN, state:TransactionVS.State.OK,
                validTo: request.validTo, subject:request.subject, currencyCode: request.currencyCode,
                type:transactionVSType, tag:request.tag, accountFromMovements: accountFromMovements).save()
        subscriptionList.each { it ->
            JSONObject messageJSON = request.getReceptorData(messageSMIMEReq.id, it.userVS.getNif(),
                    subscriptionList.totalCount, userPart)
            SMIMEMessage receipt = signatureVSService.getSMIMEMessage(systemService.getSystemUser().getNif(),
                    it.userVS.getNif(), messageJSON.toString(), request.operation.toString(), null)
            MessageSMIME messageSMIMEReceipt = new MessageSMIME(smimeParent:messageSMIMEReq,
                    type:TypeVS.FROM_GROUP_TO_ALL_MEMBERS, content:receipt.getBytes()).save()
            TransactionVS.generateTriggeredTransaction(transactionParent, userPart, it.userVS, it.userVS.IBAN).save()
        }
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName,
                "transactionVS_${transactionParent.id}_${request.operation.toString()}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg,
                type:TypeVS.valueOf(request.operation.toString()))
    }

    private class GroupVSTransactionVSRequest {
        Boolean isTimeLimited;
        BigDecimal amount;
        UserVS signer;
        List<UserVS> toUserVSList = []
        GroupVS groupVS
        TagVS tag;
        String currencyCode, fromUser, subject;
        TransactionVS.Type operation;
        Date validTo;
        JSONObject messageJSON;
        public GroupVSTransactionVSRequest(JSONObject messageJSON, UserVS messageSigner) {
            if(!messageJSON.operation) throw new ValidationExceptionVS(this.getClass(), "missing param 'operation'");
            operation = TransactionVS.Type.valueOf(messageJSON.operation)
            this.messageJSON = messageJSON
            signer = messageSigner
            groupVS = GroupVS.findWhere(IBAN:messageJSON.fromUserIBAN, representative:messageSigner)
            if(!groupVS) {
                throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage('groupNotFoundByIBANErrorMsg',
                        [messageJSON.fromUserIBAN, messageSigner.nif].toArray(), locale))
            }
            if(!messageJSON.amount)  throw new ValidationExceptionVS(this.getClass(), "missing param 'amount'");
            amount = new BigDecimal(messageJSON.amount)
            currencyCode = messageJSON.currencyCode
            if(!currencyCode)  throw new ValidationExceptionVS(this.getClass(), "missing param 'currencyCode'");
            subject = messageJSON.subject
            if(!subject)  throw new ValidationExceptionVS(this.getClass(), "missing param 'subject'");
            isTimeLimited = messageJSON.isTimeLimited
            if(isTimeLimited) validTo = DateUtils.getCurrentWeekPeriod().dateTo
            if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
                tag = TagVS.findWhere(name:messageJSON.tags[0])
                if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0]}'")
            } else throw new Exception("Invalid number of tags: '${messageJSON.tags}'")
            if(operation != TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
                messageJSON.toUserIBAN?.each { it ->
                    UserVS userVS = getUserFromGroup(groupVS, it)
                    if(!userVS) {
                        throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage(
                                'groupUserNotFoundByIBANErrorMsg',  [it, groupVS.name].toArray(), locale))
                    } else toUserVSList.add(userVS)
                }
                if(toUserVSList.isEmpty()) throw new ValidationExceptionVS(this.getClass(),
                        "Transaction without valid receptors")
            }
        }

        JSONObject getReceptorData(Long messageSMIMEReqId, String toUserNif, int numReceptors, BigDecimal userPart) {
            messageJSON.messageSMIMEParentId = messageSMIMEReqId? messageSMIMEReqId : null
            messageJSON.toUser = toUserNif? toUserNif : null
            messageJSON.numUsers = numReceptors?numReceptors:null
            messageJSON.toUserAmount = userPart?userPart.toString():null
            return messageJSON;
        }

    }

}