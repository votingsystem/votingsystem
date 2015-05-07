package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;

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
public class TransactionVSUserVSBean {

    private static Logger log = Logger.getLogger(TransactionVSUserVSBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject WalletBean walletBean;


    public ResultListDto<TransactionVSDto> processTransactionVS(TransactionVSDto request) throws Exception {
        validateRequest(request);
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                request.getSigner().getIBAN(), request.getTag(), request.getAmount(), request.getCurrencyCode());
        //Transactions from users doesn't need parent transaction
        TransactionVS transactionVS = dao.persist(TransactionVS.USERVS(request.getSigner(), request.getReceptor(),
                request.getType(), accountFromMovements, request.getAmount(), request.getCurrencyCode(),
                request.getSubject(), request.getValidTo(), request.getTransactionVSSMIME(), request.getTag()));
        String fromUser = config.getServerName();
        String toUser = request.getSigner().getNif();
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(fromUser, toUser,
                request.getTransactionVSSMIME().getSMIME(), request.getTransactionVSSMIME().getSMIME().getSubject());
        request.getTransactionVSSMIME().setSMIME(receipt);
        dao.merge(request.getTransactionVSSMIME().refresh());
        TransactionVSDto dto = new TransactionVSDto(transactionVS);
        dto.setMessageSMIME(Base64.getUrlEncoder().encodeToString(request.getTransactionVSSMIME().getContent()));
        List<TransactionVSDto> listDto = Arrays.asList(dto);
        ResultListDto<TransactionVSDto> resultListDto = new ResultListDto<>(listDto, request.getOperation());
        return resultListDto;
    }


    public TransactionVSDto validateRequest(TransactionVSDto dto) throws ValidationExceptionVS {
        if(TypeVS.FROM_BANKVS != dto.getOperation()) throw new ValidationExceptionVS(
                "operation expected: 'FROM_BANKVS' - operation found: " + dto.getOperation());
        if(dto.getToUserIBAN().size() != 1) throw new ValidationExceptionVS(
                "there can be only one receptor. request.toUserIBAN: " + dto.getToUserIBAN().get(0));
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", dto.getToUserIBAN().get(0));
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        if(toUserVS == null) throw new ValidationExceptionVS("invalid 'toUserIBAN':" + dto.getToUserIBAN().get(0));
        //this is to get data from banks clients
        if(dto.getFromUserIBAN() == null)  throw new ValidationExceptionVS("missing param 'fromUserIBAN'");
        if(dto.getFromUser() == null)  throw new ValidationExceptionVS("missing param 'fromUser'");
        return dto;
    }

}
