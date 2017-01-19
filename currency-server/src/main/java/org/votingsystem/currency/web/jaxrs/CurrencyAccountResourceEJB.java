package org.votingsystem.currency.web.jaxrs;

import com.google.common.collect.ImmutableMap;
import org.votingsystem.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.currency.web.ejb.UserEJB;
import org.votingsystem.currency.web.http.CurrencyPrincipal;
import org.votingsystem.currency.web.util.AuthRole;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyAccountDto;
import org.votingsystem.dto.currency.SystemAccountsDto;
import org.votingsystem.dto.currency.TagDto;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.JSON;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
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
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/currencyAccount")
public class CurrencyAccountResourceEJB {

    private static final Logger log = Logger.getLogger(CurrencyAccountResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private UserEJB userBean;


    @RolesAllowed(AuthRole.ADMIN)
    @Path("/user/id/{userId}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response userAccountsForAdmin(@PathParam("userId") long userId, @Context HttpServletRequest req)
            throws IOException, ServletException {
        User user = em.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("user id: " + userId).build();
        }
        Query query = em.createQuery("select a from CurrencyAccount a where a.user =:user " +
                "and a.state =:state").setParameter("user", user).setParameter("state", CurrencyAccount.State.ACTIVE);
        List<CurrencyAccount> userAccountsDB = query.getResultList();
        List<CurrencyAccountDto> userAccounts = new ArrayList<>();
        for(CurrencyAccount account : userAccountsDB) {
            userAccounts.add(new CurrencyAccountDto(account));
        }
        ResultListDto<CurrencyAccountDto> resultListDto = new ResultListDto<>(userAccounts);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @RolesAllowed(AuthRole.USER)
    @Path("/user")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response userAccounts(@Context HttpServletRequest req) throws IOException, ServletException {
        User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        List<CurrencyAccount> userAccountsDB = em.createQuery(
                "select account from CurrencyAccount account where account.user =:user and account.state =:state")
                .setParameter("user", user)
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        Map<CurrencyCode, List<CurrencyAccountDto>> resultMap = new HashMap<>();
        for(CurrencyAccount account : userAccountsDB) {
            if(resultMap.containsKey(account.getCurrencyCode()))
                resultMap.get(account.getCurrencyCode()).add(new CurrencyAccountDto(account));
            else resultMap.put(account.getCurrencyCode(), new ArrayList<>(Arrays.asList(new CurrencyAccountDto(account))));
        }
        //ResultListDto<CurrencyAccountDto> resultListDto = new ResultListDto<>(userAccounts);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultMap)).build();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("/system") @Transactional
    public Response system() throws IOException, ServletException {
        List<CurrencyAccount> systemAccounts = em.createQuery(
                "select c FROM CurrencyAccount c where c.user =:user and c.state =:state")
                .setParameter("user", config.getSystemUser()).setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        List<CurrencyAccountDto> systemAccountsDto = new ArrayList<>();
        for(CurrencyAccount currencyAccount:systemAccounts) {
            systemAccountsDto.add(new CurrencyAccountDto(currencyAccount));
        }
        List<User.Type> notList = Arrays.asList(User.Type.CURRENCY_SERVER);
        Query query = em.createQuery(
                "select SUM(c.balance), tag, c.currencyCode from CurrencyAccount c JOIN c.tag tag where c.state =:state " +
                "and c.user.type not in :notList group by tag, c.currencyCode")
                .setParameter("state", CurrencyAccount.State.ACTIVE)
                .setParameter("notList", notList);
        List<Object[]> resultList = query.getResultList();
        List<TagDto> userAccounts = new ArrayList<>();
        for(Object[] result : resultList) {
            userAccounts.add(new TagDto((BigDecimal) result[0], (CurrencyCode) result[2], (Tag) result[1]));
        }
        query = em.createQuery(
                "select SUM(t.amount), tag, t.currencyCode from Transaction t JOIN t.tag tag where t.state =:state " +
                "and t.type =:type group by tag, t.currencyCode").setParameter("state", Transaction.State.OK)
                .setParameter("type", Transaction.Type.FROM_BANK);
        resultList = query.getResultList();
        List<TagDto> bankInputs = new ArrayList<>();
        for(Object[] result : resultList) {
            bankInputs.add(new TagDto((BigDecimal) result[0], (CurrencyCode) result[2], (Tag) result[1]));
        }
        SystemAccountsDto resultDto = new SystemAccountsDto(systemAccountsDto, userAccounts, bankInputs);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultDto)).build();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("/bank/id/{bankId}") @Transactional
    public Response bank(@PathParam("bankId") long bankId) throws Exception {
        Bank bank = em.find(Bank.class, bankId);
        if(bank == null || bank.getType() != User.Type.BANK) {
            return Response.status(Response.Status.NOT_FOUND).entity("bank id: " + bankId).build();
        }
        Query query = em.createQuery("select SUM(t.amount), tag, t.currencyCode from Transaction t " +
                "JOIN t.tag tag where t.state =:state and t.fromUser=:bank group by tag, t.currencyCode")
                .setParameter("state", Transaction.State.OK)
                .setParameter("bank", bank);
        List<Object[]> resultList = query.getResultList();
        List<TagDto> tagBalanceList = new ArrayList<>();
        for(Object[] result : resultList) {
            tagBalanceList.add(new TagDto((BigDecimal) result[0], (CurrencyCode) result[2], (Tag) result[1]));
        }
        Map<CurrencyCode, List<TagDto>> tagBalanceMap = tagBalanceList.stream()
                .collect(Collectors.groupingBy(TagDto::getCurrencyCode));
        Map result = ImmutableMap.of("bank", userBean.getUserDto(bank, false), "tagBalanceList", tagBalanceMap);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(result)).build();
    }

}