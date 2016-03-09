package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyAccountDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/tagVS")
public class TagVSResource {

    private static final Logger log = Logger.getLogger(TagVSResource.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;

    @Path("/")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response index(@DefaultValue("") @QueryParam("tag") String tag, @Context ServletContext context,
                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws JsonProcessingException {
        Query query = dao.getEM().createQuery("select t from TagVS t where upper(t.name) like :tag").setParameter("tag",
                "%" + StringUtils.removeAccents(tag).toUpperCase() + "%");
        List<TagVS> tagVSList = query.getResultList();
        ResultListDto<TagVS> resultListDto = new ResultListDto<>(tagVSList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }


    @Path("/list")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws JsonProcessingException {
        List<TagVS> tagVSList = dao.findAll(TagVS.class);
        ResultListDto resultList = new ResultListDto(tagVSList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultList)).type(MediaTypeVS.JSON).build();
    }

    @POST @Path("/")
    @Consumes({"application/json"})
    public Response indexPost(Map requestMap, @Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) throws JsonProcessingException, ValidationExceptionVS {
        if(!requestMap.containsKey("tag"))  return Response.status(Response.Status.BAD_REQUEST).entity(
                "missing json node - tag").build();
        String tagName = (String) requestMap.get("tag");
        TagVS tagVS = config.getTag(tagName);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(tagVS)).type(MediaTypeVS.JSON).build();
    }

    @Path("/currencyAccountByBalanceTagVS")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response currencyAccountByBalanceTagVS(@QueryParam("tag") String tag) throws JsonProcessingException {
        if(tag == null) return Response.status(Response.Status.BAD_REQUEST).entity("missing param - tag").build();
        Query query = dao.getEM().createQuery("select c from CurrencyAccount c where c.tag.name =:tag")
                .setParameter("tag", tag);
        List<CurrencyAccount> accountList = query.getResultList();
        List<CurrencyAccountDto> resultList = new ArrayList<>();
        for(CurrencyAccount account : accountList) {
            resultList.add(new CurrencyAccountDto(account));
        }
        ResultListDto resultListDto = new ResultListDto(resultList);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaTypeVS.JSON).build();
    }

}
