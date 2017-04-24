package org.currency.web.ejb;

import org.currency.web.util.AuditLogger;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
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

        AuditLogger.logTransaction(request);

        switch(request.getOperation()) {
            case TRANSACTION_FROM_BANK:
                return bankTransactionService.processTransactionFromBank(request);
            case TRANSACTION_FROM_USER:
                return userTransactionService.processTransactionFromUser(request);
            default:
                throw new ValidationException(
                        Messages.currentInstance().get("unknownTransactionErrorMsg", request.getOperation().toString()));
        }
    }

    private synchronized CurrencyAccount updateUserAccountTo(Transaction transaction) throws ValidationException {
        if(transaction.getToUserIBAN() == null) 
            throw new ValidationException("transaction without toUserIBAN");
        List<CurrencyAccount> accounts = em.createNamedQuery(
                CurrencyAccount.FIND_BY_USER_IBAN_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", transaction.getToUserIBAN())
                .setParameter("currencyCode", transaction.getCurrencyCode())
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        CurrencyAccount accountTo = null;
        if(!accounts.isEmpty())
            accountTo = accounts.iterator().next();
        BigDecimal resultAmount =  transaction.getAmount();
        if(accountTo == null) {
            //TODO
            currencyAccountService.checkUserAccountForCurrency(transaction.getToUser(), transaction.getCurrencyCode());
            accountTo = new CurrencyAccount(transaction.getToUser(), resultAmount, transaction.getCurrencyCode());
            em.persist(accountTo);
            log.info("new CurrencyAccount: " + accountTo.getId() + " - for IBAN:" + transaction.getToUserIBAN() +
                    " - amount:" + accountTo.getBalance());
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
                case FROM_USER:
                    updateUserAccountFrom(transaction);
                    updateUserAccountTo(transaction);
                    break;
                case CURRENCY_REQUEST:
                    updateUserAccountFrom(transaction);
                    balancesBean.updateSystemBalance(transaction.getAmount(), transaction.getCurrencyCode());
                    break;
                case CURRENCY_SEND:
                    updateUserAccountTo(transaction);
                    for(Currency currency : transaction.getCurrencyBatch().getValidatedCurrencySet()) {
                        balancesBean.updateSystemBalance(currency.getAmount().negate(), currency.getCurrencyCode());
                    }
                    Currency leftOver = transaction.getCurrencyBatch().getLeftOver();
                    if(leftOver != null) balancesBean.updateSystemBalance(leftOver.getAmount(), leftOver.getCurrencyCode());
                    break;
                case FROM_BANK:
                    updateUserAccountTo(transaction);
                    break;
                case CURRENCY_CHANGE:
                    for(Currency currency : transaction.getCurrencyBatch().getValidatedCurrencySet()) {
                        balancesBean.updateSystemBalance(currency.getAmount().negate(), currency.getCurrencyCode());
                    }
                    Currency changeLeftOver = transaction.getCurrencyBatch().getLeftOver();
                    if(changeLeftOver != null) balancesBean.updateSystemBalance(changeLeftOver.getAmount(),
                            changeLeftOver.getCurrencyCode());
                    Currency currencyChange = transaction.getCurrencyBatch().getCurrencyChange();
                    balancesBean.updateSystemBalance(currencyChange.getAmount(), currencyChange.getCurrencyCode());
                    break;
                default:
                    if(isParentTransaction) {//Parent transaction, to system before trigger to receptors
                        if(transaction.getType() != Transaction.Type.FROM_BANK) updateUserAccountFrom(transaction);
                        balancesBean.updateSystemBalance(transaction.getAmount(), transaction.getCurrencyCode());
                    } else {
                        updateUserAccountTo(transaction);
                        balancesBean.updateSystemBalance(transaction.getAmount().negate(), transaction.getCurrencyCode());
                        log.info("transaction: " + transaction.getType() + " - " + transaction.getAmount() +  " " +
                                transaction.getCurrencyCode() + " - " + "fromUserIBAN: " + transaction.getFromUserIBAN() +
                                " - toIBAN: " + transaction.getToUser().getIBAN());
                    }
            }
        } else log.log(Level.SEVERE, "Transaction:" + transaction.getId() + " - state:" +
                transaction.getState().toString());
    }

    public TransactionDto getTransactionDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto(transaction);
        dto.setDescription(getTransactionTypeDescription(transaction.getType().toString()));
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