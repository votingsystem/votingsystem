package org.currency.web.ejb;

import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class UserTransactionEJB {

    private static Logger log = Logger.getLogger(UserTransactionEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private WalletEJB walletBean;
    @Inject private TransactionEJB transactionBean;
    @Inject private CurrencySignatureEJB signatureService;

    public ResultListDto<TransactionDto> processTransactionFromUser(TransactionDto request) throws Exception {
        validateRequest(request);
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                request.getSigner().getIBAN(), request.getAmount(), request.getCurrencyCode());
        //Transactions from users doesn't need parent transaction
        SignedDocument signedDocument = request.getSignedDocument();
        Transaction transaction = Transaction.USER(request.getSigner(), request.getReceptor(),
                request.getType(), accountFromMovements, request.getAmount(), request.getCurrencyCode(),
                request.getSubject(), signedDocument);
        em.persist(transaction);
        transactionBean.updateCurrencyAccounts(transaction);
        signatureService.addReceipt(SignedDocumentType.TRANSACTION_FROM_USER_RECEIPT, signedDocument);

        TransactionDto dto = new TransactionDto(transaction);
        dto.setSignedDocumentBase64(Base64.getEncoder().encodeToString(signedDocument.getReceipt().getBody().getBytes()));
        List<TransactionDto> listDto = Arrays.asList(dto);
        ResultListDto<TransactionDto> resultListDto = new ResultListDto<>(listDto, request.getOperation());
        resultListDto.setStatusCode(ResponseDto.SC_OK);
        resultListDto.setMessage(Messages.currentInstance().get("transactionFromUserOKMsg", request.getAmount() +
                " " + request.getCurrencyCode(), request.getReceptor().getName()));
        return resultListDto;
    }

    public TransactionDto validateRequest(TransactionDto dto) throws ValidationException {
        if(CurrencyOperation.TRANSACTION_FROM_USER != dto.getOperation()) throw new ValidationException(
                "Expected operation: 'TRANSACTION_FROM_USER' - found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationException(
                "There can be only one receptor. request.toUserIBAN: " + dto.getToUserIBAN().iterator().next());
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN)
                .setParameter("IBAN", dto.getToUserIBAN().iterator().next()).getResultList();
        if(userList.isEmpty())
            throw new ValidationException("invalid 'toUserIBAN':" + dto.getToUserIBAN().iterator().next());
        dto.setReceptor(userList.iterator().next());
        return dto;
    }

}