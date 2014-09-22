package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.orm.PagedResultList
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.model.*
import org.votingsystem.vicket.model.CoreSignal
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.AlertVS
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.util.IbanVSUtil
import org.votingsystem.vicket.util.LoggerVS
import org.votingsystem.util.MetaInfMsg
import java.math.RoundingMode
import org.votingsystem.vicket.model.CoreSignal

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class TransactionVSService {

    private static final CLASS_NAME = TransactionVSService.class.getSimpleName()

    private final Set<String> listenerSet = Collections.synchronizedSet(new HashSet<String>());

    def systemService
    def messageSource
    def grailsLinkGenerator
    def grailsApplication
    def webSocketService
    def transactionVS_GroupVSService
    def transactionVS_BankVSService

    public void addTransactionListener (String listenerId) {
        listenerSet.add(listenerId)
    }

    public ResponseVS processDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        String msg;
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage().getSignedContent())
        if(messageJSON.toUserIBAN instanceof JSONArray) {
            messageJSON.toUserIBAN.each { it ->
                IbanVSUtil.validate(it);}
        } else {
            IbanVSUtil.validate(messageJSON.toUserIBAN);
        }
        TypeVS transactionType = TypeVS.valueOf(messageJSON.operation)
        switch(transactionType) {
            case TypeVS.VICKET_DEPOSIT_FROM_BANKVS:
                return transactionVS_BankVSService.processDeposit(messageSMIMEReq, messageJSON, locale)
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
        messageMap.coreSignal = CoreSignal.NEW_TRANSACTIONVS;
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

    public void alert(AlertVS alertVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.error("${methodName} - " + alertVS.getMessage());
        alertVS.save()
    }

    public void updateBalances(TransactionVS transactionVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(transactionVS.state == TransactionVS.State.OK) {
            if(transactionVS.type == TransactionVS.Type.INIT_PERIOD) {

            } else {
                if(transactionVS.transactionParent == null) {//Parent transaction, to system before trigger to receptors
                    if(transactionVS.type != TransactionVS.Type.FROM_BANKVS) {
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
            }
        } else log.error("TransactionVS '${transactionVS.id}' with state ${transactionVS.state}")
    }

    public Map getTransactionMap(TransactionVS transaction) {
        return getTransactionMap(transaction, null)
    }

    @Transactional
    public Map getUserVSTransactionVSMap(UserVS userVS, DateUtils.TimePeriod timePeriod, Map params, Locale locale) {
        PagedResultList userVSTransactions = TransactionVS.createCriteria().list(
                sort:'dateCreated', order:'asc', offset:params?.offset, max:params?.max) {
            or {
                and{
                    isNull('transactionParent')
                    eq("fromUserVS", userVS)
                }
                eq("toUserVS", userVS)
            }
            and { between('dateCreated', timePeriod.getDateFrom(), timePeriod.getDateTo()) }
        }
        List userTransactionVSList = []
        userVSTransactions.each { it ->
            userTransactionVSList.add(getTransactionMap(it, locale))
        }
        String name = userVS.name
        if(!name) name = "${userVS.firstName} ${userVS.lastName}"
        Map userVSMap = [nif:userVS?.nif, name:name]
        return [offset: params.offset, queryRecordCount: userVSTransactions.size(),
                numTotalRecords:userVSTransactions.totalCount,  userVS: userVSMap, transactionVSList:userTransactionVSList]
    }

    @Transactional
    public PagedResultList getTransactionFromList(UserVS fromUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('fromUserVS', fromUserVS)
            isNull("transactionParent")
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        return transactionList
    }

    @Transactional
    public Map getTransactionFromListWithBalances(UserVS fromUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('fromUserVS', fromUserVS)
            isNull("transactionParent")
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        def transactionFromList = []
        Map<String, Map> balancesMap = [:]
        transactionList.each { transaction ->
            if(balancesMap[transaction.currencyCode]) {
                Map<String, BigDecimal> currencyMap = balancesMap[transaction.currencyCode]
                if(currencyMap[transaction.tag.name]) {
                    currencyMap[transaction.tag.name] = ((BigDecimal) currencyMap[transaction.tag.name]).add(transaction.amount)
                } else currencyMap[(transaction.tag.name)] = transaction.amount
            } else {
                Map<String, BigDecimal> currencyMap = [(transaction.tag.name):transaction.amount]
                balancesMap[(transaction.currencyCode)] = currencyMap
            }
            transactionFromList.add(getTransactionMap(transaction))
        }
        return [transactionFromList:transactionFromList, balancesFrom:balancesMap]
    }

    @Transactional
    public PagedResultList getTransactionToList(UserVS toUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('toUserVS', toUserVS)
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        return transactionList
    }

    @Transactional
    public Map getTransactionToListWithBalances(UserVS toUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('toUserVS', toUserVS)
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        def transactionToList = []
        Map<String, Map> balancesMap = [:]
        transactionList.each { transaction ->
            if(balancesMap[transaction.currencyCode]) {
                Map<String, BigDecimal> currencyMap = balancesMap[transaction.currencyCode]
                if(currencyMap[transaction.tag.name]) {
                    currencyMap[transaction.tag.name] = ((BigDecimal) currencyMap[transaction.tag.name]).add(transaction.amount)
                } else currencyMap[(transaction.tag.name)] = transaction.amount
            } else {
                Map<String, BigDecimal> currencyMap = [(transaction.tag.name):transaction.amount]
                balancesMap[(transaction.currencyCode)] = currencyMap
            }
            transactionToList.add(getTransactionMap(transaction))
        }
        return [transactionToList:transactionToList, balancesTo:balancesMap]
    }

    public Map<String, BigDecimal> balanceResult(Map<String, BigDecimal> balancesTo, Map<String, BigDecimal> balancesFrom) {
        Map<String, Map> balanceResult = new HashMap<String, Map>(balancesTo)
        Set<Map.Entry<String, Map>> mapEntries = balancesFrom.entrySet()
        mapEntries.each { currency ->
            Map<String, BigDecimal> currencyBalanceMap = currency.getValue()
            if(balanceResult [currency.getKey()]) {
                Set<Map.Entry<String, BigDecimal>> currencyEntriesFrom = currency.getValue().entrySet()
                Map<String, BigDecimal> currencyEntriesTo = [:]
                currencyEntriesTo << balanceResult[currency.getKey()] //To avoid balanceTo modification
                currencyEntriesFrom.each { tagEntry ->
                    if(currencyEntriesTo[tagEntry.getKey()]) {
                        BigDecimal balanceResultTagAmount = new BigDecimal ((currencyEntriesTo[tagEntry.getKey()]).toString())
                        currencyEntriesTo[tagEntry.getKey()] = balanceResultTagAmount.subtract(tagEntry.getValue())
                    } else currencyEntriesTo[(tagEntry.getKey())] = tagEntry.getValue().negate()
                }
            } else {
                balanceResult[(currency.getKey())] = currency.getValue()
                Set<Map.Entry<String, BigDecimal>> currencyEntries = balanceResult[(currency.getKey())].entrySet()
                currencyEntries.each { currEntry ->
                    currEntry.setValue(currEntry.getValue().negate())
                }
            }
        }
        return balanceResult
    }

    @Transactional
    public Map getTransactionMap(TransactionVS transaction, Locale locale) {
        Map transactionMap = [:]
        if(transaction.fromUserVS) {
            transactionMap.fromUserVS = [nif:transaction.fromUserVS.nif, name:transaction.fromUserVS.getDefaultName(),
                type:transaction.fromUserVS.type.toString(), id:transaction.fromUserVS.id]
            if(transaction.fromUserIBAN) {
                transactionMap.fromUserVS.sender = [fromUserIBAN: transaction.fromUserIBAN, fromUser:transaction.fromUser]
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
        if(locale) transactionMap.description = getTransactionTypeDescription(transaction.getType().toString(), locale)
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
            /*if(!childTransactionListDB.isEmpty()) {
                List childTransactionList = []
                childTransactionListDB.each {childTransaction ->
                    childTransactionList.add(getTransactionMap(childTransaction))
                }
                transactionMap.childTransactions = childTransactionList
            }*/
            transactionMap.numChildTransactions = childTransactionListDB.totalCount
        }

        if(transaction.tag) {
            transactionMap.tags = [[id:transaction.tag.id, name:transaction.tag.name]]
        } else transactionMap.tags = []

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
            case 'FROM_BANKVS':
                typeDescription = messageSource.getMessage('bankVSInputLbl', null, locale);
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

}