package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.voting.RepresentativeAccreditationsDto;
import org.votingsystem.dto.voting.RepresentativeVotingHistoryDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Image;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.RepresentationDocument;
import org.votingsystem.model.voting.RepresentativeDocument;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeBean;
import org.votingsystem.web.accesscontrol.ejb.RepresentativeDelegationBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.EmailTemplateWrapper;
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
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

@Path("/representative")
public class RepresentativeResource {

    private static final Logger log = Logger.getLogger(RepresentativeResource.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject RepresentativeBean representativeBean;
    @Inject RepresentativeDelegationBean representativeDelegationBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Path("/save") @POST
    public Response save(CMSMessage cmsMessage) throws Exception {
        RepresentativeDocument representativeDocument = representativeBean.saveRepresentative(cmsMessage);
        UserDto representativeDto = representativeBean.getRepresentativeDto(representativeDocument.getUser());
        return Response.ok().entity(representativeDto).type(MediaType.JSON).build();
    }

    @Path("/history") @POST
    public Response history(CMSMessage cmsMessage, @Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws Exception {
        //To enable application filters change what follows to the standalone / domain server configuration
        //<servlet-container name="default" allow-non-standard-wrappers="true">
        EmailTemplateWrapper responseWrapper = new EmailTemplateWrapper(resp);
        context.getRequestDispatcher("/mail/RepresentativeVotingHistoryDownloadInstructions.vsp").forward(req, responseWrapper);
        String mailTemplate = responseWrapper.toString();
        representativeBean.processVotingHistoryRequest(cmsMessage, mailTemplate, req.getLocale());
        RepresentativeVotingHistoryDto request = cmsMessage.getSignedContent(RepresentativeVotingHistoryDto.class);
        return Response.ok().entity(messages.get("backupRequestOKMsg", request.getEmail())).build();
    }

    @Path("/accreditations") @POST
    public Response accreditations(CMSMessage cmsMessage,
                   @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        //To enable application filters change what follows to the standalone / domain server configuration
        //<servlet-container name="default" allow-non-standard-wrappers="true">
        EmailTemplateWrapper responseWrapper = new EmailTemplateWrapper(resp);
        req.setAttribute("pageTitle", messages.get("representativeAccreditationsLbl"));
        context.getRequestDispatcher("/mail/RepresentativeAccreditationRequestDownloadInstructions.vsp").forward(req, responseWrapper);
        String mailTemplate = responseWrapper.toString();
        RepresentativeAccreditationsDto request = cmsMessage.getSignedContent(RepresentativeAccreditationsDto.class);
        representativeBean.processAccreditationsRequest(cmsMessage, mailTemplate, req.getLocale());
        return Response.ok().entity(messages.get("backupRequestOKMsg", request.getEmail())).build();
    }

    @Path("/revoke") @POST
    public Response revoke(CMSMessage cmsMessage) throws Exception {
        CMSMessage response = representativeBean.processRevoke(cmsMessage);
        return Response.ok().entity(response.getContentPEM()).type(MediaType.JSON_SIGNED).build();
    }

    @Path("/") @GET
    public Response index( @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("50") @QueryParam("max") int max,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        List<UserDto> responseList = new ArrayList<>();
        Query query = dao.getEM().createQuery("select u from User u where u.type =:type")
                .setParameter("type", User.Type.REPRESENTATIVE);
        List<User> representativeList = query.getResultList();
        for(User representative : representativeList) {
            responseList.add(representativeBean.getRepresentativeDto(representative));
        }
        //TODO totalCount
        ResultListDto<UserDto> resultListDto = new ResultListDto<>(responseList, offset, max, responseList.size());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto))
                .type(MediaType.JSON).build();
    }

    @Path("/id/{id}") @GET
    public Response indexById(@PathParam("id") Long id, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException, URISyntaxException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        User representative = dao.find(User.class, id);
        if(representative == null || User.Type.REPRESENTATIVE != representative.getType()) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    "ERROR - User is not a representative - id: " + id).build();
        }
        if(contentType.contains("json")) {
            UserDto representativeDto = representativeBean.getRepresentativeDto(representative);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(representativeDto))
                    .type(MediaType.JSON).build();
        } else {
            return Response.temporaryRedirect(new URI("../spa.xhtml#!/rest/representative/id/" + representative.getId())).build();
        }
    }

    @Transactional
    @Path("/nif/{nif}") @GET
    public Response getByNif(@PathParam("nif") String nifReq, @Context HttpServletRequest req, @Context ServletContext context,
             @Context HttpServletResponse resp) throws IOException, ExceptionVS, ServletException, URISyntaxException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        String nif = NifUtils.validate(nifReq);
        Query query = dao.getEM().createQuery("select u from User u where u.nif =:nif and u.type =:type")
                .setParameter("nif", nif).setParameter("type", User.Type.REPRESENTATIVE);
        User representative = dao.getSingleResult(User.class, query);
        if(representative == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User is not a representative - nif: " + nif).build();
        if(contentType.contains("json")) {
            UserDto representativeDto = representativeBean.getRepresentativeDto(representative);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(representativeDto))
                    .type(MediaType.JSON).build();
        } else {
            return Response.temporaryRedirect(new URI("../spa.xhtml#!/rest/representative/id/" + representative.getId())).build();
        }
    }

    @Path("/image/id/{id}") @GET
    public Response imageById(@PathParam("id") Long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        Image image = dao.find(Image.class, id);
        if(image == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - Image not found - imageId: " + id).build();
        return Response.ok().entity(image.getFileBytes()).type(ContentType.IMAGE.getName()).build();
    }

    @Path("/id/{id}/image") @GET
    public Response image(@PathParam("id") Long id, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        User representative = dao.find(User.class, id);
        if (representative == null || User.Type.REPRESENTATIVE != representative.getType()) return Response.status(
                Response.Status.NOT_FOUND).entity("ERROR - representative not found - userId: " + id).build();
        Query query = dao.getEM().createQuery("select i from Image i where i.user =:representative and " +
                "i.type =:type").setParameter("representative", representative).setParameter("type",
                Image.Type.REPRESENTATIVE);
        Image image = dao.getSingleResult(Image.class, query);
        if(image == null)return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - Image not found - representativeId:" + id).build();
        return Response.ok().entity(image.getFileBytes()).type(ContentType.IMAGE.getName()).build();
    }

    @Path("eventVS/id/{id}/accreditationsBackup") @GET
    public Response accreditationsBackupForEvent(@PathParam("id") Long id) throws IOException, ExceptionVS {
        EventElection eventVS = dao.find(EventElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - EventElection not found - eventId" + id).build();
        if(eventVS.isActive(new Date())) return Response.status(Response.Status.BAD_REQUEST).entity(
                messages.get("eventActiveErrorMsg")).build();
        representativeBean.getAccreditationsBackupForEvent(eventVS);
        return Response.ok().entity("request procesed check your email").build();
    }

    @Path("/anonymousDelegation") @POST
    public Response anonymousDelegation(CMSMessage cmsMessage) throws Exception {
        RepresentationDocument response = representativeDelegationBean.saveAnonymousDelegation(cmsMessage);
        return Response.ok().entity(response.getActivationCMS().getContentPEM()).type(MediaType.JSON_SIGNED).build();
    }


}
