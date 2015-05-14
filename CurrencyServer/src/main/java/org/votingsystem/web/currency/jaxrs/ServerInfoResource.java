package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyAccountDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.util.JSON;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject TimeStampBean timeStampBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response doGet() throws Exception {
        ActorVS actorVS = new ActorVS();
        actorVS.setType(ActorVS.Type.CURRENCY);
        actorVS.setName(config.getServerName());
        actorVS.setServerURL(config.getContextURL());
        actorVS.setWebSocketURL(config.getWebSocketURL());
        actorVS.setState(ActorVS.State.OK);
        actorVS.setEnvironmentVS(config.getMode());
        actorVS.setDate(new Date());
        actorVS.setTimeStampCertPEM(new String(timeStampBean.getSigningCertPEMBytes()));
        actorVS.setTimeStampServerURL(config.getTimeStampServerURL());
        actorVS.setCertChainPEM(new String(signatureBean.getKeyStorePEMCerts()));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${serverInfo as JSON})"
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(actorVS)).build() ;
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("/accounts") @Transactional
    public Response accounts(@Context HttpServletRequest req, @Context HttpServletResponse resp,
            @Context ServletContext context) throws IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Query query = dao.getEM().createQuery("select c FROM CurrencyAccount c where c.userVS =:userVS and c.state =:state")
                .setParameter("userVS", config.getSystemUser()).setParameter("state", CurrencyAccount.State.ACTIVE);
        List<CurrencyAccount> accountList = query.getResultList();
        List<CurrencyAccountDto> accountListDto = new ArrayList<>();
        for(CurrencyAccount currencyAccount :accountList) {
            accountListDto.add(new CurrencyAccountDto(currencyAccount));
        }
        ResultListDto<CurrencyAccountDto> resultListDto = new ResultListDto<>(accountListDto);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
        } else {
            req.setAttribute("accountListDto", JSON.getMapper().writeValueAsString(resultListDto));
            context.getRequestDispatcher("/serverInfo/systemAccounts.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

}