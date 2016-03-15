package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.model.voting.VoteCanceler;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.accesscontrol.ejb.VoteBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.logging.Logger;

@Path("/vote")
public class VoteResource {

    private static final Logger log = Logger.getLogger(VoteResource.class.getName());


    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject
    VoteBean voteBean;

    /**
     * Service that receives and checks the votes signed by the Control Center
     */
    @Transactional
    @Path("/")
    @POST
    public Response save(CMSDto CMSDto, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Vote vote = voteBean.validateVote(CMSDto);
        return Response.ok().entity(vote.getCMSMessage().getContentPEM()).type(ContentType.VOTE.getName()).build();
    }

    @Transactional
    @Path("/id/{id}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Vote vote = dao.find(Vote.class, id);
        if(vote == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - Vote not found - voteId: " + id).build();
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                new VoteDto(vote, config.getContextURL()))).type(MediaType.JSON).build();
    }

    @Transactional
    @Path("/hash/{hashHex}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response getByHash(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                        @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select v from Vote v where v.certificate.hashCertVSBase64 =:hashCertVSBase64")
                .setParameter("hashCertVSBase64", hashCertVSBase64);
        Vote vote = dao.getSingleResult(Vote.class, query);
        if(vote == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    "ERROR - Vote not found - hashHex: " + hashHex).build();
        }
        VoteDto dto = new VoteDto(vote, config.getContextURL());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).type(MediaType.JSON).build();
    }

    @Transactional
    @Path("/id/{id}/cancelation")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response cancelation(@PathParam("id") Long id, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Vote vote = dao.find(Vote.class, id);
        if(vote == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - Vote not found - voteId: " + id).build();
        Query query = dao.getEM().createQuery("select v from VoteCanceler v where v.vote =:vote")
                .setParameter("vote", vote);
        VoteCanceler voteCanceler = dao.getSingleResult(VoteCanceler.class, query);
        if(voteCanceler == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteCanceler not found - voteId: " + id).build();
        return Response.ok().entity(voteCanceler.getCmsMessage().getContentPEM())
                .type(MediaType.JSON_SIGNED).build();
    }


    @Transactional
    @Path("/canceler/hash/{hashHex}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response cancelerByHash(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select v from VoteCanceler v where v.hashCertVSBase64 =:hashCertVSBase64")
                .setParameter("hashCertVSBase64", hashCertVSBase64);
        VoteCanceler voteCanceler = dao.getSingleResult(VoteCanceler.class, query);
        if(voteCanceler == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - VoteCanceler not found - hashHex: " + hashHex + " - hashBase64: " + hashCertVSBase64).build();
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(new VoteDto(voteCanceler, config.getContextURL())))
                .entity(MediaType.JSON).build();
    }

    @Path("/cancel") @POST
    public Response post (CMSMessage cmsMessage, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        VoteCanceler canceler = voteBean.processCancel(cmsMessage);
        return Response.ok().entity(canceler.getCmsMessage().getContentPEM()).type(MediaType.JSON_SIGNED).build();
    }

}