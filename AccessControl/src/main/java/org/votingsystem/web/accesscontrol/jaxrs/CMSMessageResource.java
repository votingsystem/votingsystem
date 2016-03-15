package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.RequestUtils;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/cmsMessage")
public class CMSMessageResource {

    private static final Logger log = Logger.getLogger(CMSMessageResource.class.getName());

    @Inject DAOBean dao;

    @Path("/id/{id}") @GET
    public Object index(@PathParam("id") long id, @Context ServletContext context,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        CMSMessage cmsMessage = dao.find(CMSMessage.class, id);
        if(cmsMessage == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "CMSMessage not found - id: " + id).build();
        if(contentType.contains(ContentType.TEXT.getName())) {
            return Response.ok().entity(cmsMessage.getContentPEM()).type(ContentType.TEXT_STREAM.getName()).build();
        } else return RequestUtils.processRequest(cmsMessage, context, req, resp);
    }

    @Transactional
    @Path("/vote/hash/{hashHex}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response getVoteByHash(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select v from Vote v where v.certificate.hashCertVSBase64 =:hashCertVSBase64")
                .setParameter("hashCertVSBase64", hashCertVSBase64);
        Vote vote = dao.getSingleResult(Vote.class, query);
        if(vote == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    "ERROR - Vote not found - hashHex: " + hashHex).build();
        }
        return Response.ok().entity(vote.getCMSMessage().getCMS().toPEM()).type(MediaType.PEM).build();
    }

}