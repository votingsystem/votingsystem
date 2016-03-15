package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.TagVSDto;
import org.votingsystem.dto.currency.CurrencyAccountDto;
import org.votingsystem.dto.currency.SystemAccountsDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.ejb.DAOBean;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/currencyAccount")
public class CurrencyAccountResource {

    private static final Logger log = Logger.getLogger(CurrencyAccountResource.class.getName());

    @Inject ConfigVS app;
    @Inject DAOBean dao;
    @Inject ConfigVS config;


    @Path("/user/id/{userId}/balance")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response balance(@PathParam("userId") long userId, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        User user = dao.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("user id: " + userId).build();
        }
        Query query = dao.getEM().createQuery("select a from CurrencyAccount a where a.user =:user " +
                "and a.state =:state").setParameter("user", user).setParameter("state", CurrencyAccount.State.ACTIVE);
        List<CurrencyAccount> userAccountsDB = query.getResultList();
        List<CurrencyAccountDto> userAccounts = new ArrayList<>();
        for(CurrencyAccount account : userAccountsDB) {
            userAccounts.add(new CurrencyAccountDto(account));
        }
        ResultListDto<CurrencyAccountDto> resultListDto = new ResultListDto<>(userAccounts);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }


    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("/system") @Transactional
    public Response system(@Context HttpServletRequest req, @Context HttpServletResponse resp,
                             @Context ServletContext context) throws IOException, ServletException {
        Query query = dao.getEM().createQuery("select c FROM CurrencyAccount c where c.user =:user and c.state =:state")
                .setParameter("user", config.getSystemUser()).setParameter("state", CurrencyAccount.State.ACTIVE);
        List<CurrencyAccount> accountList = query.getResultList();
        List<CurrencyAccountDto> accountListDto = new ArrayList<>();
        for(CurrencyAccount currencyAccount :accountList) {
            accountListDto.add(new CurrencyAccountDto(currencyAccount));
        }
        List<User.Type> notList = Arrays.asList(User.Type.SYSTEM);
        query = dao.getEM().createQuery("select SUM(c.balance), tag, c.currencyCode from CurrencyAccount c JOIN c.tag tag where c.state =:state " +
                "and c.user.type not in :notList group by tag, c.currencyCode").setParameter("state", CurrencyAccount.State.ACTIVE)
                .setParameter("notList", notList);
        List<Object[]> resultList = query.getResultList();
        List<TagVSDto> tagVSBalanceList = new ArrayList<>();
        for(Object[] result : resultList) {
            tagVSBalanceList.add(new TagVSDto((BigDecimal) result[0], (String) result[2], (TagVS) result[1]));
        }


        query = dao.getEM().createQuery("select SUM(t.amount), tag, t.currencyCode from TransactionVS t JOIN t.tag tag where t.state =:state " +
                "and t.type =:type group by tag, t.currencyCode").setParameter("state", TransactionVS.State.OK)
                .setParameter("type", TransactionVS.Type.FROM_BANK);
        resultList = query.getResultList();
        List<TagVSDto> bankBalanceList = new ArrayList<>();
        for(Object[] result : resultList) {
            bankBalanceList.add(new TagVSDto((BigDecimal) result[0], (String) result[2], (TagVS) result[1]));
        }
        SystemAccountsDto systemAccountsDto = new SystemAccountsDto(accountListDto, tagVSBalanceList, bankBalanceList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(systemAccountsDto)).build();
    }

}
