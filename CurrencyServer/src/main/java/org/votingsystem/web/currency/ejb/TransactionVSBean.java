package org.votingsystem.web.currency.ejb;

import com.google.common.eventbus.Subscribe;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.*;
import org.votingsystem.service.EventBusService;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.currency.cdi.ConfigVSImpl;
import org.votingsystem.web.currency.util.BalanceUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Startup
public class TransactionVSBean {

    private static Logger log = Logger.getLogger(TransactionVSBean.class.getName());

    @Inject ConfigVS config;
    @Inject MessagesBean messages;
    @Inject DAOBean dao;
    @Inject SystemBean systemBean;
    @Inject SignatureBean signatureBean;
    @Inject BankVSBean bankVSBean;
    @Inject GroupVSBean groupVSBean;
    @Inject UserVSBean userVSBean;
    @Inject TransactionVSGroupVSBean transactionVSGroupVSBean;
    @Inject TransactionVSBankVSBean transactionVSBankVSBean;
    @Inject TransactionVSUserVSBean transactionVSUserVSBean;


    @PostConstruct public void initialize() {
        log.info(" --- initialize --- ");
        EventBusService.getInstance().register(this);
    }

    @PreDestroy public void destroy() {
        log.info(" --- destroy --- ");
        EventBusService.getInstance().unRegister(this);
    }

    @Asynchronous
    @Subscribe public void newTransactionVS(final TransactionVS transactionVS) {
        log.info("newTransactionVS: " + transactionVS.getId());
        try {
            updateCurrencyAccounts(transactionVS);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public ResultListDto<TransactionVSDto> processTransactionVS(MessageSMIME messageSMIME) throws Exception {
        TransactionVSDto request = messageSMIME.getSignedContent(TransactionVSDto.class);
        request.validate();
        request.setSigner(messageSMIME.getUserVS());
        for(String IBAN : request.getToUserIBAN()) {
            ((ConfigVSImpl)config).validateIBAN(IBAN);
        }
        String transactionTag =  request.getTags().iterator().next();
        if(request.isTimeLimited() && TagVS.WILDTAG.equals(transactionTag.toUpperCase()))
            throw new ValidationExceptionVS("WILDTAG transactions cannot be time limited");
        request.setTag(config.getTag(transactionTag));
        if(request.getTag() == null) throw new ValidationExceptionVS("unknown tag:" + transactionTag);
        switch(request.getOperation()) {
            case FROM_BANKVS:
                return transactionVSBankVSBean.processTransactionVS(validateBankVSRequest(request));
            case FROM_GROUP_TO_MEMBER:
            case FROM_GROUP_TO_MEMBER_GROUP:
            case FROM_GROUP_TO_ALL_MEMBERS:
                return transactionVSGroupVSBean.processTransactionVS(validateGroupVSRequest(request));
            case FROM_USERVS:
                return transactionVSUserVSBean.processTransactionVS(validateUserVSRequest(request));
            default:
                throw new ExceptionVS(messages.get("unknownTransactionErrorMsg",request.getOperation().toString()));
        }
    }

    public TransactionVSDto validateBankVSRequest(TransactionVSDto dto) throws ValidationExceptionVS {
        if(TypeVS.FROM_BANKVS != dto.getOperation())
            throw new ValidationExceptionVS(
                    "peration expected: 'FROM_BANKVS' - operation found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationExceptionVS(
                "there can be only one receptor. request.toUserIBAN:" + dto.getToUserIBAN());
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dto.getToUserIBAN().get(0));
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        if(toUserVS == null) throw new ValidationExceptionVS("invalid 'toUserIBAN':" + dto.getToUserIBAN().get(0));
        dto.setReceptor(toUserVS);
        return dto;
    }

    public TransactionVSDto validateGroupVSRequest(TransactionVSDto dto) throws ValidationExceptionVS {
        Query query = dao.getEM().createNamedQuery("findUserByRepresentativeAndIBAN").setParameter(
                "representative", dto.getSigner()).setParameter("IBAN", dto.getToUserIBAN().get(0));
        GroupVS groupVS = dao.getSingleResult(GroupVS.class, query);
        if(groupVS == null) {
            throw new ValidationExceptionVS(messages.get(
                    "groupNotFoundByIBANErrorMsg", dto.getFromUserIBAN(), dto.getSigner().getNif()));
        }
        if(dto.getType() != TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            List<UserVS> toUserVSList = new ArrayList<>();
            for(String groupUserIBAN : dto.getToUserIBAN()) {
                query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndStateAndUserIBAN").setParameter("groupVS", groupVS)
                        .setParameter("state", SubscriptionVS.State.ACTIVE)
                        .setParameter("IBAN", groupUserIBAN);
                SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
                if(subscription == null) throw new ValidationExceptionVS(messages.get("groupUserNotFoundByIBANErrorMsg",
                        groupUserIBAN, groupVS.getName()));
                toUserVSList.add(subscription.getUserVS());
            }
            if(toUserVSList.isEmpty()) throw new ValidationExceptionVS("transaction without valid receptors");
            dto.setToUserVSList(toUserVSList);
        } else if (dto.getType() == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            query = dao.getEM().createNamedQuery("countSubscriptionByGroupVSAndState").setParameter("groupVS", groupVS)
                    .setParameter("state", SubscriptionVS.State.ACTIVE);
            dto.setNumReceptors(((Long)query.getSingleResult()).intValue());
        }
        return dto;
    }

    public TransactionVSDto validateUserVSRequest(TransactionVSDto dto) throws ValidationExceptionVS{
        if(TypeVS.FROM_BANKVS != dto.getOperation()) throw new ValidationExceptionVS(
                "operation expected: 'FROM_BANKVS' - operation found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationExceptionVS(
                "there can be only one receptor. request.toUserIBAN: " + dto.getToUserIBAN().get(0));
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dto.getToUserIBAN().get(0));
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        if(toUserVS == null) throw new ValidationExceptionVS("invalid 'toUserIBAN':" + dto.getToUserIBAN().get(0));
        //this is to get data from banks clients
        if(dto.getFromUserIBAN() == null)  throw new ValidationExceptionVS("missing param 'fromUserIBAN'");
        if(dto.getFromUser() == null)  throw new ValidationExceptionVS("missing param 'fromUser'");
        return dto;
    }

