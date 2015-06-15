package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.currency.BalanceUtils;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Startup
public class TransactionVSBean {

    private static Logger log = Logger.getLogger(TransactionVSBean.class.getName());

    private static final BlockingQueue<TransactionVS> queue = new ArrayBlockingQueue(50);

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject BalancesBean balancesBean;
    @Inject TransactionVSGroupVSBean transactionVSGroupVSBean;
    @Inject TransactionVSBankVSBean transactionVSBankVSBean;
    @Inject TransactionVSUserVSBean transactionVSUserVSBean;
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    @PostConstruct public void initialize() {
        log.info(" --- initialize --- ");
        executorService.submit(() -> {
            try {
                while(true) {
                    TransactionVS transactionVS = queue.take();
                    updateCurrencyAccounts(transactionVS);
                    log.info("--- queue.take - queue.size: " + queue.size());
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    @PreDestroy public void destroy() {
        log.info(" --- destroy --- ");

    }

    public void newTransactionVS(TransactionVS... transactions) {
        for(TransactionVS transactionVS : transactions) {
            try {
                queue.put(transactionVS);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public ResultListDto<TransactionVSDto> processTransactionVS(MessageSMIME messageSMIME) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        TransactionVSDto request = messageSMIME.getSignedContent(TransactionVSDto.class);
        request.validate();
        request.setTransactionVSSMIME(messageSMIME);
        if(request.getToUserIBAN() != null) {
            for(String IBAN : request.getToUserIBAN()) {
                config.validateIBAN(IBAN);
            }
        }
        String transactionTag =  request.getTags().iterator().next();
        if(request.isTimeLimited() && TagVS.WILDTAG.equals(transactionTag.toUpperCase()))
            throw new ValidationExceptionVS(messages.get("timeLimitedWildTagErrorMsg"));
        TagVS tagVS = config.getTag(transactionTag);
        if(tagVS == null) throw new ValidationExceptionVS("unknown tag:" + transactionTag);

        LoggerVS.logTransactionVS(request);

        switch(request.getOperation()) {
            case FROM_BANKVS:
                return transactionVSBankVSBean.processTransactionVS(request, tagVS);
            case FROM_GROUP_TO_MEMBER_GROUP:
            case FROM_GROUP_TO_ALL_MEMBERS:
                return transactionVSGroupVSBean.processTransactionVS(request, tagVS);
            case FROM_USERVS:
                return transactionVSUserVSBean.processTransactionVS(request, tagVS);
            default:
                throw new ExceptionVS(messages.get("unknownTransactionErrorMsg",request.getOperation().toString()));
        }
    }

    @Transactional
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
                    dao.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(transactionVS.getAmount())));
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

    @Transactional
    private void updateUserVSAccountFrom(TransactionVS transactionVS) throws ExceptionVS {
        if(transactionVS.getAccountFromMovements() == null)
            throw new ExceptionVS("TransactionVS without accountFromMovements");
        for(Map.Entry<CurrencyAccount, BigDecimal> entry: transactionVS.getAccountFromMovements().entrySet()) {
            CurrencyAccount currencyAccount = entry.getKey();
            dao.merge(currencyAccount.setBalance(currencyAccount.getBalance().subtract(entry.getValue())));
        }
    }

    public void updateCurrencyAccounts(TransactionVS transactionVS) throws Exception {
        if(transactionVS.getId() == null) transactionVS = dao.persist(transactionVS);
        log.info("updateCurrencyAccounts - TransactionVS id: " + transactionVS.getId());
        if(transactionVS.getState() == TransactionVS.State.OK) {
            boolean isParentTransaction = (transactionVS.getTransactionParent() == null);
            switch(transactionVS.getType()) {
                case CURRENCY_PERIOD_INIT:
                    break;
                case CURRENCY_PERIOD_INIT_TIME_LIMITED:
                    updateUserVSAccountFrom(transactionVS);
                    balancesBean.updateTagBalance(transactionVS.getAmount(), transactionVS.getCurrencyCode(), transactionVS.getTag());
                    break;
                case FROM_USERVS:
                    updateUserVSAccountFrom(transactionVS);
                    updateUserVSAccountTo(transactionVS);
                    balancesBean.updateTagBalance(transactionVS.getAmount(), transactionVS.getCurrencyCode(), transactionVS.getTag());
                    break;
                case CURRENCY_REQUEST:
                    updateUserVSAccountFrom(transactionVS);
                    balancesBean.updateTagBalance(transactionVS.getAmount(), transactionVS.getCurrencyCode(), transactionVS.getTag());
                    break;
                case CURRENCY_SEND:
                    updateUserVSAccountTo(transactionVS);
                    balancesBean.updateTagBalance(transactionVS.getAmount().negate(), transactionVS.getCurrencyCode(),
                            transactionVS.getTag());
                    break;
                case FROM_BANKVS:
                    updateUserVSAccountTo(transactionVS);
                    break;
                case CURRENCY_CHANGE:
                    break;
                default:
                    if(isParentTransaction) {//Parent transaction, to system before trigger to receptors
                        if(transactionVS.getType() != TransactionVS.Type.FROM_BANKVS) updateUserVSAccountFrom(transactionVS);
                        balancesBean.updateTagBalance(transactionVS.getAmount(),transactionVS.getCurrencyCode(),
                                transactionVS.getTag());
                    } else {
                        updateUserVSAccountTo(transactionVS);
                        balancesBean.updateTagBalance(transactionVS.getAmount().negate(), transactionVS.getCurrencyCode(),
                                transactionVS.getTag());
                        log.info("transactionVS: " + transactionVS.getType() + " - " + transactionVS.getAmount() +  " " +
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


    public TransactionVSDto getTransactionDto(TransactionVS transactionVS) {
        MessagesVS messages = MessagesVS.getCurrentInstance();
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
        MessagesVS messages = MessagesVS.getCurrentInstance();
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
                    .setParameter("notList", Arrays.asList(TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS, TransactionVS.Type.CURRENCY_PERIOD_INIT))
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
