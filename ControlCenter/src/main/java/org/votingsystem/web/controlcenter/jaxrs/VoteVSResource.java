package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.dto.SMIMEDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.model.voting.VoteVSCanceler;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.controlcenter.ejb.VoteVSBean;
import org.votingsystem.web.ejb.DAOBean;
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
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.logging.Logger;

@Path("/voteVS")
public class VoteVSResource {

    private static final Logger log = Logger.getLogger(VoteVSResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject VoteVSBean voteVSBean;

    /**
     * Service that validates and sends votes to the 'access control'
     */
    @Path("/")
    @POST
    public Response save(SMIMEDto smimeDto,  @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        VoteVS voteVS = voteVSBean.validateVote(smimeDto);
        MessageSMIME messageSMIME = smimeDto.getMessageSMIME();
        if(messageSMIME.getUserVS() != null) resp.setHeader("representativeNIF", messageSMIME.getUserVS().getNif());
        return Response.ok().entity(voteVS.getMessageSMIME().getContent()).type(ContentTypeVS.VOTE.getName()).build();
    }

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        VoteVS voteVS = dao.find(VoteVS.class, id);
        if(voteVS == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteVS not found - voteId: " + id).build();
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                new VoteVSDto(voteVS, config.getContextURL()))).type(MediaTypeVS.JSON).build();
    }


    @Path("/hash/{hashHex}") @GET
    public Response getByHash(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS.hashCertVSBase64 =:hashCertVSBase64")
                .setParameter("hashCertVSBase64", hashCertVSBase64);
        VoteVS voteVS = dao.getSingleResult(VoteVS.class, query);
        if(voteVS == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteVS not found - hashHex: " + hashHex).build();
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                new VoteVSDto(voteVS, config.getContextURL()))).type(MediaTypeVS.JSON).build();
    }

    @Path("/id/{id}/cancelation")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response cancelation(@PathParam("id") Long id, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        VoteVS voteVS = dao.find(VoteVS.class, id);
        if(voteVS == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteVS not found - voteId: " + id).build();
        Query query = dao.getEM().createQuery("select v from VoteVSCanceler v where v.voteVS =:voteVS")
                .setParameter("voteVS", voteVS);
        VoteVSCanceler voteVSCanceler = dao.getSingleResult(VoteVSCanceler.class, query);
        if(voteVSCanceler == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteVSCanceler not found - voteId: " + id).build();
        return Response.ok().entity(voteVSCanceler.getMessageSMIME().getContent())
                .type(MediaTypeVS.JSON_SIGNED).build();
    }


    @Path("/canceler/hash/{hashHex}") @GET
    public Response cancelerByHash(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select v from VoteVSCanceler v where v.hashCertVSBase64 =:hashCertVSBase64")
                .setParameter("hashCertVSBase64", hashCertVSBase64);
        VoteVSCanceler voteCanceler = dao.getSingleResult(VoteVSCanceler.class, query);
        if(voteCanceler == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteVSCanceler not found - hashHex: " + hashHex + " - hashBase64: " + hashCertVSBase64).build();
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(new VoteVSDto(voteCanceler, config.getContextURL())))
                .entity(MediaTypeVS.JSON).build();
    }

    @Path("/cancel") @POST
    public Response post (MessageSMIME messageSMIME,  @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        VoteVSCanceler canceler = voteVSBean.processCancel(messageSMIME);
        return Response.ok().entity(canceler.getMessageSMIME().getContent()).type(MediaTypeVS.JSON_SIGNED).build();
    }

}
