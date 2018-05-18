package org.currency.web.ejb;

import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.SignatureException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.MessageFormat;
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

    public ResultListDto<TransactionDto> processTransactionFromBank(TransactionDto transactionDto)
            throws ValidationException, SignatureException {
        if(CurrencyOperation.TRANSACTION_FROM_BANK != transactionDto.getOperation().getCurrencyOperationType())
            throw new ValidationException(
                    "Operation expected: 'TRANSACTION_FROM_BANK' - found: " + transactionDto.getOperation());
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN)
                .setParameter("IBAN", transactionDto.getToUserIBAN()).getResultList();
        if(userList.isEmpty())
            throw new ValidationException("invalid receptor IBAN:" + transactionDto.getToUserIBAN());
        transactionDto.setReceptor(userList.iterator().next());
        String bankCertificateHash = null;
        try {
            bankCertificateHash = CertificateUtils.getHash(transactionDto.getSigner().getX509Certificate());
        }catch (Exception ex) {
            throw new ValidationException("Error with bank certificate");
        }
        List<Bank> bankList = em.createQuery(
                "select b from Certificate c JOIN c.signer b where c.UUID=:UUID")
                .setParameter("UUID", bankCertificateHash).getResultList();
        if(bankList.isEmpty())
            throw new ValidationException(MessageFormat.format("User ''{0}'' is not an authorized bank",
                    transactionDto.getSigner().getX509Certificate().getSubjectDN()));
        Bank bank = null;
        try {
            bank = bankList.iterator().next();
        } catch (Exception ex) {
            throw new ValidationException(MessageFormat.format("User ''{0}'' is not an authorized bank",
                    transactionDto.getSigner().getX509Certificate().getSubjectDN()));
        }
        SignedDocument signedDocument = transactionDto.getSignedDocument();
        Transaction transaction = Transaction.FROM_BANK(bank, transactionDto.getFromUserIBAN(),
                transactionDto.getFromUserName(), transactionDto.getReceptor(), transactionDto.getAmount(),
                transactionDto.getCurrencyCode(),
                transactionDto.getSubject(), signedDocument);

        em.persist(transaction);

        transactionDto.getSignedDocument().setOperationType(OperationType.TRANSACTION_FROM_BANK);
        signatureService.addReceipt(OperationType.TRANSACTION_FROM_BANK_RECEIPT, signedDocument);


        transactionBean.updateCurrencyAccounts(transaction);
        log.info("Bank: " + bank.getId() + " - to user: " + transactionDto.getReceptor().getIBAN());
        return new ResultListDto(Arrays.asList(new TransactionDto(transaction)), 0, 1, 1L);
    }

}