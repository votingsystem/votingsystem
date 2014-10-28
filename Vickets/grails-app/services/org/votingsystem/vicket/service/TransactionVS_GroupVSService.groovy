package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount

import java.math.RoundingMode

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class TransactionVS_GroupVSService {

    def walletVSService
    def messageSource
    def systemService
    def signatureVSService

    @Transactional
    private ResponseVS processTransactionVS(TransactionVSService.TransactionVSRequest request) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        String msg
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements = walletVSService.getAccountMovementsForTransaction(
                request.groupVS.IBAN, request.tag, request.amount, request.currencyCode)
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) throw new ValidationExceptionVS(this.getClass(),
                accountFromMovements.getMessage(), MetaInfMsg.getErrorMsg(methodName, "lowBalance"))

        if(request.operation == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            return processTransactionVSForAllMembers(request, accountFromMovements.data)
        } else {
            BigDecimal numUsersBigDecimal = new BigDecimal(request.toUserVSList.size())
            BigDecimal userPart = request.amount.divide(numUsersBigDecimal, 4, RoundingMode.FLOOR)
            String metaInfMsg
            if(request.operation == TransactionVS.Type.FROM_GROUP_TO_MEMBER) {
                msg = messageSource.getMessage('transactionVSFromGroupToMemberOKMsg',
                        ["${request.amount} ${request.currencyCode}", request.toUserVSList.iterator().next().nif].toArray(),
                        locale)
            } else if (request.operation == TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP) {
                msg = messageSource.getMessage('transactionVSFromGroupToMemberGroupOKMsg',
                        ["${request.amount} ${request.currencyCode}"].toArray(), locale)
            }
            TransactionVS transactionParent = new TransactionVS(amount: request.amount, messageSMIME:messageSMIMEReq,
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
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:request.operation)
        }
    }

    @Transactional
    private ResponseVS processTransactionVSForAllMembers(TransactionVSService.TransactionVSRequest request,
             Map<UserVSAccount, BigDecimal> accountFromMovements) {
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
        TransactionVS transactionParent = new TransactionVS(amount: request.amount, messageSMIME:request.messageSMIME,
                fromUserVS:request.groupVS, fromUserIBAN: request.groupVS.IBAN, state:TransactionVS.State.OK,
                validTo: request.validTo, subject:request.subject, currencyCode: request.currencyCode,
                type:transactionVSType, tag:request.tag, accountFromMovements: accountFromMovements).save()
        subscriptionList.each { it ->
            JSONObject messageJSON = request.getReceptorData(request.messageSMIME.id, it.userVS.getNif(),
                    subscriptionList.totalCount, userPart)
            SMIMEMessage receipt = signatureVSService.getSMIME(systemService.getSystemUser().getNif(),
                    it.userVS.getNif(), messageJSON.toString(), request.operation.toString(), null)
            MessageSMIME messageSMIMEReceipt = new MessageSMIME(smimeParent:request.messageSMIME,
                    type:TypeVS.FROM_GROUP_TO_ALL_MEMBERS, content:receipt.getBytes()).save()
            TransactionVS.generateTriggeredTransaction(transactionParent, userPart, it.userVS, it.userVS.IBAN).save()
        }
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName,
                "transactionVS_${transactionParent.id}_${request.operation.toString()}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg,
                type:TypeVS.valueOf(request.operation.toString()))
    }

}