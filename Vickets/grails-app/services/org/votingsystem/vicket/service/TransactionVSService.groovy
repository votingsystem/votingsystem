package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.log4j.Logger
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.LoggerVS
import org.votingsystem.vicket.util.MetaInfMsg
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.util.IbanVSUtil

import java.math.RoundingMode

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TransactionVSService {

    private final Set<String> listenerSet = Collections.synchronizedSet(new HashSet<String>());

    def signatureVSService
    def messageSource
    def userVSService
    def sessionFactory
    def grailsLinkGenerator
    def grailsApplication
    def webSocketService
    def csrService


    public ResponseVS cancelVicketDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        //messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        log.debug(smimeMessageReq.getSignedContent())
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = smimeMessageReq.getFrom().toString()
        String subject = messageSource.getMessage('vicketReceiptSubject', null, locale)
        SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                smimeMessageReq, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                content:smimeMessageResp.getBytes()).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.VICKET_CANCEL, data:messageSMIMEResp,
                contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED)
    }

    public void addTransactionListener (String listenerId) {
        listenerSet.add(listenerId)
    }

    public ResponseVS processDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        String msg;
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage().getSignedContent())
        String toUser = smimeMessageReq.getFrom().toString().replace(" ", "")
        try {
            if(messageJSON.toUserIBAN instanceof JSONArray) {
                messageJSON.toUserIBAN.each { it ->
                    IbanVSUtil.validate(it);}
            } else {
                IbanVSUtil.validate(messageJSON.toUserIBAN);
            }
        } catch(Exception ex) {
            msg = messageSource.getMessage('IBANCodeErrorMsg', [smimeMessageReq.getFrom().toString()].toArray(),
                    locale)
            log.error("${methodName} - ${msg} - ${ex.getMessage()}", ex)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: msg,
                    metaInf:MetaInfMsg.getExceptionMsg(methodName, ex, "IBAN_code"), type:TypeVS.ERROR)
        }
        TypeVS transactionType = TypeVS.valueOf(messageJSON.operation)
        switch(transactionType) {
            case TypeVS.VICKET_DEPOSIT_FROM_VICKET_SOURCE:
                processDepositFromVicketSource(messageSMIMEReq, messageJSON, locale)
                break;
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                processDepositFromGroup(messageSMIMEReq, messageJSON, locale)
                break;
            default:
                msg = messageSource.getMessage('unknownTransactionErrorMsg', [transactionType.toString()].toArray(), locale)
                log.debug("${methodName} - ${msg}");
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: msg,
                        metaInf:MetaInfMsg.getErrorMsg(methodName, "UNKNOWN_TRANSACTION"), type:TypeVS.ERROR)
        }
    }


    public void notifyListeners(TransactionVS transactionVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        Map messageMap = getTransactionMap(transactionVS)
        if(!listenerSet.isEmpty()) {
            ResponseVS broadcastResult = webSocketService.broadcastList(messageMap, listenerSet);
            if(ResponseVS.SC_OK != broadcastResult.statusCode) {
                def errorList = broadcastResult.data
                errorList.each {listenerSet.remove(it)}
            }
        } else log.debug("${methodName} - NO listeners")
        if(transactionVS.state == TransactionVS.State.OK) {
            UserVSAccount accountFrom
            UserVSAccount accountTo
            boolean isParent = true
            switch (transactionVS.type) {
                case TransactionVS.Type.VICKET_SOURCE_INPUT: //fromUserIBAN is not a system IBAN
                    accountTo = UserVSAccount.findWhere(IBAN:transactionVS.toUserIBAN, currencyCode:transactionVS.currencyCode)
                    break;
                case TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                    if(transactionVS.transactionParent != null) {
                        isParent = false
                        accountFrom = UserVSAccount.findWhere(IBAN:transactionVS.fromUserIBAN, currencyCode:transactionVS.currencyCode)
                        accountTo = UserVSAccount.findWhere(IBAN:transactionVS.toUserIBAN, currencyCode:transactionVS.currencyCode)
                    } else return;
                    break;
                case TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                    if(transactionVS.transactionParent != null) {
                        isParent = false
                        accountFrom = UserVSAccount.findWhere(IBAN:transactionVS.fromUserIBAN, currencyCode:transactionVS.currencyCode)
                        accountTo = UserVSAccount.findWhere(IBAN:transactionVS.toUserIBAN, currencyCode:transactionVS.currencyCode)
                    } else return;
                    break;
                case TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                    if(transactionVS.transactionParent != null) {
                        accountFrom = UserVSAccount.findWhere(IBAN:transactionVS.fromUserIBAN, currencyCode:transactionVS.currencyCode)
                        accountTo = UserVSAccount.findWhere(IBAN:transactionVS.toUserIBAN, currencyCode:transactionVS.currencyCode)
                    } else return;
                    break;
                default:
                    log.error("Unprocessed TransactionVS.Type: ${transactionVS.type.toString()}")
            }
            if(accountFrom) {
                accountFrom.balance = accountFrom.balance.subtract(transactionVS.amount)
                accountFrom.save()
            }
            if(accountTo) {
                accountTo.balance = accountTo.balance.add(transactionVS.amount)
                accountTo.save()
            }
            LoggerVS.logTransactionVS(transactionVS.id, transactionVS.state.toString(), transactionVS.type.toString(),
                    transactionVS.fromUserIBAN, transactionVS.toUserIBAN, transactionVS.getCurrencyCode(),
                    transactionVS, transactionVS.dateCreated, transactionVS.subject, isParent)
            log.debug("${methodName} - ${transactionVS.type.toString()} - ${transactionAmount} ${transactionVS.currencyCode} " +
                    " - fromIBAN '${transactionVS.fromUserIBAN}' toIBAN '${accountTo?.IBAN}'")
        }
    }

    private ResponseVS processDepositFromVicketSource(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {
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

        TransactionVS transaction = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                fromUserIBAN: messageJSON.fromUserIBAN, toUserIBAN:messageJSON.toUserIBAN, fromUser: messageJSON.fromUser,
                state:TransactionVS.State.OK, validTo:validTo, subject:subject, fromUserVS:signer, toUserVS: toUser,
                currencyCode: currency.getCurrencyCode(), type:TransactionVS.Type.VICKET_SOURCE_INPUT).save()
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}")
        log.debug("${metaInfMsg} - from VicketSource '${signer.id}' to userVS '${toUser.id}' ")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", metaInf:metaInfMsg,
                type:TypeVS.VICKET_DEPOSIT_FROM_VICKET_SOURCE)
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
    private ResponseVS processDepositFromGroup(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {
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

        if(operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS) {
            def subscriptionList = SubscriptionVS.createCriteria().list(offset: 0) {
                eq("groupVS", groupVS)
                eq("state", SubscriptionVS.State.ACTIVE)
            }
            subscriptionList.each { it ->
                receptorList.add(it.userVS)
            }
        }
        if(responseVS != null) return responseVS
        Currency currency = Currency.getInstance(messageJSON.currency)
        boolean transactionWithUsersIBAN = (operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER ||
                operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP ||
                operationType == TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS)
        Date transactionValidTo = DateUtils.getDateFromString(messageJSON.validTo)
        if (!messageJSON.amount || !messageJSON.currency || receptorList.isEmpty() || !messageJSON.validTo||
                !transactionWithUsersIBAN || Calendar.getInstance().getTime().after(transactionValidTo) ||
                !messageJSON.subject) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }

        BigDecimal amount = new BigDecimal(messageJSON.amount)
        BigDecimal numUsersBigDecimal = new BigDecimal(receptorList.size())
        BigDecimal userPart = amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)

        UserVSAccount groupAccount = UserVSAccount.findWhere(IBAN:messageJSON.fromUserIBAN, currencyCode: messageJSON.currency)

        if(! (groupAccount?.balance.compareTo(amount) > 0) ) {
            msg = messageSource.getMessage('balanceErrorMsg', ["${groupAccount?.balance} ${groupAccount.currencyCode}",
                   "${amount} ${currency.getCurrencyCode()}"].toArray(), locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR, reason:msg,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }

        String metaInfMsg
        TransactionVS.Type transactionVSType
        switch(operationType) {
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                transactionVSType = TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP
                msg = messageSource.getMessage('vicketDepositFromGroupToMemberGroupOKMsg',
                        ["${messageJSON.amount} ${currency.getCurrencyCode()}"].toArray(), locale)
                TransactionVS transactionParent = new TransactionVS(amount: messageJSON.amount, messageSMIME:messageSMIMEReq,
                        fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo: transactionValidTo,
                        subject:messageJSON.subject, fromUserVS:groupVS, currencyCode: currency.getCurrencyCode(),
                        type:transactionVSType).save()
                receptorList.each { toUser ->
                    TransactionVS transaction = new TransactionVS(amount: userPart, messageSMIME:messageSMIMEReq,
                            fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo:transactionValidTo,
                            transactionParent: transactionParent, subject:messageJSON.subject, fromUserVS:groupVS,
                            toUserIBAN:toUser.IBAN, toUserVS: toUser, currencyCode: currency.getCurrencyCode(),
                            type:transactionVSType).save()
                    metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}")
                    log.debug("${metaInfMsg} - ${userPart} ${messageJSON.currency} - from group '${groupVS.name}' to userVS '${toUser.id}' ")
                }
                break;
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                transactionVSType = TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER
                msg = messageSource.getMessage('vicketDepositFromGroupToMemberOKMsg',
                        ["${messageJSON.amount} ${currency.getCurrencyCode()}", receptorList.iterator().next().nif].toArray(), locale)
                TransactionVS transactionParent = new TransactionVS(amount: messageJSON.amount, messageSMIME:messageSMIMEReq,
                        fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo: transactionValidTo,
                        subject:messageJSON.subject, fromUserVS:groupVS, currencyCode: currency.getCurrencyCode(),
                        type:transactionVSType).save()
                receptorList.each { toUser ->
                    TransactionVS transaction = new TransactionVS(amount: userPart, messageSMIME:messageSMIMEReq,
                            fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo:transactionValidTo,
                            transactionParent: transactionParent, subject:messageJSON.subject, fromUserVS:groupVS,
                            toUserIBAN:toUser.IBAN, toUserVS: toUser, currencyCode: currency.getCurrencyCode(),
                            type:transactionVSType).save()
                    metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}")
                    log.debug("${metaInfMsg} - ${userPart} ${messageJSON.currency} - from group '${groupVS.name}' to userVS '${toUser.id}' ")
                }
                break;
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                transactionVSType = TransactionVS.Type.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS
                msg = messageSource.getMessage('vicketDepositFromGroupToAllMembersGroupOKMsg',
                        ["${messageJSON.amount} ${currency.getCurrencyCode()}"].toArray(), locale)
                UserVS systemUser = userVSService.getSystemUser()
                TransactionVS transactionParent = new TransactionVS(amount: messageJSON.amount, messageSMIME:messageSMIMEReq,
                        fromUserIBAN: messageJSON.fromUserIBAN, state:TransactionVS.State.OK, validTo: transactionValidTo,
                        subject:messageJSON.subject, fromUserVS:groupVS, currencyCode: currency.getCurrencyCode(),
                        type:transactionVSType, toUserIBAN:systemUser.getIBAN(), toUserVS: systemUser).save()
                receptorList.each { toUser ->
                    messageJSON.messageSMIMEParentId = messageSMIMEReq.id
                    messageJSON.toUser = toUser.getNif()
                    messageJSON.numUsers = receptorList.size()
                    messageJSON.toUserAmount = userPart.toString()
                    SMIMEMessageWrapper receipt = signatureVSService.getSignedMimeMessage(systemUser.getNif(), toUser.getNif(),
                            messageJSON.toString(), TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS.toString(), null)
                    MessageSMIME messageSMIMEReceipt = new MessageSMIME(smimeParent:messageSMIMEReq,
                            type:TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, content:receipt.getBytes()).save()

                    TransactionVS transaction = new TransactionVS(amount: userPart, messageSMIME:messageSMIMEReceipt,
                            fromUserVS:systemUser, fromUserIBAN: systemUser.getIBAN(), state:TransactionVS.State.OK,
                            validTo:transactionValidTo, transactionParent: transactionParent, subject:messageJSON.subject,
                            toUserVS: toUser, toUserIBAN:toUser.IBAN,currencyCode: currency.getCurrencyCode(),
                            type:transactionVSType).save()
                    metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${transaction.id}")
                    log.debug("${metaInfMsg} - ${userPart} ${messageJSON.currency} - from group '${groupVS.name}' to userVS '${toUser.id}' ")
                }
                break;
        }

        metaInfMsg = MetaInfMsg.getOKMsg(methodName, "transactionVS_${operationType.toString()}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, metaInf:metaInfMsg, type:operationType)
    }

    public ResponseVS processVicketRequest(MessageSMIME messageSMIMEReq, byte[] csrRequest, Locale locale) {
        log.debug("processVicketRequest");
        //To avoid circular references issues
        ResponseVS responseVS = ((VicketService)grailsApplication.mainContext.getBean("vicketService")).processRequest(
                messageSMIMEReq, locale)
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            ResponseVS vicketGenBatchResponse = csrService.signVicketBatchRequest(csrRequest,
                    responseVS.data.amount, responseVS.data.currency, locale)
            if (ResponseVS.SC_OK == vicketGenBatchResponse.statusCode) {
                UserVS userVS = messageSMIMEReq.userVS
                TransactionVS userTransaction = new TransactionVS(amount:responseVS.data.amount,
                        state:TransactionVS.State.OK, currency:responseVS.data.currency,
                        subject: messageSource.getMessage('vicketRequest', null, locale), messageSMIME: messageSMIMEReq,
                        fromUserVS: userVS, toUserVS: userVS, type:TransactionVS.Type.VICKET_REQUEST).save()

                Map transactionMap = getTransactionMap(userTransaction)
                Map resultMap = [transactionList:[transactionMap], issuedVickets:vicketGenBatchResponse.data]
                return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_ENCRYPTED,
                        type:TypeVS.VICKET_REQUEST, messageBytes:"${resultMap as JSON}".getBytes());
            } else return vicketGenBatchResponse
        } else return responseVS
    }

    @Transactional
    public Map getTransactionMap(TransactionVS transaction) {
        Map transactionMap = [:]
        if(transaction.fromUserVS) {
            transactionMap.fromUserVS = [nif:transaction.fromUserVS.nif, name:transaction.fromUserVS.getDefaultName(),
                type:transaction.fromUserVS.type.toString(), id:transaction.fromUserVS.id]
            if(transaction.fromUserIBAN) {
                transactionMap.fromUserVS.payer = [fromUserIBAN: transaction.fromUserIBAN,
                                                   fromUser:transaction.fromUser]
            }
        }
        if(transaction.toUserVS) {
            String toUserVSName = "${transaction.toUserVS.getDefaultName()}"
            transactionMap.toUserVS = [nif:transaction.toUserVS.nif, name:toUserVSName, id:transaction.toUserVS.id,
                                       type:transaction.toUserVS.type.toString()]
        }
        transactionMap.dateCreated = transaction.dateCreated
        if(transaction.validTo) transactionMap.validTo = transaction.validTo
        transactionMap.id = transaction.id
        transactionMap.subject = transaction.subject
        transactionMap.type = transaction.getType().toString()
        transactionMap.amount = transaction.amount.setScale(2, RoundingMode.FLOOR).toString()
        transactionMap.currency = transaction.currencyCode

        if(transaction.messageSMIME) {
            String messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${transaction.messageSMIME?.id}"
            transactionMap.messageSMIMEURL = messageSMIMEURL
        }

        if(transaction.transactionParent == null) {
            def childTransactionListDB = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
                eq('transactionParent', transaction)
            }
            if(!childTransactionListDB.isEmpty()) {
                List childTransactionList = []
                childTransactionListDB.each {childTransaction ->
                    childTransactionList.add(getTransactionMap(childTransaction))
                }
                transactionMap.childTransactions = childTransactionList
            }
        }

        return transactionMap
    }

    public String getTransactionTypeDescription(String transactionType, Locale locale) {
        String typeDescription
        switch(transactionType) {
            case 'VICKET_REQUEST':
                typeDescription = messageSource.getMessage('vicketRequestLbl', null, locale);
                break;
            case 'VICKET_SEND':
                typeDescription = messageSource.getMessage('vicketSendLbl', null, locale);
                break;
            case 'VICKET_CANCELLATION':
                typeDescription = messageSource.getMessage('vicketCancellationLbl', null, locale);
                break;
            case 'VICKET_SOURCE_INPUT':
                typeDescription = messageSource.getMessage('vicketSourceInputLbl', null, locale);
                break;
            case 'VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER':
                typeDescription = messageSource.getMessage('vicketDepositFromGroupToMember', null, locale);
                break;
            case 'VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP':
                typeDescription = messageSource.getMessage('vicketDepositFromGroupToMemberGroup', null, locale);
                break;
            case 'VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS':
                typeDescription = messageSource.getMessage('vicketDepositFromGroupToAllMembers', null, locale);
                break;
            default: typeDescription = transactionType

        }
        return typeDescription
    }

    public Map getUserInfoMap(UserVS userVS, Calendar mondayLapse) {
        Calendar weekToCalendar = mondayLapse.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)
        def userInputTransactions = TransactionVS.createCriteria().scroll {
            eq("toUserVS", userVS)
            or {
                eq("type", TransactionVS.Type.VICKET_CANCELLATION)
            }
            ge("dateCreated", mondayLapse.getTime())
            le("dateCreated", weekToCalendar.getTime())
        }

        String dirPath = DateUtils.getDirPath(mondayLapse.getTime())
        //int year =  calendar.get(Calendar.YEAR);
        //int month = calendar.get(Calendar.MONTH) + 1; // Note: zero based!
        //int day = calendar.get(Calendar.DAY_OF_MONTH);
        def userOutputTransactions = TransactionVS.createCriteria().scroll {
            eq("fromUserVS", userVS)
            eq("type", TransactionVS.Type.VICKET_REQUEST)
            ge("dateCreated", mondayLapse.getTime())
        }

        Map resultMap = [:]
        Map dateResultMap = [:]
        dateResultMap[Currency.getInstance("EUR").getCurrencyCode()] = [totalOutputs: new BigDecimal(0),
                totalInputs:new BigDecimal(0), transactionList:[]]

        while (userInputTransactions.next()) {
            TransactionVS transactionVS = (TransactionVS) userInputTransactions.get(0);
            Map currencyMap = dateResultMap.get(transactionVS.getCurrencyCode())
            if(!currencyMap) {
                currencyMap = [totalOutputs: new BigDecimal(0), totalInputs:new BigDecimal(0), transactionList:[]]
                dateResultMap.put(transactionVS.getCurrencyCode(), currencyMap)
            }
            currencyMap.totalInputs = currencyMap.totalInputs.add(transactionVS.amount)
            currencyMap.transactionList.add(getTransactionMap(transactionVS))
        }

        while (userOutputTransactions.next()) {
            TransactionVS transactionVS = (TransactionVS) userOutputTransactions.get(0);
            Map currencyMap = dateResultMap.get(transactionVS.getCurrencyCode())
            if(!currencyMap) {
                currencyMap = [totalOutputs: new BigDecimal(0), totalInputs:new BigDecimal(0), transactionList:[]]
                dateResultMap.put(transactionVS.getCurrencyCode(), currencyMap)
            }
            currencyMap.totalOutputs = currencyMap.totalOutputs.add(transactionVS.amount)
            currencyMap.transactionList.add(getTransactionMap(transactionVS))
        }
        resultMap.date = DateUtils.getStringFromDate(Calendar.getInstance().getTime())
        resultMap[dirPath] = dateResultMap
        return resultMap
    }

}