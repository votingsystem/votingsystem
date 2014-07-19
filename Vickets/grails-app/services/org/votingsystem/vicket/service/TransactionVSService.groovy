package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.log4j.Logger
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.model.UserVSAccount
import org.votingsystem.vicket.model.WalletVS
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
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class TransactionVSService {

    private final Set<String> listenerSet = Collections.synchronizedSet(new HashSet<String>());

    def systemService
    def signatureVSService
    def messageSource
    def userVSService
    def grailsLinkGenerator
    def grailsApplication
    def webSocketService
    def csrService
    def walletVSService
    def transactionVS_GroupVSService


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
                return processDepositFromVicketSource(messageSMIMEReq, messageJSON, locale)
                break;
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
            case TypeVS.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                return transactionVS_GroupVSService.processDeposit(messageSMIMEReq, messageJSON, locale)
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
        if(!listenerSet.isEmpty()) { //notify websocket clients listening transactions
            ResponseVS broadcastResult = webSocketService.broadcastList(messageMap, listenerSet);
            if(ResponseVS.SC_OK != broadcastResult.statusCode) {
                def errorList = broadcastResult.data
                errorList.each {listenerSet.remove(it)}
            }
        } else log.debug("${methodName} - NO websocket listeners")
        LoggerVS.logTransactionVS(transactionVS.id, transactionVS.state.toString(), transactionVS.type.toString(),
                transactionVS.fromUserIBAN, transactionVS.toUserIBAN, transactionVS.getCurrencyCode(),
                transactionVS.amount, transactionVS.tag, transactionVS.dateCreated, transactionVS.subject,
                transactionVS.transactionParent?true:false)
    }

    public void updateBalances(TransactionVS transactionVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(transactionVS.state == TransactionVS.State.OK) {
            if(transactionVS.transactionParent == null) {//Triggering transaction, to system before share out among receptors
                if(transactionVS.type != TransactionVS.Type.VICKET_SOURCE_INPUT) {
                    transactionVS.accountFromMovements.each { userAccountFrom, amount->
                        userAccountFrom.balance = userAccountFrom.balance.subtract(amount)
                        userAccountFrom.save()
                    }
                }
                systemService.updateTagBalance(transactionVS.amount,transactionVS.currencyCode, transactionVS.tag)
            } else {
                UserVSAccount accountTo = UserVSAccount.findWhere(IBAN:transactionVS.toUserIBAN,
                        currencyCode:transactionVS.currencyCode, tag:transactionVS.tag)
                if(!accountTo) {//new user account for tag
                    accountTo = new UserVSAccount(IBAN:transactionVS.toUserIBAN, balance:transactionVS.amount,
                            currencyCode:transactionVS.currencyCode, tag:transactionVS.tag, userVS:transactionVS.toUserVS).save()
                    log.debug("New UserVSAccount '${accountTo.id}' for IBAN '${transactionVS.toUserIBAN}' - " +
                            "tag '${accountTo.tag?.name}' - amount '${accountTo.balance}'")
                } else {
                    accountTo.balance = accountTo.balance.add(transactionVS.amount)
                    accountTo.save()
                }
                systemService.updateTagBalance(transactionVS.amount.negate(), transactionVS.currencyCode, transactionVS.tag)
                notifyListeners(transactionVS)
                log.debug("${methodName} - ${transactionVS.type.toString()} - ${transactionVS.amount} ${transactionVS.currencyCode} " +
                        " - fromIBAN '${transactionVS.fromUserIBAN}' toIBAN '${accountTo?.IBAN}' - tag '${transactionVS.tag?.name}'")
            }
        } else log.error("TransactionVS '${transactionVS.id}' with state ${transactionVS.state}")
    }

    @Transactional
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

        VicketTagVS tag
        if(messageJSON.tags && !messageJSON.tags.size() == 1) { //transactions can only have one tag associated
            tag = VicketTagVS.findWhere(id:Long.valueOf(messageJSON.tags[0].id), name:messageJSON.tags[0].name)
            if(!tag) throw new Exception("Unknown tag '${messageJSON.tags[0].name}'")
        } else if(messageJSON.tags.size() > 1) {
            throw new Exception("Invalid number of tags: '${messageJSON.tags}'")
        }

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

        if(transaction.tag) {
            transactionMap.tags = [[id:transaction.tag.id, name:transaction.tag.name]]
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