package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.util.MapUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/tagVS")
public class TagVSResource {

    private static final Logger log = Logger.getLogger(TagVSResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;

    @Path("/")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object index(@QueryParam("tag") String tag, @Context ServletContext context,
                  @Context HttpServletRequest req, @Context HttpServletResponse resp) {
        if(tag == null) return Response.status(Response.Status.BAD_REQUEST).entity("missing param - tag").build();
        Query query = dao.getEM().createQuery("select t from TagVS t where t.name like :tag").setParameter("tag",
                "%" + tag +  "%");
        List<TagVS> tagVSList = query.getResultList();
        List resultList = new ArrayList<>();
        for(TagVS tagVS : tagVSList) {
            resultList.add(MapUtils.getTagMap(tagVS));
        }
        Map result = new HashMap<>();
        result.put("tagRecords", resultList);
        result.put("numTotalTags", tagVSList.size());
        return result;
    }

    @POST @Path("/")
    @Consumes({"application/json"})
    public Object indexPost(Map requestMap, @Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        if(!requestMap.containsKey("tag"))  return Response.status(Response.Status.BAD_REQUEST).entity(
                "missing json node - tag").build();
        String tagName = (String) requestMap.get("tag");
        TagVS tagVS = config.getTag(tagName);
        if(tagVS == null) {
            tagVS = dao.persist(new TagVS(StringUtils.normalize(tagName)));
            dao.persist(new CurrencyAccount(signatureBean.getSystemUser(), BigDecimal.ZERO,
                    Currency.getInstance("EUR").getCurrencyCode(), tagVS));
        }
        Map result = MapUtils.getTagMap(tagVS);
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${result as JSON})"
        return result;
    }

    @Path("/list")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object list() {
        List<TagVS> tagVSList = dao.findAll(TagVS.class);
        List resultList = new ArrayList<>();
        for(TagVS tagVS : tagVSList) {
            resultList.add(MapUtils.getTagMap(tagVS));
        }
        Map result = new HashMap<>();
        result.put("tagRecords", resultList);
        result.put("numTotalTags", tagVSList.size());
        return result;
    }

    @Path("/currencyAccountByBalanceTagVS")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object currencyAccountByBalanceTagVS(@QueryParam("tag") String tag) {
        if(tag == null) return Response.status(Response.Status.BAD_REQUEST).entity("missing param - tag").build();
        Query query = dao.getEM().createQuery("select c from CurrencyAccount c where c.tag.name =:tag")
                .setParameter("tag", tag);
        List<CurrencyAccount> accountList = query.getResultList();
        List resultList = new ArrayList<>();
        for(CurrencyAccount account : accountList) {
            Map accountMap = new HashMap<>();
            accountMap.put("id", account.getId());
            accountMap.put("userVS", account.getUserVS().getNif());
            accountMap.put("amount", account.getBalance().toString());
            resultList.add(accountMap);
        }
        return resultList;
    }


}
