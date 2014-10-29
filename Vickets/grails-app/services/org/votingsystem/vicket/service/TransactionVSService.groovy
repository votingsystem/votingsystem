package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.TransactionVS
import static org.votingsystem.vicket.model.TransactionVS.*
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.CoreSignal
import org.votingsystem.vicket.util.IbanVSUtil
import org.votingsystem.vicket.util.LoggerVS
import java.math.RoundingMode
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class TransactionVSService {

    private final Set<String> listenerSet = Collections.synchronizedSet(new HashSet<String>());

    def systemService
    def signatureVSService
    def messageSource
    def grailsLinkGenerator
    def grailsApplication
    def webSocketService
    def transactionVS_GroupVSService
    def transactionVS_BankVSService
    def transactionVS_UserVSService

    public ResponseVS processTransactionVS(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        TransactionVSRequest request = new TransactionVSRequest(messageSMIMEReq)
        switch(request.operation) {
            case TypeVS.FROM_BANKVS:
                return transactionVS_BankVSService.processTransactionVS(request.getBankVSRequest())
            case TypeVS.FROM_GROUP_TO_MEMBER:
            case TypeVS.FROM_GROUP_TO_MEMBER_GROUP:
            case TypeVS.FROM_GROUP_TO_ALL_MEMBERS:
                return transactionVS_GroupVSService.processTransactionVS(request.getGroupVSRequest())
            case TypeVS.FROM_USERVS_TO_USERVS:
            case TypeVS.FROM_USERVS:
                return transactionVS_UserVSService.processTransactionVS(request.getUserVSRequest())
            default:
                throw new ExceptionVS(messageSource.getMessage('unknownTransactionErrorMsg',
                        [request.operation.toString()].toArray(), locale),
                        MetaInfMsg.getErrorMsg(methodName, "UNKNOWN_TRANSACTION"))
        }
    }

    public void addTransactionListener (String listenerId) {
        listenerSet.add(listenerId)
    }

    public void notifyListeners(TransactionVS transactionVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        Map messageMap = getTransactionMap(transactionVS)
        messageMap.coreSignal = CoreSignal.NEW_TRANSACTIONVS;
        if(!listenerSet.isEmpty()) { //notify websocket clients listening transactions
            log.debug("${methodName} - sending websocket message to listeners")
            ResponseVS broadcastResult = webSocketService.broadcastList(messageMap, listenerSet);
            if(ResponseVS.SC_OK != broadcastResult.statusCode) {
                def errorList = broadcastResult.data
                errorList.each {listenerSet.remove(it)}
            }
        }
        LoggerVS.logTransactionVS(transactionVS.id, transactionVS.state.toString(), transactionVS.type.toString(),
                transactionVS.fromUserIBAN, transactionVS.toUserIBAN, transactionVS.getCurrencyCode(),
                transactionVS.amount, transactionVS.tag, transactionVS.dateCreated, transactionVS.validTo,
                transactionVS.subject, transactionVS.transactionParent?true:false)
    }

    public void alert(ResponseVS responseVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.error("${methodName} - " + responseVS.getMetaInf());
        responseVS.save()
    }

    private UserVSAccount updateUserVSAccountTo(TransactionVS transactionVS) {
        if(!transactionVS.toUserIBAN) throw new ExceptionVS("transactionVS without toUserIBAN")
        UserVSAccount accountTo = UserVSAccount.findWhere(IBAN:transactionVS.toUserIBAN,
                currencyCode:transactionVS.currencyCode, tag:transactionVS.tag)
        if(!accountTo) {//new user account for tag
            accountTo = new UserVSAccount(IBAN:transactionVS.toUserIBAN, balance:transactionVS.amount,
                    currencyCode:transactionVS.currencyCode, tag:transactionVS.tag, userVS:transactionVS.toUserVS).save()
            log.debug("New UserVSAccount '${accountTo.id}' for IBAN '${transactionVS.toUserIBAN}' - " +
                    "tag '${accountTo.tag?.name}' - amount '${accountTo.balance}'")
        } else {
            accountTo.setBalance(accountTo.balance.add(transactionVS.amount)).save()
        }
        return accountTo
    }

    private void updateUserVSAccountFrom(TransactionVS transactionVS) {
        if(!transactionVS.accountFromMovements) throw new ExceptionVS("TransactionVS without accountFromMovements")
        transactionVS.accountFromMovements.each { userAccountFrom, amount->
            userAccountFrom.setBalance(userAccountFrom.balance.subtract(amount)).save()
        }
    }

    public void updateBalances(TransactionVS transactionVS) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(transactionVS.state == TransactionVS.State.OK) {
            boolean isLoggable = true
            switch(transactionVS.type) {
                case TransactionVS.Type.VICKET_INIT_PERIOD:
                    break;
                case TransactionVS.Type.VICKET_INIT_PERIOD_TIME_LIMITED:
                    updateUserVSAccountFrom(transactionVS)
                    systemService.updateTagBalance(transactionVS.amount, transactionVS.currencyCode, transactionVS.tag)
                    break;
                case TransactionVS.Type.FROM_USERVS:
                case TransactionVS.Type.FROM_USERVS_TO_USERVS:
                case TransactionVS.Type.VICKET_REQUEST:
                    updateUserVSAccountFrom(transactionVS)
                    systemService.updateTagBalance(transactionVS.amount, transactionVS.currencyCode, transactionVS.tag)
                    break;
                case TransactionVS.Type.VICKET_SEND:
                    updateUserVSAccountTo(transactionVS)
                    systemService.updateTagBalance(transactionVS.amount.negate(), transactionVS.currencyCode, transactionVS.tag)
                    break;
                default:
                    if(transactionVS.transactionParent == null) {//Parent transaction, to system before trigger to receptors
                        if(transactionVS.type != TransactionVS.Type.FROM_BANKVS) updateUserVSAccountFrom(transactionVS)
                        systemService.updateTagBalance(transactionVS.amount,transactionVS.currencyCode, transactionVS.tag)
                        isLoggable = false
                    } else {
                        updateUserVSAccountTo(transactionVS)
                        systemService.updateTagBalance(transactionVS.amount.negate(), transactionVS.currencyCode, transactionVS.tag)
                        log.debug("${methodName} - ${transactionVS.type.toString()} - ${transactionVS.amount} ${transactionVS.currencyCode} " +
                                " - fromIBAN '${transactionVS.fromUserIBAN}' toIBAN '${transactionVS.toUserVS?.IBAN}' - " +
                                "tag '${transactionVS.tag?.name}'")
                    }
            }
            if(isLoggable)notifyListeners(transactionVS)
        } else log.error("TransactionVS '${transactionVS.id}' with state ${transactionVS.state}")
    }

    @Transactional
    public List getTransactionFromList(UserVS fromUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            if(fromUserVS instanceof GroupVS) {
                or {
                    and{
                        eq('fromUserVS', fromUserVS)
                        eq('state', TransactionVS.State.OK)
                        isNotNull("transactionParent")
                        between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
                        not { inList("type", [TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS,
                                              TransactionVS.Type.VICKET_INIT_PERIOD] ) }
                    }
                    and {
                        eq('fromUserVS', fromUserVS)
                        eq('state', TransactionVS.State.OK)
                        isNull("transactionParent")
                        between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
                        inList("type", [TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS] )
                    }
                }
            } else {
                or {
                    and {
                        eq('fromUserVS', fromUserVS)
                        eq('state', TransactionVS.State.OK)
                        isNotNull("transactionParent")
                        between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
                    }
                    and {
                        eq('fromUserVS', fromUserVS)
                        eq('state', TransactionVS.State.OK)
                        isNull("transactionParent")
                        between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
                        inList("type", [TransactionVS.Type.VICKET_REQUEST, TransactionVS.Type.FROM_USERVS,
                                        TransactionVS.Type.FROM_USERVS_TO_USERVS] )
                    }
                }

            }
        }
        return transactionList
    }

    @Transactional public Map getDataWithBalancesMap(UserVS userVS, DateUtils.TimePeriod timePeriod){
        if(userVS instanceof  BankVS) {
            return ((BankVSService)grailsApplication.mainContext.getBean("bankVSService")).getDataWithBalancesMap(
                    userVS, timePeriod)
        } else if(userVS instanceof GroupVS) {
            return ((GroupVSService)grailsApplication.mainContext.getBean("groupVSService")).getDataWithBalancesMap(
                    userVS, timePeriod)
        } else {
            return ((UserVSService)grailsApplication.mainContext.getBean("userVSService")).getDataWithBalancesMap(
                    userVS, timePeriod)
        }
    }

    @Transactional
    public Map getTransactionFromListWithBalances(UserVS fromUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = getTransactionFromList(fromUserVS, timePeriod)
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
    public List getTransactionToList(UserVS toUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('toUserVS', toUserVS)
            eq('state', TransactionVS.State.OK)
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
        }
        return transactionList
    }

    @Transactional
    public Map getTransactionToListWithBalances(UserVS toUserVS, DateUtils.TimePeriod timePeriod) {
        List<TransactionVS> transactionList = getTransactionToList(toUserVS, timePeriod)
        def transactionToList = []
        Map<String, Map> balancesMap = [:]
        for(TransactionVS transaction : transactionList) {
            TransactionVSUtils.addTransactionVSToBalance(balancesMap, transaction)
            transactionToList.add(getTransactionMap(transaction))
        }
        return [transactionToList:transactionToList, balancesTo:balancesMap]
    }

    @Transactional
    public Map getTransactionMap(TransactionVS transaction) {
        Map transactionMap = [:]
        if(transaction.fromUserVS) {
            transactionMap.fromUserVS = [nif:transaction.fromUserVS.nif, type:transaction.fromUserVS.type.toString(),
                 name:transaction.fromUserVS.name, id:transaction.fromUserVS.id, IBAN:transaction.fromUserVS.IBAN]
            if(transaction.fromUserIBAN) {
                transactionMap.fromUserVS.sender = [fromUserIBAN: transaction.fromUserIBAN, fromUser:transaction.fromUser]
            }
        }
        if(transaction.toUserVS) {
            String toUserVSName = "${transaction.toUserVS.name}"
            transactionMap.toUserVS = [nif:transaction.toUserVS.nif, name:toUserVSName, id:transaction.toUserVS.id,
                      IBAN:transaction.toUserVS.IBAN, type:transaction.toUserVS.type.toString()]
        }
        transactionMap.dateCreated = transaction.dateCreated
        transactionMap.dateCreatedValue = DateUtils.getDateStr(transaction.dateCreated)
        if(transaction.validTo) {
            transactionMap.validTo =  transaction.validTo
            transactionMap.validToValue =  DateUtils.getDateStr(transaction.validTo)
        }
        transactionMap.id = transaction.id
        transactionMap.description = getTransactionTypeDescription(transaction.getType().toString())
        transactionMap.subject = transaction.subject
        transactionMap.type = transaction.getType().toString()
        transactionMap.amount = transaction.amount.setScale(2, RoundingMode.FLOOR).toString()
        transactionMap.currency = transaction.currencyCode

        if(transaction.messageSMIME) {
            String messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${transaction.messageSMIME?.id}"
            transactionMap.messageSMIMEURL = messageSMIMEURL
        }

        if(transaction.type  == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            TransactionVS transactionParent = (transaction.transactionParent == null)?transaction:transaction.transactionParent;
            transactionMap.numChildTransactions = TransactionVS.countByTransactionParent(transactionParent)
        }
        if(transaction.tag) {
            String tagName = TagVS.WILDTAG.equals(transaction.tag.name)? messageSource.getMessage('wildTagLbl', null,
                    locale).toUpperCase():transaction.tag.name
            transactionMap.tags = [tagName]
        } else transactionMap.tags = []
        return transactionMap
    }

    public String getTransactionTypeDescription(String transactionType) {
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
            case 'FROM_GROUP_TO_MEMBER':
                typeDescription = messageSource.getMessage('transactionVSFromGroupToMember', null, locale);
                break;
            case 'FROM_GROUP_TO_MEMBER_GROUP':
                typeDescription = messageSource.getMessage('transactionVSFromGroupToMemberGroup', null, locale);
                break;
            case 'FROM_GROUP_TO_ALL_MEMBERS':
                typeDescription = messageSource.getMessage('transactionVSFromGroupToAllMembers', null, locale);
                break;
            default: typeDescription = transactionType
        }
        return typeDescription
    }

    private class TransactionVSRequest {
        Boolean isTimeLimited;
        BigDecimal amount, numReceptors;
        UserVS fromUserVS;
        UserVS toUserVS;
        List<UserVS> toUserVSList = []
        GroupVS groupVS
        TagVS tag;
        String currencyCode, fromUser, fromUserIBAN, subject;
        TypeVS operation;
        TransactionVS.Type transactionType;
        Date validTo;
        MessageSMIME messageSMIME
        JSONObject messageJSON;

        public TransactionVSRequest(MessageSMIME messageSMIMEReq) {
            this.messageSMIME = messageSMIMEReq
            this.fromUserVS = messageSMIME.userVS
            messageJSON = JSON.parse(messageSMIMEReq.getSMIME().getSignedContent())
            if(messageJSON.toUserIBAN instanceof JSONArray) {
                messageJSON.toUserIBAN.each { it ->IbanVSUtil.validate(it);}
            } else if(messageJSON.toUserIBAN) IbanVSUtil.validate(messageJSON.toUserIBAN);
            operation = TypeVS.valueOf(messageJSON.operation)
            transactionType = TransactionVS.Type.valueOf(messageJSON.operation)
            if(!messageJSON.amount)  throw new ValidationExceptionVS(this.getClass(), "missing param 'amount'");
            amount = new BigDecimal(messageJSON.amount)
            if(!messageJSON.currencyCode)  throw new ValidationExceptionVS(this.getClass(), "missing param 'currencyCode'");
            currencyCode = messageJSON.currencyCode
            if(!messageJSON.subject)  throw new ValidationExceptionVS(this.getClass(), "missing param 'subject'");
            subject = messageJSON.subject
            isTimeLimited = messageJSON.isTimeLimited
            if(isTimeLimited) validTo = DateUtils.getCurrentWeekPeriod().dateTo
            if(messageJSON.tags?.size() == 1) { //transactions can only have one tag associated
                tag = TagVS.findWhere(name:messageJSON.tags[0])
                if(!tag) throw new ValidationExceptionVS(this.getClass(), "Unknown tag '${messageJSON.tags[0]}'")
            } else throw new ValidationExceptionVS(this.getClass(), "Invalid number of tags: '${messageJSON.tags}'")
        }

        public TransactionVSRequest getUserVSRequest() {
            if(!(TypeVS.FROM_USERVS == operation || TypeVS.FROM_USERVS_TO_USERVS == operation))
                    throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'FROM_USERVS' - operation found: " + operation.toString())
            if(messageJSON.toUserIBAN.length() != 1) throw new ExceptionVS(
                    "There can be only one receptor. request.toUserIBAN -> ${messageJSON.toUserIBAN} ")
            toUserVS = UserVS.findWhere(IBAN:messageJSON.toUserIBAN.get(0))
            if(!toUserVS) throw new ValidationExceptionVS(this.getClass(), "invalid 'toUserIBAN': '${messageJSON.toUserIBAN}'");
            return this;
        }

        public TransactionVSRequest getUserVSToUserVSRequest() {
            if(TypeVS.FROM_USERVS_TO_USERVS != operation) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'FROM_USERVS_TO_USERVS' - operation found: " + operation.toString())
            if(messageJSON.toUserIBAN.length() != 1) throw new ExceptionVS(
                    "There can be only one receptor. request.toUserIBAN -> ${messageJSON.toUserIBAN} ")
            toUserVS = UserVS.findWhere(IBAN:messageJSON.toUserIBAN.get(0))
            if(!toUserVS) throw new ValidationExceptionVS(this.getClass(), "invalid 'toUserIBAN': '${messageJSON.toUserIBAN}'");
            return this;
        }

        public TransactionVSRequest getBankVSRequest() {
            if(TypeVS.FROM_BANKVS != operation) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'FROM_BANKVS' - operation found: " + operation.toString())
            if(messageJSON.toUserIBAN.length() != 1) throw new ExceptionVS(
                    "There can be only one receptor. request.toUserIBAN -> ${messageJSON.toUserIBAN} ")
            toUserVS = UserVS.findWhere(IBAN:messageJSON.toUserIBAN.get(0))
            if(!toUserVS) throw new ValidationExceptionVS(this.getClass(), "invalid 'toUserIBAN': '${messageJSON.toUserIBAN}'");
            //This is for banks clients
            fromUserIBAN =  messageJSON.fromUserIBAN
            if(!fromUserIBAN)  throw new ValidationExceptionVS(this.getClass(), "missing param 'fromUserIBAN'");
            fromUser = messageJSON.fromUser
            if(!fromUser)  throw new ValidationExceptionVS(this.getClass(), "missing param 'fromUser'");
            return this;
        }

        public TransactionVSRequest getGroupVSRequest() {
            if(!messageJSON.operation) throw new ValidationExceptionVS(this.getClass(), "missing param 'operation'");
            this.messageJSON = messageJSON
            groupVS = GroupVS.findWhere(IBAN:messageJSON.fromUserIBAN, representative:this.fromUserVS)
            if(!groupVS) {
                throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage('groupNotFoundByIBANErrorMsg',
                        [messageJSON.fromUserIBAN, this.fromUserVS.nif].toArray(), locale))
            }
            if(transactionType != TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
                for(int i = 0; i < messageJSON.toUserIBAN.size(); i++) {
                    List subscriptionList = SubscriptionVS.createCriteria().list(offset: 0) {
                        eq("groupVS", groupVS)
                        eq("state", SubscriptionVS.State.ACTIVE)
                        userVS { eq("IBAN", messageJSON.toUserIBAN.get(i))}
                    };
                    if(subscriptionList.isEmpty()) throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage(
                            'groupUserNotFoundByIBANErrorMsg',  [messageJSON.toUserIBAN.get(i), groupVS.name].toArray(), locale))
                    toUserVSList.add(((SubscriptionVS)subscriptionList.iterator().next()).userVS)
                }
                if(toUserVSList.isEmpty()) throw new ValidationExceptionVS(this.getClass(),
                        "Transaction without valid receptors")
                numReceptors = new BigDecimal(toUserVSList.size())
            } else if (transactionType == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
                numReceptors = new BigDecimal(SubscriptionVS.countByGroupVSAndState(groupVS, SubscriptionVS.State.ACTIVE))
            }
            return this;
        }

        SMIMEMessage signReceptorData(Long messageSMIMEReqId, String toUserNif, int numReceptors, BigDecimal userPart) {
            messageJSON.messageSMIMEParentId = messageSMIMEReqId? messageSMIMEReqId : null
            messageJSON.toUser = toUserNif? toUserNif : null
            messageJSON.numUsers = numReceptors?numReceptors:null
            messageJSON.toUserAmount = userPart?userPart.toString():null
            return signatureVSService.getSMIME(systemService.getSystemUser().getNif(),
                    toUserNif, messageJSON.toString(), transactionType.toString(), null)
        }
    }
}