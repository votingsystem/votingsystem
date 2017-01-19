package org.votingsystem.currency.web.ejb;

import com.google.common.collect.Sets;
import org.votingsystem.cms.BalanceUtils;
import org.votingsystem.currency.web.util.AuditLogger;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
public class TransactionEJB {

    private static Logger log = Logger.getLogger(TransactionEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private BalancesEJB balancesBean;
    @Inject private CurrencyAccountEJB currencyAccountService;
    @Inject private BankTransactionEJB bankTransactionService;
    @Inject private UserTransactionEJB userTransactionService;


    public ResultListDto<TransactionDto> processTransaction(SignedDocument signedDocument) throws Exception {
        TransactionDto request = signedDocument.getSignedContent(TransactionDto.class);
        request.setSignedDocument(signedDocument).validate();
        if(request.getToUserIBAN() != null) {
            for(String IBAN : request.getToUserIBAN()) {
                config.validateIBAN(IBAN);
            }
        }
        String transactionTag =  request.getTags().iterator().next();
        if(request.isTimeLimited() && Tag.WILDTAG.equals(transactionTag.toUpperCase()))
            throw new ValidationException(Messages.currentInstance().get("timeLimitedWildTagErrorMsg"));
        Tag tag = config.getTag(transactionTag);

        AuditLogger.logTransaction(request);

        switch(request.getOperation()) {
            case TRANSACTION_FROM_BANK:
                return bankTransactionService.processTransactionFromBank(request, tag);
            case TRANSACTION_FROM_USER:
                return userTransactionService.processTransactionFromUser(request, tag);
            default:
                throw new ValidationException(
                        Messages.currentInstance().get("unknownTransactionErrorMsg", request.getOperation().toString()));
        }
    }

    private synchronized CurrencyAccount updateUserAccountTo(Transaction transaction) throws ValidationException {
        if(transaction.getToUserIBAN() == null) 
            throw new ValidationException("transaction without toUserIBAN");
        List<CurrencyAccount> accounts = em.createNamedQuery(
                CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", transaction.getToUserIBAN()).setParameter("tag", transaction.getTag())
                .setParameter("currencyCode", transaction.getCurrencyCode())
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        CurrencyAccount accountTo = null;
        if(!accounts.isEmpty())
            accountTo = accounts.iterator().next();
        BigDecimal resultAmount =  transaction.getAmount();
        if(!Tag.WILDTAG.equals(transaction.getTag().getName())) {
            BigDecimal wildTagExpensesForTag = checkWildTagExpensesForTag(transaction.getToUser(),
                    transaction.getTag(), transaction.getCurrencyCode());
            if(wildTagExpensesForTag.compareTo(BigDecimal.ZERO) > 0) {
                resultAmount = resultAmount.subtract(wildTagExpensesForTag);
                accounts = em.createNamedQuery(CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE)
                        .setParameter("userIBAN", transaction.getToUserIBAN())
                        .setParameter("tag", config.getTag(Tag.WILDTAG))
                        .setParameter("currencyCode", transaction.getCurrencyCode())
                        .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
                CurrencyAccount wildTagAccount = accounts.iterator().next();
                if(resultAmount.compareTo(BigDecimal.ZERO) > 0) {
                    em.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(wildTagExpensesForTag)));
                } else {
                    em.merge(wildTagAccount.setBalance(wildTagAccount.getBalance().add(transaction.getAmount())));
                    resultAmount = BigDecimal.ZERO;
                }
            }
        }
        if(accountTo == null) {
            //User doesn't have an account for that tag
            //check if there exists a WILDTAG account for that user and that CurrencyCode
            if(!Tag.WILDTAG.equals(transaction.getTag().getName()))
                currencyAccountService.checkWildtagUserAccountForCurrency(transaction.getToUser(), transaction.getCurrencyCode());
            accountTo = new CurrencyAccount(transaction.getToUser(), resultAmount, transaction.getCurrencyCode(),
                    transaction.getTag());
            em.persist(accountTo);
            log.info("new CurrencyAccount: " + accountTo.getId() + " - for IBAN:" + transaction.getToUserIBAN() +
                    " -  tag:" + accountTo.getTag().getName() + " - amount:" + accountTo.getBalance());
        } else {
            em.merge(accountTo.setBalance(accountTo.getBalance().add(resultAmount)));
            log.info("updateUserAccountTo - account id: " + accountTo.getId() + " - balance: " +
                    accountTo.getBalance() + " - LastUpdated: " + accountTo.getLastUpdated());
        }
        return accountTo;
    }

