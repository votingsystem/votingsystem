package org.votingsystem.cooin.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.cooin.model.Payment
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.cooin.util.BalanceUtils
import org.votingsystem.cooin.util.CoreSignal
import org.votingsystem.cooin.util.IbanVSUtil
import org.votingsystem.cooin.util.LoggerVS
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.throwable.ValidationExceptionVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.MetaInfMsg

import java.math.RoundingMode

import static org.springframework.context.i18n.LocaleContextHolder.getLocale
import static org.votingsystem.cooin.model.TransactionVS.Source

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

    private CooinAccount updateUserVSAccountTo(TransactionVS transactionVS) {
        if(!transactionVS.toUserIBAN) throw new ExceptionVS("transactionVS without toUserIBAN")
        CooinAccount accountTo = CooinAccount.findWhere(IBAN:transactionVS.toUserIBAN,
                currencyCode:transactionVS.currencyCode, tag:transactionVS.tag)
        BigDecimal resultAmount =  transactionVS.amount
        if(!TagVS.WILDTAG.equals(transactionVS.tag.getName())) {
            BigDecimal wildTagExpensesForTag = checkWildTagExpensesForTag(transactionVS.toUserVS, transactionVS.tag,
                    transactionVS.currencyCode)
            if(wildTagExpensesForTag.compareTo(BigDecimal.ZERO) > 0) {
                resultAmount = resultAmount.subtract(wildTagExpensesForTag)
                CooinAccount wildTagAccount = CooinAccount.findWhere(IBAN:transactionVS.toUserIBAN,
                        currencyCode: transactionVS.currencyCode, tag:systemService.getTag(TagVS.WILDTAG))
                if(resultAmount.compareTo(BigDecimal.ZERO) > 0) {
                    wildTagAccount.setBalance(wildTagAccount.balance.add(wildTagExpensesForTag)).save()
                } else {
                    wildTagAccount.setBalance(wildTagAccount.balance.add(wildTagExpensesForTag.subtract(resultAmount))).save()
                    resultAmount = BigDecimal.ZERO
                }
            }
        }
        if(!accountTo) {//new user account for tag
            accountTo = new CooinAccount(IBAN:transactionVS.toUserIBAN, balance:resultAmount,
                    currencyCode:transactionVS.currencyCode, tag:transactionVS.tag, userVS:transactionVS.toUserVS).save()
            log.debug("New CooinAccount '${accountTo.id}' for IBAN '${transactionVS.toUserIBAN}' - " +
                    "tag '${accountTo.tag?.name}' - amount '${accountTo.balance}'")
        } else accountTo.setBalance(accountTo.balance.add(resultAmount)).save()
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
            boolean isParentTransaction = (transactionVS.transactionParent == null)
            switch(transactionVS.type) {
                case TransactionVS.Type.COOIN_INIT_PERIOD:
                    break;
                case TransactionVS.Type.COOIN_INIT_PERIOD_TIME_LIMITED:
                    updateUserVSAccountFrom(transactionVS)
                    systemService.updateTagBalance(transactionVS.amount, transactionVS.currencyCode, transactionVS.tag)
                    break;
                case TransactionVS.Type.FROM_USERVS:
                    updateUserVSAccountFrom(transactionVS)
                    updateUserVSAccountTo(transactionVS)
                    systemService.updateTagBalance(transactionVS.amount, transactionVS.currencyCode, transactionVS.tag)
                    break;
                case TransactionVS.Type.COOIN_REQUEST:
                    updateUserVSAccountFrom(transactionVS)
                    systemService.updateTagBalance(transactionVS.amount, transactionVS.currencyCode, transactionVS.tag)
                    break;
                case TransactionVS.Type.COOIN_SEND:
                    switch(transactionVS.getCooinTransactionBatch().getPaymentMethod()) {
                        case Payment.ANONYMOUS_SIGNED_TRANSACTION:
                            updateUserVSAccountTo(transactionVS)
                            systemService.updateTagBalance(transactionVS.amount.negate(), transactionVS.currencyCode,
                                    transactionVS.tag)
                            break;
                        case Payment.CASH_SEND:
                            systemService.updateTagBalance(transactionVS.amount.negate(), transactionVS.currencyCode,
                                    transactionVS.tag)
                            break;
                    }
                    break;
                default:
                    if(isParentTransaction) {//Parent transaction, to system before trigger to receptors
                        if(transactionVS.type != TransactionVS.Type.FROM_BANKVS) updateUserVSAccountFrom(transactionVS)
                        systemService.updateTagBalance(transactionVS.amount,transactionVS.currencyCode, transactionVS.tag)
                    } else {
                        updateUserVSAccountTo(transactionVS)
                        systemService.updateTagBalance(transactionVS.amount.negate(), transactionVS.currencyCode, transactionVS.tag)
                        log.debug("${methodName} - ${transactionVS.type.toString()} - ${transactionVS.amount} ${transactionVS.currencyCode} " +
                                " - fromIBAN '${transactionVS.fromUserIBAN}' toIBAN '${transactionVS.toUserVS?.IBAN}' - " +
                                "tag '${transactionVS.tag?.name}'")
                    }
            }
            if(!isParentTransaction) notifyListeners(transactionVS)
        } else log.error("TransactionVS '${transactionVS.id}' with state ${transactionVS.state}")
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
    public List<TransactionVS> getTransactionFromList(UserVS fromUserVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            if(fromUserVS instanceof GroupVS) {
                or {
                    and{
                        eq('fromUserVS', fromUserVS)
                        eq('state', TransactionVS.State.OK)
                        isNotNull("transactionParent")
                        between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
                        not { inList("type", [TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS,
                                              TransactionVS.Type.COOIN_INIT_PERIOD] ) }
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
                        inList("type", [TransactionVS.Type.COOIN_REQUEST, TransactionVS.Type.FROM_USERVS] )
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
    public Map getTransactionListWithBalances(List<TransactionVS> transactionList, Source source) {
        List<Map> transactionFromList = []
        for(TransactionVS transaction : transactionList) {
            transactionFromList.add(getTransactionMap(transaction))
        }
        return [transactionList:transactionFromList, balances:BalanceUtils.getBalances(transactionList, source)]
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
        if(transaction.validTo) transactionMap.validTo =  transaction.validTo
        transactionMap.id = transaction.id
        transactionMap.description = getTransactionTypeDescription(transaction.getType().toString())
        transactionMap.subject = transaction.subject
        transactionMap.type = transaction.getType().toString()
        transactionMap.amount = transaction.amount.setScale(2, RoundingMode.FLOOR).toString()
        transactionMap.currency = transaction.currencyCode
        if(transaction.messageSMIME) {
            transactionMap.messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/" +
                    "${transaction.messageSMIME?.id}"
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

    //Check the amount from WILDTAG account expended for the param tag
    public BigDecimal checkWildTagExpensesForTag(UserVS userVS, TagVS tagVS, String currencyCode) {
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        Map balancesFrom = BalanceUtils.getBalances(getTransactionFromList(userVS, timePeriod), TransactionVS.Source.FROM)
        Map balancesTo = BalanceUtils.getBalances(getTransactionToList(userVS, timePeriod), TransactionVS.Source.TO)
        if(balancesFrom[currencyCode] == null) return BigDecimal.ZERO
        BigDecimal expendedForTagVS = balancesFrom[currencyCode][tagVS.name]
        if(expendedForTagVS == null || BigDecimal.ZERO.compareTo(expendedForTagVS) == 0) return BigDecimal.ZERO
        BigDecimal incomesForTagVS = balancesTo[currencyCode][tagVS.name].total
        if(incomesForTagVS.compareTo(expendedForTagVS) < 0) return expendedForTagVS.subtract(incomesForTagVS)
        else return BigDecimal.ZERO
    }

    public String getTransactionTypeDescription(String transactionType) {
        String typeDescription
        switch(transactionType) {
            case 'COOIN_REQUEST':
                typeDescription = messageSource.getMessage('cooinRequestLbl', null, locale);
                break;
            case 'COOIN_SEND':
                typeDescription = messageSource.getMessage('cooinSendLbl', null, locale);
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
                tag = systemService.getTag(messageJSON.tags[0])
                if(!tag) throw new ValidationExceptionVS(this.getClass(), "unknown tag '${messageJSON.tags[0]}'")
                if(isTimeLimited && TagVS.WILDTAG.equals(tag.getName()))
                    throw new ValidationExceptionVS(this.getClass(), "WILDTAG transactions cannot be time limited")
            } else throw new ValidationExceptionVS(this.getClass(), "invalid number of tags: '${messageJSON.tags}'")
        }

        public TransactionVSRequest getUserVSRequest() {
            if(TypeVS.FROM_USERVS != operation)
                    throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'FROM_USERVS' - operation found: " + operation.toString())
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