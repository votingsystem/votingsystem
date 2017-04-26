package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.FileUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
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
        Query query = em.createQuery("select SUM(c.balance), c.currencyCode from CurrencyAccount c where c.state =:state " +
                "group by c.currencyCode").setParameter("state", CurrencyAccount.State.ACTIVE);
         List<Object[]> resultList = query.getResultList();
        for(Object[] result : resultList) {
            log.info("" + result[0] + ((Tag)result[1]).getName() + result[2]);
        }
        return Response.ok().entity("OK").build();
    }


    @Transactional
    @Path("/cert-by-hash") @POST
    @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response uuid(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        String uuid = FileUtils.getStringFromStream(req.getInputStream());
        List<Bank> bankList = em.createQuery(
                "select b from Certificate c JOIN c.signer b where c.UUID=:UUID")
                .setParameter("UUID", uuid).getResultList();
        if (bankList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cert with uuid: " + uuid +
                    " not found").build();
        }
        User bank = bankList.iterator().next();
        return Response.ok().entity("Signer UUID: " + bank.getUUID() + " - simple name: " +
                bank.getClass().getSimpleName()).build();
    }
}