package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.UserVSAccount
import org.votingsystem.model.VicketTagVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.util.MetaInfMsg

import java.math.RoundingMode

@Transactional
class TransactionVS_GroupVSService {

    def walletVSService
    def messageSource
    def systemService
    def signatureVSService

    @Transactional
    private ResponseVS processDeposit(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS messageSigner = messageSMIMEReq.userVS
        List<UserVS> receptorList = []
        GroupVS groupVS = GroupVS.findWhere(IBAN:messageJSON.fromUserIBAN, representative:messageSigner)
        String msg;
        if(!groupVS) {
            msg = messageSource.getMessage('groupNotFoundByIBANErrorMsg',
                    [messageJSON.fromUserIBAN, messageSigner.nif].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        ResponseVS responseVS
        TypeVS operationType = TypeVS.valueOf(messageJSON.operation)
        Currency currency = Currency.getInstance(messageJSON.currency)
        Date transactionValidTo = DateUtils.getDateFromString(messageJSON.validTo)
        if (!messageJSON.amount || !messageJSON.currency  || !messageJSON.validTo||
                Calendar.getInstance().getTime().after(transactionValidTo) || !messageJSON.subject) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        VicketTagVS tag
        if(messageJSON.tags && !messageJSON.tags.size() == 1) { //transactions can only have one tag associated
            tag = VicketTagVS.findWhere(id:Long.valueOf(messageJSON.tags[0].id), name:messageJSON.tags[0].name)
            if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0].name}'")
        } else if(messageJSON.tags.size() > 1) {
            throw new Exception("Invalid number of tags: '${messageJSON.tags}'")
        }
        BigDecimal amount = new BigDecimal(messageJSON.amount)
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                walletVSService.getAccountMovementsForTransaction(messageJSON.fromUserIBAN, tag, amount, messageJSON.currency)
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) {
            log.error "${methodName} - ${accountFromMovements.getMessage()}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    reason:accountFromMovements.getMessage(), message:accountFromMovements.getMessage(),
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "lowBalance"))
        }
        if(operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS) {
            return processDepositForAllMembers(messageSMIMEReq, messageJSON, accountFromMovements.data, transactionValidTo ,
                    currency, groupVS, tag, locale)
        } else {
            messageJSON.toUserIBAN?.each { it ->
                UserVS userVS = getUserFromGroup(groupVS, it)
                if(!userVS) {
                    msg = messageSource.getMessage('groupUserNotFoundByIBANErrorMsg', [it, groupVS.name].toArray(), locale)
                    log.error "${methodName} - ${msg}"
                    responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                            message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
                } else {
                    receptorList.add(userVS)
                }
            }
            if(responseVS != null) return responseVS
            if(receptorList.isEmpty()) throw new Exception("Transaction without valid receptors")

            BigDecimal numUsersBigDecimal = new BigDecimal(receptorList.size())
            BigDecimal userPart = amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)

            String metaInfMsg
            TransactionVS.Type transactionVSType
            if(operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER) {
                transactionVSType = TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER
                msg = messageSource.getMessage('vicketDepositFromGroupToMemberOKMsg',
                        ["${messageJSON.amount} ${currency.getCurrencyCode()}", receptorList.iterator().next().nif].toArray(), locale)
            } else if (operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP) {
                transactionVSType = TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP
                msg = messageSource.getMessage('vicketDepositFromGroupToMemberGroupOKMsg',
                        ["${messageJSON.amount} ${currency.getCurrencyCode()}"].toArray(), locale)
            }
            UserVS systemUser = systemService.getSystemUser()
            TransactionVS transactionParent = new TransactionVS(amount: messageJSON.amount, messageSMIME:messageSMIMEReq,
                    fromUserIBAN: messageJSON.fromUserIBAN, toUserIBAN:systemUser.getIBAN(), state:TransactionVS.State.OK,
                    validTo: transactionValidTo, subject:messageJSON.subject, fromUserVS:groupVS, type:transactionVSType,
                    accountFromMovements: accountFromMovements, currencyCode: currency.getCurrencyCode(), tag:tag).save()
            receptorList.each { toUser ->
                TransactionVS transaction = new TransactionVS(amount: userPart, messageSMIME:messageSMIMEReq,
                        fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo:transactionValidTo,
                        transactionParent: transactionParent, subject:messageJSON.subject, fromUserVS:groupVS,
                        toUserIBAN:toUser.IBAN, toUserVS: toUser, currencyCode: currency.getCurrencyCode(),
                        type:transactionVSType, tag:tag).save()
                metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}_${operationType.toString()}")
                log.debug("${metaInfMsg} - ${userPart} ${messageJSON.currency} - from group '${groupVS.name}' to userVS '${toUser.id}' ")
            }
            metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transactionParent.id}_${operationType.toString()}")
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:operationType)
        }
    }


    @Transactional
    private UserVS getUserFromGroup (GroupVS groupVS, String IBAN) {
        def subscriptionList = SubscriptionVS.createCriteria().list(offset: 0) {
            eq("groupVS", groupVS)
            eq("state", SubscriptionVS.State.ACTIVE)
            userVS { eq("IBAN", IBAN)}
        }
        return subscriptionList.iterator()?.next()?.userVS
    }

    @Transactional
    private ResponseVS processDepositForAllMembers(MessageSMIME messageSMIMEReq, JSONObject messageJSON,
            Map<UserVSAccount, BigDecimal> accountFromMovements, Date transactionValidTo,
            Currency currency, GroupVS groupVS, VicketTagVS tag, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        String msg;
        String metaInfMsg
        TypeVS operationType = TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS

        def subscriptionList = SubscriptionVS.createCriteria().list(offset: 0) {
            eq("groupVS", groupVS)
            eq("state", SubscriptionVS.State.ACTIVE)
        }

        BigDecimal amount = new BigDecimal(messageJSON.amount)
        BigDecimal numUsersBigDecimal = new BigDecimal(subscriptionList.totalCount)
        BigDecimal userPart = amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)

        TransactionVS.Type transactionVSType = TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS
        msg = messageSource.getMessage('vicketDepositFromGroupToAllMembersGroupOKMsg',
                ["${messageJSON.amount} ${currency.getCurrencyCode()}"].toArray(), locale)
        UserVS systemUser = systemService.getSystemUser()
        TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo: transactionValidTo,
                subject:messageJSON.subject, fromUserVS:groupVS, currencyCode: currency.getCurrencyCode(),
                type:transactionVSType, toUserIBAN:systemUser.getIBAN(), toUserVS: systemUser, tag:tag,
                accountFromMovements: accountFromMovements).save()

        subscriptionList.each { it ->
            messageJSON.messageSMIMEParentId = messageSMIMEReq.id
            messageJSON.toUser = it.toUser.getNif()
            messageJSON.numUsers = subscriptionList.totalCount
            messageJSON.toUserAmount = userPart.toString()
            SMIMEMessageWrapper receipt = signatureVSService.getSignedMimeMessage(systemUser.getNif(), it.toUser.getNif(),
                    messageJSON.toString(), TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS.toString(), null)
            MessageSMIME messageSMIMEReceipt = new MessageSMIME(smimeParent:messageSMIMEReq,
                    type:TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, content:receipt.getBytes()).save()

            TransactionVS transaction = new TransactionVS(amount: userPart, messageSMIME:messageSMIMEReceipt,
                    fromUserVS:systemUser, fromUserIBAN: systemUser.getIBAN(), state:TransactionVS.State.OK,
                    validTo:transactionValidTo, transactionParent: transactionParent, subject:messageJSON.subject,
                    toUserVS: it.toUser, toUserIBAN:it.toUser.IBAN,currencyCode: currency.getCurrencyCode(),
                    type:transactionVSType, tag:tag).save()
            metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}_${operationType.toString()}")
            log.debug("${metaInfMsg} - ${userPart} ${messageJSON.currency} - from group '${groupVS.name}' to userVS '${it.toUser.id}' ")
        }
        metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transactionParent.id}_${operationType.toString()}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:operationType)
    }

}