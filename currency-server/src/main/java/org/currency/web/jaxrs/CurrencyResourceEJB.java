package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.currency.web.ejb.CurrencyEJB;
import org.currency.web.ejb.TransactionEJB;
import org.currency.web.ejb.UserEJB;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.CurrencyIssuedDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/currency")
public class CurrencyResourceEJB {

    private static final Logger log = Logger.getLogger(CurrencyResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private UserEJB userBean;
    @Inject private TransactionEJB transactionBean;
    @Inject private ConfigCurrencyServer config;
    @Inject private SignatureServiceEJB signatureService;
    @Inject private CurrencyEJB currencyBean;

    @Path("/issuedLog")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object issuedLog(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse res)
            throws IOException, ServletException {
        //TODO get reference to currency logging file and render file content as JSON
        return Response.ok().build();
    }

    @Path("/requestLog")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object processReques(@Context HttpServletRequest req, @Context HttpServletResponse res) throws IOException, ServletException {
        //TODO get reference to currency logging file and render file content as JSON
        return Response.ok().build();
    }


    @Path("/revocationHash/state") @GET
    public Response state(@Context HttpServletRequest req, @Context HttpServletResponse res, String revocationHash)
            throws Exception {
        List<org.votingsystem.model.currency.Currency> currencyList = em.createQuery(
                "select c from Currency c where c.revocationHash =:revocationHash")
                .setParameter("revocationHash", revocationHash).getResultList();
        if(currencyList.isEmpty()) return Response.status(ResponseDto.SC_NOT_FOUND).entity(
                Messages.currentInstance().get("currencyNotFoundErrorMsg"))
                .type(javax.ws.rs.core.MediaType.TEXT_PLAIN + ";charset=utf-8").build();
        org.votingsystem.model.currency.Currency currency = currencyList.iterator().next();
        CurrencyStateDto currencyStateDto = new CurrencyStateDto(currency);
        if(currency.getCurrencyBatch() != null) {
            currencyList = em.createQuery("select c from Currency c where c.currencyBatch =:currencyBatch")
                    .setParameter("currencyBatch", currency.getCurrencyBatch()).getResultList();
            currencyStateDto.setBatchResponseCerts(currencyList);
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(currencyStateDto)).type(MediaType.JSON).build();
    }

    @Path("/bundle-state")
    @POST @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response bundleState(Set<String> hashSet) throws Exception {
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(currencyBean.checkBundleState(hashSet))).build();
    }

    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    @Path("/issued") @Transactional
    public Response issued() throws JsonProcessingException {
        Map<CurrencyCode, CurrencyIssuedDto> result = new HashMap<>();
        for(CurrencyCode currencyCode :CurrencyCode.values()) {
            result.put(currencyCode, getCurrencyIssuedDto(currencyCode));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(result)).build();
    }

    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    @Path("/issued/currencyCode/{currencyCode}") @Transactional
    public Response currencyIssued(@PathParam("currencyCode") String currencyCode) throws JsonProcessingException {
        CurrencyIssuedDto currencyIssuedDto = getCurrencyIssuedDto(CurrencyCode.valueOf(currencyCode));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(currencyIssuedDto)).build();
    }

    private CurrencyIssuedDto getCurrencyIssuedDto(CurrencyCode currencyCode) {
        List<org.votingsystem.model.currency.Currency.State> inState = Arrays.asList(
                Currency.State.OK, Currency.State.EXPENDED, Currency.State.LAPSED);
        Query query = em.createQuery("select SUM(c.amount), c.currencyCode, c.state from Currency c " +
                "where c.state in :inState and c.currencyCode =:currencyCode group by c.currencyCode, c.state")
                .setParameter("inState", inState)
                .setParameter("currencyCode", currencyCode);
        List<Object[]> resultList = query.getResultList();
        CurrencyIssuedDto resultDto = new CurrencyIssuedDto();
        for(Object[] result : resultList) {
            resultDto.addCurrency((BigDecimal) result[0], (CurrencyCode) result[1], (Currency.State) result[2]);
        }
        return resultDto;
    }

}
