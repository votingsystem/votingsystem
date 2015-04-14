package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.util.BalanceUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionVSBean {

    private static Logger log = Logger.getLogger(TransactionVSBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject MessagesBean messages;
    @Inject DAOBean dao;
    @Inject SystemBean systemBean;
    @Inject IBANBean ibanBean;
    @Inject SignatureBean signatureBean;
    @Inject BankVSBean bankVSBean;
    @Inject GroupVSBean groupVSBean;
    @Inject UserVSBean userVSBean;
    @Inject TransactionVSGroupVSBean transactionVSGroupVSBean;
    @Inject TransactionVSBankVSBean transactionVSBankVSBean;
    @Inject TransactionVSUserVSBean transactionVSUserVSBean;


    public String processTransactionVS(MessageSMIME messageSMIMEReq) throws Exception {
        TransactionVSRequest request = new TransactionVSRequest(messageSMIMEReq);
        switch(request.operation) {
            case FROM_BANKVS:
                return transactionVSBankVSBean.processTransactionVS(request.getBankVSRequest());
            case FROM_GROUP_TO_MEMBER:
            case FROM_GROUP_TO_MEMBER_GROUP:
            case FROM_GROUP_TO_ALL_MEMBERS:
                return transactionVSGroupVSBean.processTransactionVS(request.getGroupVSRequest());
            case FROM_USERVS:
                SMIMEMessage smime = transactionVSUserVSBean.processTransactionVS(request.getUserVSRequest());
                return new String(smime.getBytes(), StandardCharsets.UTF_8);
            default:
                throw new ExceptionVS(messages.get("unknownTransactionErrorMsg",request.operation.toString()));
        }
    }

    public Map getDataWithBalancesMap(UserVS userVS, DateUtils.TimePeriod timePeriod) throws Exception {
        if(userVS instanceof BankVS) return bankVSBean.getDataWithBalancesMap((BankVS) userVS, timePeriod);
        else if(userVS instanceof GroupVS) return groupVSBean.getDataWithBalancesMap(userVS, timePeriod);
        else return userVSBean.getDataWithBalancesMap(userVS, timePeriod);
    }

    private CurrencyAccount updateUserVSAccountTo(TransactionVS transactionVS) throws ExceptionVS {
        if(transactionVS.getToUserIBAN() == null) throw new ExceptionVS("transactionVS without toUserIBAN");
        Query query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("userIBAN", transactionVS.getToUserIBAN()).setParameter("tag", transactionVS.getTag())
                .setParameter("currencyCode", transactionVS.getCurrencyCode())
                .setParameter("state", CurrencyAccount.State.ACTIVE);
        CurrencyAccount accountTo = dao.getSingleResult(CurrencyAccount.class, query);
        BigDecimal resultAmount =  transactionVS.getAmount();
        if(!TagVS.WILDTAG.equals(transactionVS.getTag().getName())) {
            BigDecimal wildTagExpensesForTag = checkWildTagExpensesForTag(transactionVS.getToUserVS(),
                    transactionVS.getTag(), transactionVS.getCurrencyCode());
            if(wildTagExpensesForTag.compareTo(BigDecimal.ZERO) > 0) {
                resultAmount = resultAmount.subtract(wildTagExpensesForTag);
                query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                        .setParameter("userIBAN", transactionVS.getToUserIBAN())
                        .setParameter("tag", config.getTag(TagVS.WILDTAG))
                        .setParameter("currencyCode", transactionVS.getCurrencyCode())
                        .setParameter("state", CurrencyAccount.State.ACTIVE);
                CurrencyAccount wildTagAccount = dao.getSingleResult(CurrencyAccount.class, query);
                if(resultAmount.compareTo(BigDecimal.ZERO) > 0) {
                    dao.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(wildTagExpensesForTag)));
                } else {
                    dao.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(
                            wildTagExpensesForTag.subtract(resultAmount))));
                    resultAmount = BigDecimal.ZERO;
                }
            }
        }
        if(accountTo == null) {//new user account for tag
            //UserVS userVS, BigDecimal balance, String currencyCode, TagVS tag
            accountTo = new CurrencyAccount(transactionVS.getToUserVS(), resultAmount, transactionVS.getCurrencyCode(),
                    transactionVS.getTag());
            dao.persist(accountTo);
            log.info("new CurrencyAccount: " + accountTo.getId() + " - for IBAN:" + transactionVS.getToUserIBAN() +
                    " -  tag:" + accountTo.getTag().getName() + " - amount:" + accountTo.getBalance());
        } else dao.merge(accountTo.setBalance(accountTo.getBalance().add(resultAmount)));
        return accountTo;
    }

    private void updateUserVSAccountFrom(TransactionVS transactionVS) throws ExceptionVS {
        if(transactionVS.getAccountFromMovements() == null)
            throw new ExceptionVS("TransactionVS without accountFromMovements");
        for(Map.Entry<CurrencyAccount, BigDecimal> entry: transactionVS.getAccountFromMovements().entrySet()) {
            CurrencyAccount currencyAccount = entry.getKey();
            dao.merge(currencyAccount.setBalance(currencyAccount.getBalance().subtract(entry.getValue())));
        }
    }

    public void updateBalances(TransactionVS transactionVS) throws Exception {
        if(transactionVS.getState() == TransactionVS.State.OK) {
            boolean isParentTransaction = (transactionVS.getTransactionParent() == null);
            switch(transactionVS.getType()) {
                case CURRENCY_INIT_PERIOD:
                    break;
                case CURRENCY_INIT_PERIOD_TIME_LIMITED:
                    updateUserVSAccountFrom(transactionVS);
                    systemBean.updateTagBalance(transactionVS.getAmount(), transactionVS.getCurrencyCode(), transactionVS.getTag());
                    break;
                case FROM_USERVS:
                    updateUserVSAccountFrom(transactionVS);
                    updateUserVSAccountTo(transactionVS);
                    systemBean.updateTagBalance(transactionVS.getAmount(), transactionVS.getCurrencyCode(), transactionVS.getTag());
                    break;
                case CURRENCY_REQUEST:
                    updateUserVSAccountFrom(transactionVS);
                    systemBean.updateTagBalance(transactionVS.getAmount(), transactionVS.getCurrencyCode(), transactionVS.getTag());
                    break;
                case CURRENCY_SEND:
                    switch(transactionVS.getCurrencyTransactionBatch().getPaymentMethod()) {
                        case ANONYMOUS_SIGNED_TRANSACTION:
                            updateUserVSAccountTo(transactionVS);
                            systemBean.updateTagBalance(transactionVS.getAmount().negate(), transactionVS.getCurrencyCode(),
                                    transactionVS.getTag());
                            break;
                        case CASH_SEND:
                            systemBean.updateTagBalance(transactionVS.getAmount().negate(), transactionVS.getCurrencyCode(),
                                    transactionVS.getTag());
                            break;
                    }
                    break;
                default:
                    if(isParentTransaction) {//Parent transaction, to system before trigger to receptors
                        if(transactionVS.getType() != TransactionVS.Type.FROM_BANKVS) updateUserVSAccountFrom(transactionVS);
                        systemBean.updateTagBalance(transactionVS.getAmount(),transactionVS.getCurrencyCode(),
                                transactionVS.getTag());
                    } else {
                        updateUserVSAccountTo(transactionVS);
                        systemBean.updateTagBalance(transactionVS.getAmount().negate(), transactionVS.getCurrencyCode(),
                                transactionVS.getTag());
                        log.info("" + transactionVS.getType() + " - " + transactionVS.getAmount() +  " " +
                                transactionVS.getCurrencyCode() + " - " + transactionVS.getTag().getName() + " - " +
                                "fromUserIBAN: " + transactionVS.getFromUserIBAN() + " - toIBAN: " +
                                transactionVS.getToUserVS().getIBAN());
                    }
            }
        } else log.log(Level.SEVERE, "TransactionVS:" + transactionVS.getId() + " - state:" +
                transactionVS.getState().toString());
    }


    //Check the amount from WILDTAG account expended for the param tag
    public BigDecimal checkWildTagExpensesForTag(UserVS userVS, TagVS tagVS, String currencyCode) {
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        Map<String, Map> balancesFrom = BalanceUtils.getBalances(getTransactionFromList(userVS, timePeriod), TransactionVS.Source.FROM);
        Map<String, Map> balancesTo = BalanceUtils.getBalances(getTransactionToList(userVS, timePeriod), TransactionVS.Source.TO);
        if(balancesFrom.get("currencyCode") == null) return BigDecimal.ZERO;
        BigDecimal expendedForTagVS = (BigDecimal) balancesFrom.get(currencyCode).get(tagVS.getName());
        if(expendedForTagVS == null || BigDecimal.ZERO.compareTo(expendedForTagVS) == 0) return BigDecimal.ZERO;
        BigDecimal incomesForTagVS = (BigDecimal) ((Map)balancesTo.get(currencyCode).get(tagVS.getName())).get("total");
        if(incomesForTagVS.compareTo(expendedForTagVS) < 0) return expendedForTagVS.subtract(incomesForTagVS);
        else return BigDecimal.ZERO;
    }

    public Map getTransactionListWithBalances(List<TransactionVS> transactionList, TransactionVS.Source source) {
        List<Map> transactionFromList = new ArrayList<>();
        for(TransactionVS transaction : transactionList) {
            transactionFromList.add(getTransactionMap(transaction));
        }
        Map result = new HashMap<>();
        result.put("transactionList", transactionFromList);
        result.put("balances", BalanceUtils.getBalances(transactionList, source));
        return result;
    }

    public Map getTransactionMap(TransactionVS transaction) {
        Map transactionMap = new HashMap<>();
        transactionMap.put("id", transaction.getId());
        if(transaction.getFromUserVS() != null) {
            Map fromUserVSMap = new HashMap<>();
            fromUserVSMap.put("id", transaction.getFromUserVS().getId());
            fromUserVSMap.put("nif", transaction.getFromUserVS().getNif());
            fromUserVSMap.put("IBAN", transaction.getFromUserVS().getIBAN());
            fromUserVSMap.put("type", transaction.getFromUserVS().getType().toString());
            fromUserVSMap.put("name", transaction.getFromUserVS().getNif());
            if(transaction.getFromUserIBAN() != null) {
                Map senderMap = new HashMap<>();
                senderMap.put("fromUserIBAN", transaction.getFromUserIBAN());
                senderMap.put("fromUser", transaction.getFromUser());
                fromUserVSMap.put("sender", senderMap);
            }
            transactionMap.put("fromUserVS", fromUserVSMap);
        }
        if(transaction.getToUserVS() != null) {
            Map toUserVSMap = new HashMap<>();
            toUserVSMap.put("id", transaction.getToUserVS().getId());
            toUserVSMap.put("name", transaction.getToUserVS().getName());
            toUserVSMap.put("nif", transaction.getToUserVS().getNif());
            toUserVSMap.put("IBAN", transaction.getToUserVS().getIBAN());
            toUserVSMap.put("type", transaction.getToUserVS().getType().toString());
            transactionMap.put("toUserVS", toUserVSMap);
        }
        if(transaction.getValidTo() != null) transactionMap.put("validTo", transaction.getValidTo());
        transactionMap.put("dateCreated", transaction.getDateCreated());
        transactionMap.put("subject", transaction.getSubject());
        transactionMap.put("amount", transaction.getAmount().setScale(2, RoundingMode.FLOOR).toString());
        transactionMap.put("description", getTransactionTypeDescription(transaction.getType().toString()));
        transactionMap.put("type",  transaction.getType().toString());
        transactionMap.put("currency",  transaction.getCurrencyCode());
        if(transaction.getMessageSMIME() != null) {
            transactionMap.put("messageSMIMEURL",
                    config.getContextURL() + "/messageSMIME/" + transaction.getMessageSMIME().getId());
        }
        if(transaction.getType()  == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            TransactionVS transactionParent =
                    (transaction.getTransactionParent() == null)?transaction:transaction.getTransactionParent();
            Query query = dao.getEM().createNamedQuery("countTransByTransactionParent")
                    .setParameter("transactionParent", transactionParent);
            transactionMap.put("numChildTransactions",  (long)query.getSingleResult());
        }
        if(transaction.getTag() != null) {
            String tagName = TagVS.WILDTAG.equals(transaction.getTag().getName())? messages.get("wildTagLbl")
                    .toUpperCase():transaction.getTag().getName();
            transactionMap.put("tags", Arrays.asList(tagName));
        } else transactionMap.put("tags", new ArrayList<>());
        return transactionMap;
    }

    public String getTransactionTypeDescription(String transactionType) {
        String typeDescription;
        switch(transactionType) {
            case "CURRENCY_REQUEST":
                typeDescription = messages.get("currencyRequestLbl");
                break;
            case "CURRENCY_SEND":
                typeDescription = messages.get("currencySendLbl");
                break;
            case "FROM_BANKVS":
                typeDescription = messages.get("bankVSInputLbl");
                break;
            case "FROM_GROUP_TO_MEMBER":
                typeDescription = messages.get("transactionVSFromGroupToMember");
                break;
            case "FROM_GROUP_TO_MEMBER_GROUP":
                typeDescription = messages.get("transactionVSFromGroupToMemberGroup");
                break;
            case "FROM_GROUP_TO_ALL_MEMBERS":
                typeDescription = messages.get("transactionVSFromGroupToAllMembers");
                break;
            default: typeDescription = transactionType;
        }
        return typeDescription;
    }

    public List<TransactionVS> getTransactionFromList(UserVS fromUserVS, DateUtils.TimePeriod timePeriod) {
        List<TransactionVS> transactionList = null;
        Query query = null;
        if(fromUserVS instanceof GroupVS) {
            query = dao.getEM().createNamedQuery("findGroupVSTransFromByStateAndDateCreatedAndInListAndNotInList")
                    .setParameter("fromUserVS", fromUserVS).setParameter("state", TransactionVS.State.OK)
                    .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                    .setParameter("notList", Arrays.asList(TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS, TransactionVS.Type.CURRENCY_INIT_PERIOD))
                    .setParameter("inList", Arrays.asList(TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS));
            transactionList = query.getResultList();
        } else {
            query = dao.getEM().createNamedQuery("findUserVSTransFromByFromUserAndStateAndDateCreatedAndInList")
                    .setParameter("fromUserVS", fromUserVS).setParameter("state", TransactionVS.State.OK)
                    .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                    .setParameter("inList", Arrays.asList(TransactionVS.Type.CURRENCY_REQUEST, TransactionVS.Type.FROM_USERVS));
            transactionList = query.getResultList();
        }
        return transactionList;
    }

    public List<TransactionVS> getTransactionToList(UserVS toUserVS, DateUtils.TimePeriod timePeriod) {
        Query query = dao.getEM().createNamedQuery("findTransByToUserAndStateAndDateCreatedBetween")
                .setParameter("toUserVS", toUserVS).setParameter("state", TransactionVS.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo());
        return query.getResultList();
    }

    public class TransactionVSRequest {
        Boolean isTimeLimited;
        BigDecimal amount, numReceptors;
        UserVS fromUserVS;
        UserVS toUserVS;
        List<UserVS> toUserVSList = new ArrayList<>();
        GroupVS groupVS;
        TagVS tag;
        String currencyCode, fromUser, fromUserIBAN, subject;
        TypeVS operation;
        TransactionVS.Type transactionType;
        Date validTo;
        MessageSMIME messageSMIME;
        ObjectNode dataJSON;

        public TransactionVSRequest(MessageSMIME messageSMIMEReq) throws Exception {
            this.messageSMIME = messageSMIMEReq;
            this.fromUserVS = messageSMIME.getUserVS();
            dataJSON = (ObjectNode) new ObjectMapper().readTree(messageSMIMEReq.getSMIME().getSignedContent());

            if(dataJSON.get("toUserIBAN") instanceof ArrayNode) {
                Iterator<JsonNode> ite = ((ArrayNode)dataJSON.get("toUserIBAN")).elements();
                while (ite.hasNext()) {
                    ibanBean.validateIBAN(ite.next().asText());
                }
            } else if(dataJSON.get("toUserIBAN") != null) ibanBean.validateIBAN(dataJSON.get("toUserIBAN").asText());
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
            transactionType = TransactionVS.Type.valueOf(dataJSON.get("operation").asText());
            if(dataJSON.get("amount") == null) throw new ValidationExceptionVS("missing param 'amount'");
            amount = new BigDecimal(dataJSON.get("amount").asText());
            if(dataJSON.get("currencyCode") == null) throw new ValidationExceptionVS("missing param 'currencyCode'");
            currencyCode = dataJSON.get("currencyCode").asText();
            if(dataJSON.get("subject") == null) throw new ValidationExceptionVS("missing param 'subject'");
            subject = dataJSON.get("subject").asText();
            if(dataJSON.get("isTimeLimited") != null) isTimeLimited = dataJSON.get("isTimeLimited").asBoolean();
            if(isTimeLimited) validTo = DateUtils.getCurrentWeekPeriod().getDateTo();
            if(dataJSON.get("tags").size() == 1) { //transactions can only have one tag associated
                String tagName = ((ArrayNode)dataJSON.get("tags")).get(0).asText();
                tag = config.getTag(tagName);
                if(tag == null) throw new ValidationExceptionVS("unknown tag:" + tagName);
                if(isTimeLimited && TagVS.WILDTAG.equals(tag.getName()))
                    throw new ValidationExceptionVS("WILDTAG transactions cannot be time limited");
            } else throw new ValidationExceptionVS("invalid number of tags:" + dataJSON.get("tags").size());
        }

        public TransactionVSRequest getUserVSRequest() throws ExceptionVS {
            if(TypeVS.FROM_USERVS != operation)
                throw new ValidationExceptionVS(
                        "peration expected: 'FROM_USERVS' - operation found: " + operation.toString());
            if(dataJSON.get("toUserIBAN").size() != 1) throw new ExceptionVS(
                    "there can be only one receptor. request.toUserIBAN:" + dataJSON.get("toUserIBAN"));
            Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dataJSON.get("toUserIBAN").get(0));
            toUserVS = dao.getSingleResult(UserVS.class, query);
            if(toUserVS == null) throw new ValidationExceptionVS("invalid 'toUserIBAN':" + dataJSON.get("toUserIBAN").get(0));
            return this;
        }

        public TransactionVSRequest getBankVSRequest() throws ExceptionVS {
            if(TypeVS.FROM_BANKVS != operation) throw new ValidationExceptionVS(
                    "operation expected: 'FROM_BANKVS' - operation found: " + operation.toString());
            if(dataJSON.get("toUserIBAN").size() != 1) throw new ExceptionVS(
                    "there can be only one receptor. request.toUserIBAN: " + dataJSON.get("toUserIBAN"));
            Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dataJSON.get("toUserIBAN").get(0));
            toUserVS = dao.getSingleResult(UserVS.class, query);
            if(toUserVS == null) throw new ValidationExceptionVS("invalid 'toUserIBAN':" + dataJSON.get("toUserIBAN").get(0));
            //this is to get data from banks clients
            fromUserIBAN =  dataJSON.get("fromUserIBAN").asText();
            if(fromUserIBAN == null)  throw new ValidationExceptionVS("missing param 'fromUserIBAN'");
            fromUser = dataJSON.get("fromUser").asText();
            if(fromUser == null)  throw new ValidationExceptionVS("missing param 'fromUser'");
            return this;
        }

        public TransactionVSRequest getGroupVSRequest() throws ValidationExceptionVS {
            if(dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");

            Query query = dao.getEM().createNamedQuery("findUserByRepresentativeAndIBAN").setParameter(
                    "representative",this.fromUserVS).setParameter("IBAN", dataJSON.get("toUserIBAN").get(0));
            groupVS = dao.getSingleResult(GroupVS.class, query);
            if(groupVS == null) {
                throw new ValidationExceptionVS(messages.get(
                        "groupNotFoundByIBANErrorMsg",dataJSON.get("fromUserIBAN").asText(), fromUserVS.getNif()));
            }
            if(transactionType != TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
                for(int i = 0; i < dataJSON.get("toUserIBAN").size(); i++) {
                    query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndStateAndUserIBAN").setParameter("groupVS", groupVS)
                            .setParameter("state", SubscriptionVS.State.ACTIVE)
                            .setParameter("IBAN", dataJSON.get("toUserIBAN").get(0));
                    SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
                    if(subscription == null) throw new ValidationExceptionVS(messages.get("groupUserNotFoundByIBANErrorMsg",
                            dataJSON.get("toUserIBAN").get(i).asText(), groupVS.getName()));
                    toUserVSList.add(subscription.getUserVS());
                }
                if(toUserVSList.isEmpty()) throw new ValidationExceptionVS("transaction without valid receptors");
                numReceptors = new BigDecimal(toUserVSList.size());
            } else if (transactionType == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
                query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                        .setParameter("state", SubscriptionVS.State.ACTIVE);
                numReceptors = new BigDecimal((long)query.getSingleResult());
            }
            return this;
        }

        SMIMEMessage signReceptorData(Long messageSMIMEReqId, String toUserNif, int numReceptors, BigDecimal userPart) throws Exception {
            dataJSON.put("messageSMIMEParentId", messageSMIMEReqId);
            dataJSON.put("toUser", toUserNif);
            dataJSON.put("numUsers", numReceptors);
            dataJSON.put("toUserAmount", userPart);
            return signatureBean.getSMIME(signatureBean.getSystemUser().getNif(),
                    toUserNif, dataJSON.toString(), transactionType.toString(), null);
        }
    }
}