    public BalancesDto getBalancesDto(UserVS userVS, TimePeriod timePeriod) throws Exception {
        if(userVS instanceof BankVS) return bankVSBean.getBalancesDto((BankVS) userVS, timePeriod);
        else if(userVS instanceof GroupVS) return groupVSBean.getBalancesDto(userVS, timePeriod);
        else return userVSBean.getBalancesDto(userVS, timePeriod);
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

    public void updateCurrencyAccounts(TransactionVS transactionVS) throws Exception {
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
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        Map<String, Map<String, BigDecimal>> balancesFrom =
                BalanceUtils.getBalancesFrom(getTransactionFromList(userVS, timePeriod));
        Map<String, Map<String, IncomesDto>> balancesTo = BalanceUtils.getBalancesTo(getTransactionToList(userVS, timePeriod));
        if(balancesFrom.get(currencyCode) == null) return BigDecimal.ZERO;
        BigDecimal expendedForTagVS = balancesFrom.get(currencyCode).get(tagVS.getName());
        if(expendedForTagVS == null || BigDecimal.ZERO.compareTo(expendedForTagVS) == 0) return BigDecimal.ZERO;
        BigDecimal totalIncomesForTagVS = balancesTo.get(currencyCode).get(tagVS.getName()).getTotal();
        if(totalIncomesForTagVS.compareTo(expendedForTagVS) < 0) return expendedForTagVS.subtract(totalIncomesForTagVS);
        else return BigDecimal.ZERO;
    }

    public BalancesDto getBalancesDto(List<TransactionVS> transactionList, TransactionVS.Source source) throws ExceptionVS {
        List<TransactionVSDto> transactionFromList = new ArrayList<>();
        for(TransactionVS transaction : transactionList) {
            transactionFromList.add(getTransactionDto(transaction));
        }
        switch (source) {
            case FROM:
                return BalancesDto.FROM(transactionFromList, BalanceUtils.getBalancesFrom(transactionList));
            case TO:
                return BalancesDto.TO(transactionFromList, BalanceUtils.getBalancesTo(transactionList));
        }
        throw new ExceptionVS("unknown source: " + source);
    }

    public TransactionVSDto getTransactionDto(TransactionVS transactionVS) {
        TransactionVSDto dto = new TransactionVSDto(transactionVS, config.getContextURL());
        dto.setDescription(getTransactionTypeDescription(transactionVS.getType().toString()));
        if(transactionVS.getType()  == TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS) {
            TransactionVS transactionParent =
                    (transactionVS.getTransactionParent() == null)?transactionVS:transactionVS.getTransactionParent();
            Query query = dao.getEM().createNamedQuery("countTransByTransactionParent")
                    .setParameter("transactionParent", transactionParent);
            dto.setNumChildTransactions((long) query.getSingleResult());
        }
        if(transactionVS.getTag() != null) {
            String tagName = TagVS.WILDTAG.equals(transactionVS.getTag().getName())? messages.get("wildTagLbl")
                    .toUpperCase():transactionVS.getTag().getName();
            dto.setTags(new HashSet<>(Arrays.asList(tagName)));
        }
        return dto;
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

    public List<TransactionVS> getTransactionFromList(UserVS fromUserVS, TimePeriod timePeriod) {
        List<TransactionVS> transactionList = null;
        Query query = null;
        if(fromUserVS instanceof GroupVS) {
            query = dao.getEM().createQuery("SELECT t FROM TransactionVS t WHERE (t.fromUserVS =:fromUserVS and t.state =:state " +
                    "and t.transactionParent is not null and t.dateCreated between :dateFrom and :dateTo " +
                    "and t.type not in (:notList)) OR (t.fromUserVS =:fromUserVS and t.state =:state " +
                    "and t.transactionParent is null and  t.dateCreated between :dateFrom and :dateTo " +
                    "and t.type in (:inList))").setParameter("fromUserVS", fromUserVS).setParameter("state", TransactionVS.State.OK)
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

    public List<TransactionVS> getTransactionToList(UserVS toUserVS, TimePeriod timePeriod) {
        Query query = dao.getEM().createNamedQuery("findTransByToUserAndStateAndDateCreatedBetween")
                .setParameter("toUserVS", toUserVS).setParameter("state", TransactionVS.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo());
        return query.getResultList();
    }

}
