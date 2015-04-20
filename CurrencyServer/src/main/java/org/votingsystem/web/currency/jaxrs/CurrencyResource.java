package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.JSON;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.MessagesBean;
import org.votingsystem.web.currency.ejb.CurrencyBean;
import org.votingsystem.web.currency.ejb.TransactionVSBean;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/currency")
public class CurrencyResource {

    private static final Logger log = Logger.getLogger(CurrencyResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject CurrencyResource currencyResource;
    @Inject UserVSBean userVSBean;
    @Inject TransactionVSBean transactionVSBean;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject CurrencyBean currencyBean;
    @Inject MessagesBean messages;

    @Path("/request") @GET
    public Object request(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        context.getRequestDispatcher("/currency/request.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/wallet") @GET
    public Object wallet(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        context.getRequestDispatcher("/currency/wallet.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/issuedLog")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object issuedLog(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        //TODO get reference to currency logging file and render file content as JSON
        return Response.ok().build();
    }

    @Path("/request")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object processReques(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        //TODO get reference to currency logging file and render file content as JSON
        return Response.ok().build();
    }

    @Path("/hash/{hashCertVSHex}/state") @GET
    public Response state(@PathParam("hashCertVSHex") String hashCertVSHex) { // old_url -> /currency/$hashCertVSHex/state
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        String hashCertVSBase64 = new String(hexConverter.unmarshal(hashCertVSHex));
        Query query = dao.getEM().createQuery("select c from Currency c where c.hashCertVS =:hashCertVS")
                .setParameter("hashCertVS", hashCertVSBase64);
        Currency currency = dao.getSingleResult(Currency.class, query);
        if(currency == null) return Response.status(ResponseVS.SC_NOT_FOUND).entity(
                messages.get("currencyNotFoundErrorMsg")).build();
        switch(currency.getState()) {
            case EXPENDED: return Response.status(ResponseVS.SC_CURRENCY_EXPENDED)
                    .entity(messages.get("currencyExpendedShortErrorMsg")).build();
            case OK:
                if(currency.getValidTo().after(new Date())) {
                    return Response.status(ResponseVS.SC_CURRENCY_OK).entity(messages.get("currencyOKMsg")).build();
                } else {
                    dao.merge(currency.setState(Currency.State.LAPSED));
                    return Response.status(ResponseVS.SC_CURRENCY_LAPSED).entity(
                            messages.get("currencyLapsedShortErrorMsg")).build();
                }
            case LAPSED: return Response.status(ResponseVS.SC_CURRENCY_LAPSED).entity(
                    messages.get("currencyLapsedShortErrorMsg")).build();
            default:return Response.status(Response.Status.BAD_REQUEST)
                    .entity("unknown currency state: " + currency.getState()).build();
        }
    }

    @Path("/bundleState")
    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response bundleState(List<String> hashCertVSList) throws JsonProcessingException {
        Map<String, String> result =  currencyBean.checkBundleState(hashCertVSList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(result)).build();
    }

}
