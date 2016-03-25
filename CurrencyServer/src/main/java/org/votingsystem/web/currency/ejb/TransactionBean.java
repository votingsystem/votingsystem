package org.votingsystem.web.currency.ejb;

import com.google.common.collect.Sets;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.currency.BalanceUtils;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionBean {

    private static Logger log = Logger.getLogger(TransactionBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject BalancesBean balancesBean;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject TransactionBankBean transactionBankBean;
    @Inject TransactionUserBean transactionUserBean;


    public ResultListDto<TransactionDto> processTransaction(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        TransactionDto request = cmsMessage.getSignedContent(TransactionDto.class);
        request.validate();
        request.setCmsMessage_DB(cmsMessage);
        if(request.getToUserIBAN() != null) {
            for(String IBAN : request.getToUserIBAN()) {
                config.validateIBAN(IBAN);
            }
        }
        String transactionTag =  request.getTags().iterator().next();
        if(request.isTimeLimited() && TagVS.WILDTAG.equals(transactionTag.toUpperCase()))
            throw new ValidationException(messages.get("timeLimitedWildTagErrorMsg"));
        TagVS tagVS = config.getTag(transactionTag);
        if(tagVS == null) throw new ValidationException("unknown tag:" + transactionTag);

        LoggerVS.logTransaction(request);

        switch(request.getOperation()) {
            case FROM_BANK:
                return transactionBankBean.processTransaction(request, tagVS);
            case FROM_USER:
                return transactionUserBean.processTransaction(request, tagVS);
            default:
                throw new ExceptionVS(messages.get("unknownTransactionErrorMsg",request.getOperation().toString()));
        }
    }

    private synchronized CurrencyAccount updateUserAccountTo(Transaction transaction) throws Exception {
        if(transaction.getToUserIBAN() == null) throw new ExceptionVS("transaction without toUserIBAN");
        Query query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("userIBAN", transaction.getToUserIBAN()).setParameter("tag", transaction.getTag())
                .setParameter("currencyCode", transaction.getCurrencyCode())
                .setParameter("state", CurrencyAccount.State.ACTIVE);
        CurrencyAccount accountTo = dao.getSingleResult(CurrencyAccount.class, query);
        BigDecimal resultAmount =  transaction.getAmount();
        if(!TagVS.WILDTAG.equals(transaction.getTag().getName())) {
            BigDecimal wildTagExpensesForTag = checkWildTagExpensesForTag(transaction.getToUser(),
                    transaction.getTag(), transaction.getCurrencyCode());
            if(wildTagExpensesForTag.compareTo(BigDecimal.ZERO) > 0) {
                resultAmount = resultAmount.subtract(wildTagExpensesForTag);
                query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                        .setParameter("userIBAN", transaction.getToUserIBAN())
                        .setParameter("tag", config.getTag(TagVS.WILDTAG))
                        .setParameter("currencyCode", transaction.getCurrencyCode())
                        .setParameter("state", CurrencyAccount.State.ACTIVE);
                CurrencyAccount wildTagAccount = dao.getSingleResult(CurrencyAccount.class, query);
                if(resultAmount.compareTo(BigDecimal.ZERO) > 0) {
                    dao.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(wildTagExpensesForTag)));
                } else {
                    dao.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(transaction.getAmount())));
                    resultAmount = BigDecimal.ZERO;
                }
            }
        }
        if(accountTo == null) {//new user account for tag
            //check if there exists a WILDTAG account for that user and that CurrencyCode
            if(!TagVS.WILDTAG.equals(transaction.getTag().getName()))
                currencyAccountBean.checkWildtagUserAccountForCurrency(transaction.getToUser(), transaction.getCurrencyCode());
            accountTo = new CurrencyAccount(transaction.getToUser(), resultAmount, transaction.getCurrencyCode(),
                    transaction.getTag());
            dao.persist(accountTo);
            log.info("new CurrencyAccount: " + accountTo.getId() + " - for IBAN:" + transaction.getToUserIBAN() +
                    " -  tag:" + accountTo.getTag().getName() + " - amount:" + accountTo.getBalance());
        } else {
            dao.merge(accountTo.setBalance(accountTo.getBalance().add(resultAmount)));
            log.info("updateUserAccountTo - account id: " + accountTo.getId() + " - balance: " +
                    accountTo.getBalance() + " - LastUpdated: " + accountTo.getLastUpdated());
        }
        return accountTo;
    }

    private synchronized void updateUserAccountFrom(Transaction transaction) throws ExceptionVS {
        if(transaction.getAccountFromMovements() == null)
            throw new ExceptionVS("Transaction without accountFromMovements");
        for(Map.Entry<CurrencyAccount, BigDecimal> entry: transaction.getAccountFromMovements().entrySet()) {
            CurrencyAccount currencyAccount = entry.getKey();
            dao.merge(currencyAccount.setBalance(currencyAccount.getBalance().subtract(entry.getValue())));
            log.info("updateUserAccountFrom - account id: " + currencyAccount.getId() + " - balance: " +
                    currencyAccount.getBalance() + " - LastUpdated: " + currencyAccount.getLastUpdated());
        }
    }

    public void updateCurrencyAccounts(Transaction transaction) throws Exception {
        if(transaction.getId() == null) transaction = dao.persist(transaction);
        log.info("updateCurrencyAccounts - Transaction id: " + transaction.getId());
        if(transaction.getState() == Transaction.State.OK) {
            boolean isParentTransaction = (transaction.getTransactionParent() == null);
            switch(transaction.getType()) {
                case CURRENCY_PERIOD_INIT:
                    break;
                case CURRENCY_PERIOD_INIT_TIME_LIMITED:
                    updateUserAccountFrom(transaction);
                    balancesBean.updateTagBalance(transaction.getAmount(), transaction.getCurrencyCode(), transaction.getTag());
                    break;
                case FROM_USER:
                    updateUserAccountFrom(transaction);
                    updateUserAccountTo(transaction);
                    break;
                case CURRENCY_REQUEST:
                    updateUserAccountFrom(transaction);
                    balancesBean.updateTagBalance(transaction.getAmount(), transaction.getCurrencyCode(), transaction.getTag());
                    break;
                case CURRENCY_SEND:
                    updateUserAccountTo(transaction);
                    for(Currency currency : transaction.getCurrencyBatch().getValidatedCurrencySet()) {
                        balancesBean.updateTagBalance(currency.getAmount().negate(), currency.getCurrencyCode(),
                                currency.getTagVS());
                    }
                    Currency leftOver = transaction.getCurrencyBatch().getLeftOver();
                    if(leftOver != null) balancesBean.updateTagBalance(leftOver.getAmount(), leftOver.getCurrencyCode(),
                            leftOver.getTagVS());
                    break;
                case FROM_BANK:
                    updateUserAccountTo(transaction);
                    break;
                case CURRENCY_CHANGE:
                    for(Currency currency : transaction.getCurrencyBatch().getValidatedCurrencySet()) {
                        balancesBean.updateTagBalance(currency.getAmount().negate(), currency.getCurrencyCode(),
                                currency.getTagVS());
                    }
                    Currency changeLeftOver = transaction.getCurrencyBatch().getLeftOver();
                    if(changeLeftOver != null) balancesBean.updateTagBalance(changeLeftOver.getAmount(),
                            changeLeftOver.getCurrencyCode(), changeLeftOver.getTagVS());
                    Currency currencyChange = transaction.getCurrencyBatch().getCurrencyChange();
                    balancesBean.updateTagBalance(currencyChange.getAmount(),
                            currencyChange.getCurrencyCode(), currencyChange.getTagVS());
                    break;
                default:
                    if(isParentTransaction) {//Parent transaction, to system before trigger to receptors
                        if(transaction.getType() != Transaction.Type.FROM_BANK) updateUserAccountFrom(transaction);
                        balancesBean.updateTagBalance(transaction.getAmount(), transaction.getCurrencyCode(),
                                transaction.getTag());
                    } else {
                        updateUserAccountTo(transaction);
                        balancesBean.updateTagBalance(transaction.getAmount().negate(), transaction.getCurrencyCode(),
                                transaction.getTag());
                        log.info("transaction: " + transaction.getType() + " - " + transaction.getAmount() +  " " +
                                transaction.getCurrencyCode() + " - " + transaction.getTag().getName() + " - " +
                                "fromUserIBAN: " + transaction.getFromUserIBAN() + " - toIBAN: " +
                                transaction.getToUser().getIBAN());
                    }
            }
        } else log.log(Level.SEVERE, "Transaction:" + transaction.getId() + " - state:" +
                transaction.getState().toString());
    }

    //Check the amount from WILDTAG account expended for the param tag
    public BigDecimal checkWildTagExpensesForTag(User user, TagVS tagVS, CurrencyCode currencyCode) {
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        Map<CurrencyCode, Map<String, BigDecimal>> balancesFrom =
                BalanceUtils.getBalancesFrom(getTransactionFromList(user, timePeriod));
        Map<CurrencyCode, Map<String, IncomesDto>> balancesTo = BalanceUtils.getBalancesTo(getTransactionToList(user, timePeriod));
        if(balancesFrom.get(currencyCode) == null) return BigDecimal.ZERO;
        BigDecimal expendedForTagVS = balancesFrom.get(currencyCode).get(tagVS.getName());
        if(expendedForTagVS == null || BigDecimal.ZERO.compareTo(expendedForTagVS) == 0) return BigDecimal.ZERO;
        BigDecimal totalIncomesForTagVS = balancesTo.get(currencyCode).get(tagVS.getName()).getTotal();
        if(totalIncomesForTagVS.compareTo(expendedForTagVS) < 0) return expendedForTagVS.subtract(totalIncomesForTagVS);
        else return BigDecimal.ZERO;
    }

    public TransactionDto getTransactionDto(Transaction transaction) {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        TransactionDto dto = new TransactionDto(transaction, config.getContextURL());
        dto.setDescription(getTransactionTypeDescription(transaction.getType().toString()));
        if(transaction.getTag() != null) {
            String tagName = TagVS.WILDTAG.equals(transaction.getTag().getName())? messages.get("wildTagLbl")
                    .toUpperCase(): transaction.getTag().getName();
            dto.setTags(Sets.newHashSet(tagName));
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
            case "FROM_BANK":
                typeDescription = messages.get("bankInputLbl");
                break;
            default: typeDescription = transactionType;
        }
        return typeDescription;
    }

    public List<Transaction> getTransactionFromList(User fromUser, Interval timePeriod) {
        Query query = dao.getEM().createNamedQuery("findUserTransFromByFromUserAndStateAndDateCreatedAndInList")
                .setParameter("fromUser", fromUser).setParameter("state", Transaction.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("inList", Arrays.asList(Transaction.Type.CURRENCY_REQUEST, Transaction.Type.FROM_USER));
        List<Transaction> transactionList = query.getResultList();
        return transactionList;
    }

    public List<Transaction> getTransactionToList(User toUser, Interval timePeriod) {
        Query query = dao.getEM().createNamedQuery("findTransByToUserAndStateAndDateCreatedBetween")
                .setParameter("toUser", toUser).setParameter("state", Transaction.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo());
        return query.getResultList();
    }

}
