package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.MediaTypeVS;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/messageCMS")
public class MessageCMSResource {

    private static final Logger log = Logger.getLogger(MessageCMSResource.class.getName());

    @Inject DAOBean dao;

    @Path("/id/{id}") @GET
    public Object index(@PathParam("id") long id, @Context ServletContext context,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        MessageCMS messageCMS = dao.find(MessageCMS.class, id);
        if(messageCMS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "MessageCMS not found - id: " + id).build();
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(messageCMS.getContentPEM()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return RequestUtils.processRequest(messageCMS, context, req, resp);
    }

    @Transactional
    @Path("/vote/hash/{hashHex}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getVoteByHash(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select v from VoteVS v where v.certificateVS.hashCertVSBase64 =:hashCertVSBase64")
                .setParameter("hashCertVSBase64", hashCertVSBase64);
        VoteVS voteVS = dao.getSingleResult(VoteVS.class, query);
        if(voteVS == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    "ERROR - VoteVS not found - hashHex: " + hashHex).build();
        }
        return Response.ok().entity(voteVS.getCMSMessage().getCMS().toPEM()).type(MediaTypeVS.PEM).build();
    }

}
