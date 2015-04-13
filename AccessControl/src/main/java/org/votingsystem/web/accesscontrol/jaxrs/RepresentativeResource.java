package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.RepresentativeAccreditationsDto;
import org.votingsystem.dto.RepresentativeDto;
import org.votingsystem.dto.RepresentativeVotingHistoryDto;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeBean;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeDelegationBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.EmailTemplateWrapper;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
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
    @Inject MessagesBean messages;


    @Path("/delegation") @POST
    public Response delegation(MessageSMIME messageSMIME) throws Exception {
        RepresentationDocument representationDocument = representativeDelegationBean.saveDelegation(messageSMIME);
        return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/history") @POST
    public Response history(MessageSMIME messageSMIME, @Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        EmailTemplateWrapper responseWrapper = new EmailTemplateWrapper(resp);
        context.getRequestDispatcher("/jsf/mail/RepresentativeVotingHistoryDownloadInstructions.jsp").forward(req, responseWrapper);
        String mailTemplate = responseWrapper.toString();
        representativeBean.processVotingHistoryRequest(messageSMIME, mailTemplate);
        RepresentativeVotingHistoryDto request = messageSMIME.getSignedContent(RepresentativeVotingHistoryDto.class);
        return Response.ok().entity(messages.get("backupRequestOKMsg", request.getEmail())).build();
    }

    @Path("/accreditations") @POST
    public Response accreditations(MessageSMIME messageSMIME,
                   @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EmailTemplateWrapper responseWrapper = new EmailTemplateWrapper(resp);
        req.setAttribute("pageTitle", messages.get("representativeAccreditationsLbl"));
        context.getRequestDispatcher("/jsf/mail/RepresentativeAccreditationRequestDownloadInstructions.jsp").forward(req, responseWrapper);
        String mailTemplate = responseWrapper.toString();
        RepresentativeAccreditationsDto request = messageSMIME.getSignedContent(RepresentativeAccreditationsDto.class);
        representativeBean.processAccreditationsRequest(messageSMIME, mailTemplate);
        return Response.ok().entity(messages.get("backupRequestOKMsg", request.getEmail())).build();
    }


    @Path("/revoke") @POST
    public Response revoke(MessageSMIME messageSMIME) throws Exception {
        MessageSMIME response = representativeDelegationBean.processRevoke(messageSMIME);
        return Response.ok().entity(response.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/") @GET
    public Response index( @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("50") @QueryParam("max") int max,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<RepresentativeDto> responseList = new ArrayList<>();
        Map representativeMap  = new HashMap();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.type =:type")
                .setParameter("type", UserVS.Type.REPRESENTATIVE);
        List<UserVS> representativeList = query.getResultList();
        for(UserVS representative : representativeList) {
            responseList.add(representativeBean.geRepresentativeDto(representative));
        }
        representativeMap.put("offset", offset);
        representativeMap.put("max", max);
        representativeMap.put("representatives", responseList);
        representativeMap.put("numRepresentatives", responseList.size());
        representativeMap.put("numTotalRepresentatives", responseList.size());//TODO totalCount
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(representativeMap))
                    .type(MediaTypeVS.JSON).build();
        } else {
            req.setAttribute("representativeData", JSON.getMapper().writeValueAsString(representativeMap));
            context.getRequestDispatcher("/representative/index.xhtml").forward(req, resp);
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
        RepresentativeDto representativeDto = representativeBean.geRepresentativeDto(representative);
        if(contentType.contains("json")) {
            return Response.ok().entity(new ObjectMapper().writeValueAsBytes(representativeDto))
                    .type(MediaTypeVS.JSON).build();
        } else {
            req.setAttribute("representativeMap", JSON.getMapper().writeValueAsString(representativeDto));
            context.getRequestDispatcher("/representative/representative.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Transactional
    @Path("/nif/{nif}") @GET
    public Response getByNif(@PathParam("nif") String nifReq) throws JsonProcessingException, ExceptionVS {
        String nif = NifUtils.validate(nifReq);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif and u.type =:type")
                .setParameter("nif", nif).setParameter("type", UserVS.Type.REPRESENTATIVE);
        UserVS representative = dao.getSingleResult(UserVS.class, query);
        if(representative == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - UserVS is not a representative - nif: " + nif).build();
        query = dao.getEM().createQuery("select r from RepresentativeDocument r where r.userVS =:userVS " +
                "and r.state =:state").setParameter("userVS", representative).setParameter("state", RepresentativeDocument.State.OK);
        RepresentativeDocument representativeDocument = dao.getSingleResult(RepresentativeDocument.class, query);
        if(representativeDocument == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - RepresentativeDocument not found - nif: " + nif).build();
        RepresentativeDto representativeDto = new RepresentativeDto(
                representativeDocument.getUserVS(), representativeDocument.getDescription());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(representativeDto)).type(MediaTypeVS.JSON).build();
    }

    @Path("/nif/{nif}/state") @GET
    public Response state(@PathParam("nif") String nifReq) throws JsonProcessingException, ExceptionVS {
        Map result = representativeBean.checkRepresentationState(nifReq);
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(result)).type(MediaTypeVS.JSON).build();
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
    public Response accreditationsBackupForEvent(@PathParam("id") Long id) throws IOException, ExceptionVS {
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - EventVSElection not found - eventId" + id).build();
        if(eventVS.isActive(new Date())) return Response.status(Response.Status.BAD_REQUEST).entity(
                messages.get("eventActiveErrorMsg")).build();
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
