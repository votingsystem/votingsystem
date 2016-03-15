package org.votingsystem.web.currency.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
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
public class TransactionUserBean {

    private static Logger log = Logger.getLogger(TransactionUserBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject WalletBean walletBean;
    @Inject
    TransactionBean transactionBean;


    public ResultListDto<TransactionDto> processTransaction(TransactionDto request, TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        validateRequest(request);
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                request.getSigner().getIBAN(), tagVS, request.getAmount(), request.getCurrencyCode());
        //Transactions from users doesn't need parent transaction
        Transaction transaction = dao.persist(Transaction.USER(request.getSigner(), request.getReceptor(),
                request.getType(), accountFromMovements, request.getAmount(), request.getCurrencyCode(),
                request.getSubject(), request.getValidTo(), request.getCmsMessage_DB(), tagVS));
        transactionBean.updateCurrencyAccounts(transaction);
        CMSSignedMessage receipt = cmsBean.addSignature(request.getCmsMessage_DB().getCMS());
        dao.merge(request.getCmsMessage_DB().setCMS(receipt));
        TransactionDto dto = new TransactionDto(transaction);
        dto.setCmsMessagePEM(Base64.getEncoder().encodeToString(request.getCmsMessage_DB().getContentPEM()));
        List<TransactionDto> listDto = Arrays.asList(dto);
        ResultListDto<TransactionDto> resultListDto = new ResultListDto<>(listDto, request.getOperation());
        resultListDto.setStatusCode(ResponseVS.SC_OK);
        resultListDto.setMessage(messages.get("transactionFromUserOKMsg", request.getAmount() +
                " " + request.getCurrencyCode(), request.getReceptor().getName()));
        return resultListDto;
    }

    public TransactionDto validateRequest(TransactionDto dto) throws ValidationException {
        if(TypeVS.FROM_USER != dto.getOperation()) throw new ValidationException(
                "operation expected: 'FROM_USER' - operation found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationException(
                "there can be only one receptor. request.toUserIBAN: " + dto.getToUserIBAN().iterator().next());
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dto.getToUserIBAN().iterator().next());
        User toUser = dao.getSingleResult(User.class, query);
        if(toUser == null) throw new ValidationException("invalid 'toUserIBAN':" + dto.getToUserIBAN().iterator().next());
        dto.setReceptor(toUser);
        return dto;
    }

}
