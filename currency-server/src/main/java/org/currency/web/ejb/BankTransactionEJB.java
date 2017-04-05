package org.currency.web.ejb;

import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.SignatureException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BankTransactionEJB {

    private static Logger log = Logger.getLogger(BankTransactionEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private TransactionEJB transactionBean;
    @Inject private CurrencySignatureEJB signatureService;

    public ResultListDto<TransactionDto> processTransactionFromBank(TransactionDto request, Tag tag)
            throws ValidationException, SignatureException {
        validateRequest(request);
        List<Bank> userList = em.createNamedQuery(Bank.FIND_USER_BY_NIF).setParameter(
                "numId", request.getSigner().getNumId()).getResultList();
        if(userList.isEmpty()) throw new ValidationException(
                Messages.currentInstance().get("bankPrivilegesErrorMsg", request.getOperation().toString()));
        Bank bank = userList.iterator().next();

        SignedDocument signedDocument = request.getSignedDocument();
        Transaction transaction = Transaction.FROM_BANK(bank, request.getFromUserIBAN(),
                request.getFromUserName(), request.getReceptor(), request.getAmount(), request.getCurrencyCode(),
                request.getSubject(), request.getValidTo().toLocalDateTime(), signedDocument, tag);
        em.persist(transaction);

        signatureService.addReceipt(SignedDocumentType.TRANSACTION_FROM_BANK_RECEIPT, signedDocument);

        transactionBean.updateCurrencyAccounts(transaction);
        log.info("Bank: " + bank.getId() + " - to user: " + request.getReceptor().getIBAN());
        return new ResultListDto(Arrays.asList(new TransactionDto(transaction)), 0, 1, 1L);
    }


    public TransactionDto validateRequest(TransactionDto dto) throws ValidationException {
        if(CurrencyOperation.TRANSACTION_FROM_BANK != dto.getOperation())
            throw new ValidationException(
                    "Operation expected: 'TRANSACTION_FROM_BANK' - found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationException(
                "'TRANSACTION_FROM_BANK' only allows one receptor. Receptors:" + dto.getToUserIBAN());
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN)
                .setParameter("IBAN", dto.getToUserIBAN().iterator().next()).getResultList();
        if(userList.isEmpty())
            throw new ValidationException("invalid 'toUserIBAN':" + dto.getToUserIBAN().iterator().next());
        dto.setReceptor(userList.iterator().next());
        return dto;
    }

}