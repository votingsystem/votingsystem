package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyIssuedDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.currency.ejb.CurrencyBean;
import org.votingsystem.web.currency.ejb.TransactionVSBean;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/currency")
public class CurrencyResource {

    private static final Logger log = Logger.getLogger(CurrencyResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject UserVSBean userVSBean;
    @Inject TransactionVSBean transactionVSBean;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject CurrencyBean currencyBean;

    @Path("/request") @GET
    public Object request(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        context.getRequestDispatcher("/currency/request.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/issuedLog")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object issuedLog(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        //TODO get reference to currency logging file and render file content as JSON
        return Response.ok().build();
    }

    @Path("/requestLog")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object processReques(@Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        //TODO get reference to currency logging file and render file content as JSON
        return Response.ok().build();
    }

    @Transactional
    @Path("/hash/{hashCertVSHex}/state") @GET
    public Response state(@PathParam("hashCertVSHex") String hashCertVSHex) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        String hashCertVSBase64 = new String(hexConverter.unmarshal(hashCertVSHex));
        Query query = dao.getEM().createQuery("select c from Currency c where c.hashCertVS =:hashCertVS")
                .setParameter("hashCertVS", hashCertVSBase64);
        Currency currency = dao.getSingleResult(Currency.class, query);
        if(currency == null) return Response.status(ResponseVS.SC_NOT_FOUND).entity(
                messages.get("currencyNotFoundErrorMsg")).type(MediaType.TEXT_PLAIN + ";charset=utf-8").build();
        CurrencyStateDto currencyStateDto = new CurrencyStateDto(currency);
        if(currency.getCurrencyBatch() != null) {
            query = dao.getEM().createQuery("select c from Currency c where c.currencyBatch =:currencyBatch")
                    .setParameter("currencyBatch", currency.getCurrencyBatch());
            currencyStateDto.setBatchResponseCerts(query.getResultList());
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(currencyStateDto)).type(MediaTypeVS.JSON).build();
    }

    @Path("/bundleState")
    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response bundleState(List<String> hashCertVSList) throws Exception {
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(currencyBean.checkBundleState(hashCertVSList))).build();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("/issued/currencyCode/{currencyCode}") @Transactional
    public Response currencyIssued(@PathParam("currencyCode") String currencyCode, @Context HttpServletRequest req,
               @Context HttpServletResponse resp, @Context ServletContext context) throws IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<Currency.State> inState = Arrays.asList(Currency.State.OK, Currency.State.EXPENDED, Currency.State.LAPSED,
                Currency.State.ERROR);
        Query query = dao.getEM().createQuery("select SUM(c.amount), tag, c.currencyCode, c.state from Currency c " +
                "JOIN c.tagVS tag where c.state in :inState " +
                "and c.currencyCode =:currencyCode group by tag, c.currencyCode, c.state").setParameter("inState", inState)
                .setParameter("currencyCode", currencyCode);
        List<Object[]> resultList = query.getResultList();
        List<TagVSDto> okListDto = new ArrayList<>();
        List<TagVSDto> expendedListDto = new ArrayList<>();
        List<TagVSDto> lapsedListDto = new ArrayList<>();
        List<TagVSDto> errorListDto = new ArrayList<>();
        for(Object[] result : resultList) {
            Currency.State state = (Currency.State) result[3];
            TagVSDto tagVSDto = TagVSDto.CURRENCY_DATA((BigDecimal) result[0], (String) result[2], (TagVS) result[1]);
            switch (state) {
                case EXPENDED:
                    expendedListDto.add(tagVSDto);
                    break;
                case OK:
                    okListDto.add(tagVSDto);
                    break;
                case LAPSED:
                    lapsedListDto.add(tagVSDto);
                    break;
                case ERROR:
                    errorListDto.add(tagVSDto);
                    break;
            }
        }
        CurrencyIssuedDto currencyIssuedDto = new CurrencyIssuedDto(okListDto, expendedListDto, lapsedListDto, errorListDto);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(currencyIssuedDto)).build();
        } else {
            req.setAttribute("currencyIssuedDto", JSON.getMapper().writeValueAsString(currencyIssuedDto));
            context.getRequestDispatcher("/currency/currencyIssued.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

}
