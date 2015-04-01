package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.CurrencyAccountBean;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/currencyAccount")
public class CurrencyAccountResource {

    private static final Logger log = Logger.getLogger(CurrencyAccountResource.class.getSimpleName());

    @Inject ConfigVS app;
    @Inject DAOBean dao;
    @Inject CurrencyAccountBean currencyAccountBean;

    @Path("/userVS/id/{userId}/balance") // old_url -> /userVS/$id/balance
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object balance(@PathParam("userId") long userId, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        UserVS userVS = dao.find(UserVS.class, userId);
        Map resultMap = new HashMap<>();
        if(userVS == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("userVS id: " + userId);
        }
        Query query = dao.getEM().createQuery("select a from CurrencyAccount a where a.userVS =:userVS " +
                "and a.state =:state").setParameter("userVS", userVS).setParameter("state", CurrencyAccount.State.ACTIVE);
        List<CurrencyAccount> userAccountsDB = query.getResultList();
        List userAccounts = new ArrayList<>();
        for(CurrencyAccount account : userAccountsDB) {
            userAccounts.add(currencyAccountBean.getUserVSAccountMap(account));
        }
        resultMap.put("id", userVS.getId());
        resultMap.put("name", userVS.getName());
        resultMap.put("accounts", userAccounts);
        return resultMap;
    }

}
