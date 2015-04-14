package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
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

    public String processTransactionVS(TransactionVSDto request) throws ExceptionVS {
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getSigner().getNif());
        BankVS bankVS = dao.getSingleResult(BankVS.class, query);
        if(bankVS == null) throw new ExceptionVS(messages.get("bankVSPrivilegesErrorMsg", request.getOperation().toString()));
        TransactionVS transactionParent = dao.persist(TransactionVS.BANKVS_PARENT(bankVS, request.getFromUserIBAN(),
                request.getFromUser(), request.getAmount(), request.getCurrencyCode(), request.getSubject(),
                request.getValidTo(), request.getTransactionVSSMIME(), request.getTag()));
        TransactionVS triggeredTransaction = dao.persist(TransactionVS.generateTriggeredTransaction(
                transactionParent, transactionParent.getAmount(), request.getReceptor(), request.getReceptor().getIBAN()));
        log.info("BankVS: " + bankVS.getId() + " - to user: " + request.getReceptor().getId());
        return "Transaction OK";
    }
}
