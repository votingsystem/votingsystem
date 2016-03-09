package org.votingsystem.web.currency.ejb;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
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

    private static Logger log = Logger.getLogger(TransactionVSBankVSBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject CMSBean cmsBean;

    public ResultListDto<TransactionVSDto> processTransactionVS(TransactionVSDto request, TagVS tagVS) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        validateRequest(request);
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getSigner().getNif());
        BankVS bankVS = dao.getSingleResult(BankVS.class, query);
        if(bankVS == null) throw new ExceptionVS(messages.get("bankVSPrivilegesErrorMsg", request.getOperation().toString()));
        TransactionVS transactionVS = dao.persist(TransactionVS.FROM_BANKVS(bankVS, request.getFromUserIBAN(),
                request.getFromUser(), request.getReceptor(), request.getAmount(), request.getCurrencyCode(),
                request.getSubject(), request.getValidTo(), request.getCmsMessage_DB(), tagVS));
        CMSSignedMessage receipt = cmsBean.addSignature(request.getCmsMessage_DB().getCMS());
        dao.merge(request.getCmsMessage_DB().setType(TypeVS.FROM_BANKVS).setCMS(receipt));
        transactionVSBean.updateCurrencyAccounts(transactionVS);
        log.info("BankVS: " + bankVS.getId() + " - to user: " + request.getReceptor().getId());
        return new ResultListDto(Arrays.asList(new TransactionVSDto(transactionVS)), 0, 1, 1L);
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