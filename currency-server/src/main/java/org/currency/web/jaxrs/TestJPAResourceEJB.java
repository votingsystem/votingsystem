package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test-jpa")
public class TestJPAResourceEJB {

    private static final Logger log = Logger.getLogger(TestJPAResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;


    @GET @Path("/sum-balance")
    public Response sumBalance(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws JsonProcessingException, ValidationException {
        Query query = em.createQuery("select SUM(c.balance), tag, c.currencyCode from CurrencyAccount c JOIN c.tag tag where c.state =:state " +
                "group by tag, c.currencyCode").setParameter("state", CurrencyAccount.State.ACTIVE);
         List<Object[]> resultList = query.getResultList();
        for(Object[] result : resultList) {
            log.info("" + result[0] + ((Tag)result[1]).getName() + result[2]);
        }

        return Response.ok().entity("OK").build();
    }

    @GET @Path("/transactions")
    public Response transactions() throws Exception {
        Query query = em.createQuery("select t from Transaction t where t.type  in :inList")
                .setParameter("inList", Arrays.asList(Transaction.Type.CURRENCY_PERIOD_INIT,
                Transaction.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED));
        List<Transaction> resultList =  query.getResultList();
        for(Transaction transaction : resultList) {
            em.remove(transaction);
        }
        return Response.ok().entity("OK").build();
    }

}