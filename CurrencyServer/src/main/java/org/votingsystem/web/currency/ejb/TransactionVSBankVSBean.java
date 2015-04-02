package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.BankVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class TransactionVSBankVSBean {

    private static Logger log = Logger.getLogger(TransactionVSBankVSBean.class.getSimpleName());

    @Inject MessagesBean messages;
    @Inject ConfigVS config;
    @Inject DAOBean dao;

    public String processTransactionVS(TransactionVSBean.TransactionVSRequest request) throws ExceptionVS {
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.fromUserVS.getNif());
        BankVS bankVS = dao.getSingleResult(BankVS.class, query);
        if(bankVS == null) throw new ExceptionVS(messages.get("bankVSPrivilegesErrorMsg", request.operation.toString()));
        TransactionVS transactionParent = dao.persist(TransactionVS.BANKVS_PARENT(bankVS, request.fromUserIBAN, request.fromUser,
                request.amount, request.currencyCode, request.subject, request.validTo, request.messageSMIME, request.tag));
        TransactionVS triggeredTransaction = dao.persist(TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.getAmount(), request.toUserVS, request.toUserVS.getIBAN()));
        log.info("BankVS: " + bankVS.getId() + " - to user: " + request.toUserVS.getId());
        return "Transaction OK";
    }
}
