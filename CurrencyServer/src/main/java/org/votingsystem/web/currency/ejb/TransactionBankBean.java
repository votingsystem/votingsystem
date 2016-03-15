package org.votingsystem.web.currency.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionBankBean {

    private static Logger log = Logger.getLogger(TransactionBankBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionBean transactionBean;
    @Inject CMSBean cmsBean;

    public ResultListDto<TransactionDto> processTransaction(TransactionDto request, TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        validateRequest(request);
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getSigner().getNif());
        Bank bank = dao.getSingleResult(Bank.class, query);
        if(bank == null) throw new ExceptionVS(messages.get("bankPrivilegesErrorMsg", request.getOperation().toString()));
        Transaction transaction = dao.persist(Transaction.FROM_BANK(bank, request.getFromUserIBAN(),
                request.getFromUserName(), request.getReceptor(), request.getAmount(), request.getCurrencyCode(),
                request.getSubject(), request.getValidTo(), request.getCmsMessage_DB(), tagVS));
        CMSSignedMessage receipt = cmsBean.addSignature(request.getCmsMessage_DB().getCMS());
        dao.merge(request.getCmsMessage_DB().setType(TypeVS.FROM_BANK).setCMS(receipt));
        transactionBean.updateCurrencyAccounts(transaction);
        log.info("Bank: " + bank.getId() + " - to user: " + request.getReceptor().getId());
        return new ResultListDto(Arrays.asList(new TransactionDto(transaction)), 0, 1, 1L);
    }


    public TransactionDto validateRequest(TransactionDto dto) throws ValidationException {
        if(TypeVS.FROM_BANK != dto.getOperation())
            throw new ValidationException(
                    "peration expected: 'FROM_BANK' - operation found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationException(
                "there can be only one receptor. request.toUserIBAN:" + dto.getToUserIBAN());
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dto.getToUserIBAN().iterator().next());
        User toUser = dao.getSingleResult(User.class, query);
        if(toUser == null) throw new ValidationException("invalid 'toUserIBAN':" + dto.getToUserIBAN().iterator().next());
        dto.setReceptor(toUser);
        return dto;
    }

}