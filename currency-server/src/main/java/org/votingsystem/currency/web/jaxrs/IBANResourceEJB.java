package org.votingsystem.currency.web.jaxrs;

import org.iban4j.Iban;
import org.votingsystem.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.currency.web.ejb.TransactionEJB;
import org.votingsystem.currency.web.util.AuthRole;
import org.votingsystem.model.currency.Transaction;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/IBAN")
public class IBANResourceEJB {

    private static final Logger log = Logger.getLogger(IBANResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private TransactionEJB transactionBean;

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/from/{IBANCode}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object from(@PathParam("IBANCode") String IBANCode, @Context HttpServletRequest req,
                       @Context HttpServletResponse resp) throws Exception {
        Iban iban = Iban.valueOf(IBANCode);
        List result = new ArrayList<>();

        if(iban.getBankCode().equals(config.getBankCode()) && iban.getBranchCode().equals(config.getBranchCode())) {
            log.log(Level.FINE, "VotingSystem IBAN");
            List<Transaction> transactionList = em.createQuery("select t from Transaction t where t.fromUser.IBAN =:IBAN")
                    .setParameter("IBAN", iban.toString()).getResultList();
            for(Transaction transaction : transactionList) {
                result.add(transactionBean.getTransactionDto(transaction));
            }
        } else {
            log.log(Level.FINE, "external IBAN");
            List<Transaction> transactionList = em.createQuery("select t from Transaction t where t.fromUserIBAN =:IBAN")
                    .setParameter("IBAN", iban.toString()).getResultList();
            for(Transaction transaction : transactionList) {
                result.add(transactionBean.getTransactionDto(transaction));
            }
        }
        return result;
    }

}