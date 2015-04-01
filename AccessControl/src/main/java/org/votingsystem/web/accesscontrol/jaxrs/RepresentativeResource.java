package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.json.RepresentativeJSON;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeBean;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeDelegationBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.EmailTemplateWrapper;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Path("/representative")
public class RepresentativeResource {

    private static final Logger log = Logger.getLogger(RepresentativeResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject RepresentativeBean representativeBean;
    @Inject RepresentativeDelegationBean representativeDelegationBean;



    @Path("/delegation") @POST
    public Response delegation(MessageSMIME messageSMIME) throws Exception {
        RepresentationDocument representationDocument = representativeDelegationBean.saveDelegation(messageSMIME);
        return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/history") @POST
    public Response history(MessageSMIME messageSMIME, @Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        EmailTemplateWrapper responseWrapper = new EmailTemplateWrapper(resp);
        context.getRequestDispatcher("/jsf/mail/RepresentativeVotingHistoryDownloadInstructions.vsp").forward(req, responseWrapper);
        String mailTemplate = responseWrapper.toString();
        //log.info("Output : " + content);
        representativeBean.processVotingHistoryRequest(messageSMIME, mailTemplate);
        return Response.ok().build();
    }

    @Path("/accreditations") @POST
    public Response accreditations(MessageSMIME messageSMIME,
                   @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EmailTemplateWrapper responseWrapper = new EmailTemplateWrapper(resp);
        context.getRequestDispatcher("/jsf/mail/RepresentativeAccreditationRequestDownloadInstructions.vsp").forward(req, responseWrapper);
        String mailTemplate = responseWrapper.toString();
        //log.info("Output : " + content);
        representativeBean.processAccreditationsRequest(messageSMIME, mailTemplate);
        return Response.ok().build();
    }


    @Path("/revoke")
    public Response revoke(MessageSMIME messageSMIME) throws Exception {
        MessageSMIME response = representativeDelegationBean.processRevoke(messageSMIME);
        return Response.ok().entity(response.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/") @GET
    public Response index( @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("100") @QueryParam("max") int max,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<RepresentativeJSON> responseList = new ArrayList<>();
        Map representativeMap  = new HashMap();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.type =:type")
                .setParameter("type", UserVS.Type.REPRESENTATIVE);
        List<UserVS> representativeList = query.getResultList();
        for(UserVS representative : representativeList) {
            responseList.add(representativeBean.geRepresentativeJSON(representative));
        }
        representativeMap.put("offset", offset);
        representativeMap.put("max", max);
        representativeMap.put("representatives", responseList);
        representativeMap.put("numRepresentatives", responseList.size());
        representativeMap.put("numTotalRepresentatives", responseList.size());//TODO totalCount
        if(contentType.contains("json")) {
            return Response.ok().entity(new ObjectMapper().writeValueAsBytes(representativeMap))
                    .type(ContentTypeVS.JSON.getName()).build();
        } else {
            req.setAttribute("representativeData", JSON.getInstance().writeValueAsString(representativeMap));
            context.getRequestDispatcher("/jsf/representative/index.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/id/{id}") @GET
    public Response indexById(@PathParam("id") Long id, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        UserVS representative = dao.find(UserVS.class, id);
        if(representative == null || UserVS.Type.REPRESENTATIVE != representative.getType()) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    "ERROR - UserVS is not a representative - id: " + id).build();
        }
        RepresentativeJSON representativeJSON = representativeBean.geRepresentativeJSON(representative);
        if(contentType.contains("json")) {
            return Response.ok().entity(new ObjectMapper().writeValueAsBytes(representativeJSON))
                    .type(ContentTypeVS.JSON.getName()).build();
        } else {
            req.setAttribute("representativeMap", JSON.getInstance().writeValueAsString(representativeJSON));
            context.getRequestDispatcher("/jsf/representative/representative.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/nif/{nif}") @GET
    public Response getByNif(@PathParam("nif") String nifReq) throws JsonProcessingException, ExceptionVS {
        String nif = NifUtils.validate(nifReq);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", nif).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - UserVS is not a representative - nif: " + nif).build();
        Map result = new HashMap<>();
        result.put("representativeId", representative.getId());
        result.put("representativeName", representative.getName());
        result.put("representativeNIF", representative.getNif());
        result.put("firstName", representative.getFirstName());
        result.put("info", representative.getDescription());
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(result)).type(ContentTypeVS.JSON.getName()).build();
    }

    @Path("/nif/{nif}/state") @GET
    public Response state(@PathParam("nif") String nifReq) throws JsonProcessingException, ExceptionVS {
        Map result = representativeBean.checkRepresentationState(nifReq);
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(result)).type(ContentTypeVS.JSON.getName()).build();
    }

    @Path("/nif/{nif}/edit") @GET
    public Response edit(@PathParam("nif") String nifReq) throws JsonProcessingException, ExceptionVS {
        String nif = NifUtils.validate(nifReq);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", nif).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - UserVS is not a representative - nif: " + nif).build();
        Map result = new HashMap<>();
        result.put("id", representative.getId());
        result.put("name", representative.getName());
        result.put("nif", representative.getNif());
        result.put("firstName", representative.getFirstName());
        result.put("info", representative.getDescription());
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(result)).type(ContentTypeVS.JSON.getName()).build();
    }

    @Path("/image/id/{id}") @GET
    public Response imageById(@PathParam("id") Long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        ImageVS image = dao.find(ImageVS.class, id);
        if(image == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - ImageVS not found - imageId: " + id).build();
        return Response.ok().entity(image.getFileBytes()).type(ContentTypeVS.IMAGE.getName()).build();
    }

    @Path("/id/{id}/image") @GET
    public Response image(@PathParam("id") Long id, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        UserVS representative = dao.find(UserVS.class, id);
        if (representative == null || UserVS.Type.REPRESENTATIVE != representative.getType()) return Response.status(
                Response.Status.NOT_FOUND).entity("ERROR - representative not found - userId: " + id).build();
        Query query = dao.getEM().createQuery("select i from ImageVS i where i.userVS =:representative and " +
                "i.type =:type").setParameter("representative", representative).setParameter("type",
                ImageVS.Type.REPRESENTATIVE);
        ImageVS image = dao.getSingleResult(ImageVS.class, query);
        if(image == null)return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - ImageVS not found - representativeId:" + id).build();
        return Response.ok().entity(image.getFileBytes()).type(ContentTypeVS.IMAGE.getName()).build();
    }


    @Path("eventVS/id/{id}/accreditationsBackup") @GET
    public Response iaccreditationsBackupForEvent(@PathParam("id") Long id) throws IOException, ExceptionVS {
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - EventVSElection not found - eventId" + id).build();
        if(eventVS.isActive(new Date())) return Response.status(Response.Status.BAD_REQUEST).entity(
                config.get("eventActiveErrorMsg")).build();
        representativeBean.getAccreditationsBackupForEvent(eventVS);
        return Response.ok().entity("request procesed check your email").build();
    }

    @Path("/anonymousDelegation")
    public Response anonymousDelegation(MessageSMIME messageSMIME) throws Exception {
        RepresentationDocument response = representativeDelegationBean.saveAnonymousDelegation(messageSMIME);
        return Response.ok().entity(response.getActivationSMIME().getContent()).type(
                ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/cancelAnonymousDelegation")
    public Response cancelAnonymousDelegation(MessageSMIME messageSMIME) throws Exception {
        MessageSMIME response = representativeDelegationBean.cancelAnonymousDelegation(messageSMIME);
        return Response.ok().entity(response.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }


}
