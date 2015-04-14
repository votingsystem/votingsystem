package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionVSUserVSBean {

    private static Logger log = Logger.getLogger(TransactionVSUserVSBean.class.getSimpleName());

    @PersistenceContext private EntityManager em;
    @Inject ConfigVS config;
    @Inject
    DAOBean dao;
    @Inject
    SignatureBean signatureBean;
    @Inject WalletBean walletBean;


    public SMIMEMessage processTransactionVS(TransactionVSDto request) throws Exception {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
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
        em.merge(request.getTransactionVSSMIME().refresh());
        log.info("operation: " + request.getOperation() + " - transactionVS: " + transactionVS.getId());
        return receipt;
    }

}
