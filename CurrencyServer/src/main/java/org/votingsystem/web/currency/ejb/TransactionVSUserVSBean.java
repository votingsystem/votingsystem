package org.votingsystem.web.currency.ejb;

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


    public SMIMEMessage processTransactionVS(TransactionVSBean.TransactionVSRequest request) throws Exception {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                request.fromUserVS.getIBAN(), request.tag, request.amount, request.currencyCode);
        //Transactions from users doesn't need parent transaction
        TransactionVS transactionVS = dao.persist(TransactionVS.USERVS(request.fromUserVS, request.toUserVS,
                request.transactionType, accountFromMovements, request.amount, request.currencyCode, request.subject,
                request.validTo, request.messageSMIME, request.tag));
        String fromUser = config.getServerName();
        String toUser = request.fromUserVS.getNif();
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(fromUser, toUser,
                request.messageSMIME.getSMIME(), request.messageSMIME.getSMIME().getSubject());
        request.messageSMIME.setSMIME(receipt);
        em.merge(request.messageSMIME.refresh());
        log.info("operation: " + request.operation.toString() + " - transactionVS: " + transactionVS.getId());
        return receipt;
    }

}
