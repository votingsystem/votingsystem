package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
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
public class TransactionVSBankVSBean {

    private static Logger log = Logger.getLogger(TransactionVSBankVSBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;

    public ResultListDto<TransactionVSDto> processTransactionVS(TransactionVSDto request) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        validateRequest(request);
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getSigner().getNif());
        BankVS bankVS = dao.getSingleResult(BankVS.class, query);
        if(bankVS == null) throw new ExceptionVS(messages.get("bankVSPrivilegesErrorMsg", request.getOperation().toString()));
        TransactionVS transactionParent = dao.persist(TransactionVS.BANKVS_PARENT(bankVS, request.getFromUserIBAN(),
                request.getFromUser(), request.getAmount(), request.getCurrencyCode(), request.getSubject(),
                request.getValidTo(), request.getTransactionVSSMIME(), request.getTag()));
        TransactionVS triggeredTransaction = dao.persist(TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.getAmount(), request.getReceptor(), request.getReceptor().getIBAN()));
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(request.getTransactionVSSMIME().getUserVS().getNif(),
                request.getTransactionVSSMIME().getSMIME(), messages.get("bankVSInputLbl"));
        receipt.setHeader("TypeVS", TypeVS.FROM_BANKVS.toString());
        dao.merge(request.getTransactionVSSMIME().setSMIME(receipt));
        transactionVSBean.newTransactionVS(transactionParent, triggeredTransaction);
        log.info("BankVS: " + bankVS.getId() + " - to user: " + request.getReceptor().getId());
        return new ResultListDto(Arrays.asList(new TransactionVSDto(triggeredTransaction)), 0, 1, 1L);
    }


    public TransactionVSDto validateRequest(TransactionVSDto dto) throws ValidationExceptionVS {
        if(TypeVS.FROM_BANKVS != dto.getOperation())
            throw new ValidationExceptionVS(
                    "peration expected: 'FROM_BANKVS' - operation found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationExceptionVS(
                "there can be only one receptor. request.toUserIBAN:" + dto.getToUserIBAN());
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dto.getToUserIBAN().iterator().next());
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        if(toUserVS == null) throw new ValidationExceptionVS("invalid 'toUserIBAN':" + dto.getToUserIBAN().iterator().next());
        dto.setReceptor(toUserVS);
        return dto;
    }

}