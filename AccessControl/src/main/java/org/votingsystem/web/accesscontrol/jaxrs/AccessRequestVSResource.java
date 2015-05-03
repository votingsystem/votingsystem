package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.AccessRequestVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/accessRequestVS")
public class AccessRequestVSResource {

    private static Logger log = Logger.getLogger(AccessRequestVSResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    private MessagesVS messages = MessagesVS.getCurrentInstance();


    @Path("/id/{id}") @GET
    public Response getById(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        AccessRequestVS accessRequestVS = dao.find(AccessRequestVS.class, id);
        if(accessRequestVS == null) return Response.status(Response.Status.NOT_FOUND).entity(messages.get("ERROR - "
            + "not found - AccessRequestVS id: " + id)).build();
        else return Response.ok().entity(accessRequestVS.getMessageSMIME().getContent())
                .type(ContentTypeVS.TEXT_STREAM.getName()).build();

    }

    @Path("/hashHex/{hashHex}") @GET
    public Response getById(@PathParam("hashHex") String hashHex, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        String hashAccessRequestBase64 = new String(hexConverter.unmarshal(hashHex));
        Query query = dao.getEM().createQuery("select a from AccessRequestVS a where a.hashAccessRequestBase64 =:hashHex")
                .setParameter("hashHex", hashHex);
        AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
        if(accessRequestVS == null) return Response.status(Response.Status.NOT_FOUND).entity(messages.get("ERROR - "
                + "not found - AccessRequestVS hashHex: " + hashHex)).build();
        else return Response.ok().entity(accessRequestVS.getMessageSMIME().getContent())
                .type(ContentTypeVS.TEXT_STREAM.getName()).build();
    }

    @Path("/eventVS/id/{eventId}/userVS/nif/{nif}") @GET
    public Response findByEventAndNif(@PathParam("eventId") long eventId, @PathParam("nif") String nif,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws Exception {
        EventVSElection eventVS = dao.find(EventVSElection.class, eventId);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(messages.get("ERROR - "
                + "not found - EventVSElection id: " + eventId)).build();
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", nif);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity(messages.get("ERROR - "
                + "not found - UserVS nif: " + nif)).build();
        query = dao.getEM().createQuery("select a from AccessRequestVS a where a.eventVS =:eventVS " +
                "and a.userVS =:userVS and a.state =:state").setParameter("eventVS", eventVS)
                .setParameter("userVS", userVS).setParameter("state", AccessRequestVS.State.OK);
        AccessRequestVS accessRequestVS = dao.getSingleResult(AccessRequestVS.class, query);
        if(accessRequestVS == null) return Response.status(Response.Status.NOT_FOUND).entity(messages.get("ERROR - "
                + "not found - AccessRequestVS event id: " + eventId + " - userVS nif: " + nif)).build();
        else return Response.ok().entity(accessRequestVS.getMessageSMIME().getContent())
                .type(ContentTypeVS.TEXT_STREAM.getName()).build();
    }


}