    private synchronized void updateUserAccountFrom(Transaction transaction) throws ValidationException {
        if(transaction.getAccountFromMovements() == null)
            throw new ValidationException("Transaction without accountFromMovements");
        for(Map.Entry<CurrencyAccount, BigDecimal> entry: transaction.getAccountFromMovements().entrySet()) {
            CurrencyAccount currencyAccount = entry.getKey();
            em.merge(currencyAccount.setBalance(currencyAccount.getBalance().subtract(entry.getValue())));
            log.info("updateUserAccountFrom - account id: " + currencyAccount.getId() + " - balance: " +
                    currencyAccount.getBalance() + " - LastUpdated: " + currencyAccount.getLastUpdated());
        }
    }

    public void updateCurrencyAccounts(Transaction transaction) throws ValidationException {
        if(transaction.getId() == null)
            em.persist(transaction);
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
                                currency.getTag());
                    }
                    Currency leftOver = transaction.getCurrencyBatch().getLeftOver();
                    if(leftOver != null) balancesBean.updateTagBalance(leftOver.getAmount(), leftOver.getCurrencyCode(),
                            leftOver.getTag());
                    break;
                case FROM_BANK:
                    updateUserAccountTo(transaction);
                    break;
                case CURRENCY_CHANGE:
                    for(Currency currency : transaction.getCurrencyBatch().getValidatedCurrencySet()) {
                        balancesBean.updateTagBalance(currency.getAmount().negate(), currency.getCurrencyCode(),
                                currency.getTag());
                    }
                    Currency changeLeftOver = transaction.getCurrencyBatch().getLeftOver();
                    if(changeLeftOver != null) balancesBean.updateTagBalance(changeLeftOver.getAmount(),
                            changeLeftOver.getCurrencyCode(), changeLeftOver.getTag());
                    Currency currencyChange = transaction.getCurrencyBatch().getCurrencyChange();
                    balancesBean.updateTagBalance(currencyChange.getAmount(),
                            currencyChange.getCurrencyCode(), currencyChange.getTag());
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
    public BigDecimal checkWildTagExpensesForTag(User user, Tag tag, CurrencyCode currencyCode) {
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        Map<CurrencyCode, Map<String, BigDecimal>> balancesFrom =
                BalanceUtils.getBalancesFrom(getTransactionFromList(user, timePeriod));
        Map<CurrencyCode, Map<String, IncomesDto>> balancesTo = BalanceUtils.getBalancesTo(getTransactionToList(user, timePeriod));
        if(balancesFrom.get(currencyCode) == null) return BigDecimal.ZERO;
        BigDecimal expendedForTagVS = balancesFrom.get(currencyCode).get(tag.getName());
        if(expendedForTagVS == null || BigDecimal.ZERO.compareTo(expendedForTagVS) == 0) return BigDecimal.ZERO;
        BigDecimal totalIncomesForTagVS = balancesTo.get(currencyCode).get(tag.getName()).getTotal();
        if(totalIncomesForTagVS.compareTo(expendedForTagVS) < 0)
            return expendedForTagVS.subtract(totalIncomesForTagVS);
        else
            return BigDecimal.ZERO;
    }

    public TransactionDto getTransactionDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto(transaction);
        dto.setDescription(getTransactionTypeDescription(transaction.getType().toString()));
        if(transaction.getTag() != null) {
            String tagName = Tag.WILDTAG.equals(transaction.getTag().getName())? Messages.currentInstance().get("wildTagLbl")
                    .toUpperCase(): transaction.getTag().getName();
            dto.setTags(Sets.newHashSet(tagName));
        }
        return dto;
    }

    public String getTransactionTypeDescription(String transactionType) {
        String typeDescription;
        switch(transactionType) {
            case "CURRENCY_REQUEST":
                typeDescription = Messages.currentInstance().get("currencyRequestLbl");
                break;
            case "CURRENCY_SEND":
                typeDescription = Messages.currentInstance().get("currencySendLbl");
                break;
            case "FROM_BANK":
                typeDescription = Messages.currentInstance().get("bankInputLbl");
                break;
            default: typeDescription = transactionType;
        }
        return typeDescription;
    }

    public List<Transaction> getTransactionFromList(User fromUser, Interval timePeriod) {
        List<Transaction> transactionList = em.createNamedQuery(
                Transaction.FIND_USER_TRANS_FROM_BY_FROM_USER_AND_STATE_AND_DATE_CREATED_AND_IN_LIST)
                .setParameter("fromUser", fromUser).setParameter("state", Transaction.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("inList", Arrays.asList(Transaction.Type.CURRENCY_REQUEST, Transaction.Type.FROM_USER)).getResultList();
        return transactionList;
    }

    public List<Transaction> getTransactionToList(User toUser, Interval timePeriod) {
        List<Transaction> transactionList = em.createNamedQuery(
                Transaction.FIND_TRANS_BY_TO_USER_AND_STATE_AND_DATE_CREATED_BETWEEN)
                .setParameter("toUser", toUser).setParameter("state", Transaction.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo()).getResultList();
        return transactionList;
    }

}