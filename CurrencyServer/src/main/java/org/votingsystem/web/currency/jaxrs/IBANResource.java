package org.votingsystem.web.currency.jaxrs;

import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.web.currency.ejb.TransactionVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/IBAN")
public class IBANResource {

    private static final Logger log = Logger.getLogger(IBANResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TransactionVSBean transactionVSBean;

    @Path("/from/{IBANCode}") //old_url -> /IBAN/from/$IBANCode
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object from(@PathParam("IBANCode") String IBANCode, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Iban iban = Iban.valueOf(IBANCode);
        List result = new ArrayList<>();

        if(iban.getBankCode().equals(config.getBankCode()) && iban.getBranchCode().equals(config.getBranchCode())) {
            log.log(Level.FINE, "VotingSystem IBAN");
            Query query = dao.getEM().createQuery("select t from TransactionVS t where t.fromUserVS.IBAN =:IBAN")
                    .setParameter("IBAN", iban.toString());
            List<TransactionVS> transactionVSList = query.getResultList();
            for(TransactionVS transactionVS : transactionVSList) {
                result.add(transactionVSBean.getTransactionDto(transactionVS));
            }
        } else {
            log.log(Level.FINE, "external IBAN");
            Query query = dao.getEM().createQuery("select t from TransactionVS t where t.fromUserIBAN =:IBAN")
                    .setParameter("IBAN", iban.toString());
            List<TransactionVS> transactionVSList = query.getResultList();
            for(TransactionVS transactionVS : transactionVSList) {
                result.add(transactionVSBean.getTransactionDto(transactionVS));
            }
        }
        return result;
    }


    @Path("/testExternal/{id}") @GET
    public Response testExternal(@PathParam("id") long id) {
        String accountNumberStr = String.format("%010d", id);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode("7777").branchCode("7777")
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return Response.ok().entity(iban.toString()).build();
    }

}