package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.BackupRequestVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.accesscontrol.ejb.EventVSElectionBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;
import org.votingsystem.web.util.RequestUtils;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/backupVS")
public class BackupVSResource {

    private static Logger log = Logger.getLogger(BackupVSResource.class.getName());

    @Inject DAOBean dao;
    @Inject EventVSElectionBean eventVSElectionBean;
    @Inject ConfigVS config;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Path("/request/id/{requestId}/download") @GET
    public Object download(@PathParam("requestId") long requestId, @Context ServletContext context,
               @Context HttpServletRequest req, @Context HttpServletResponse resp) throws ServletException, IOException {
        BackupRequestVS backupRequest = dao.find(BackupRequestVS.class, requestId);
        if(backupRequest == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - BackupRequestVS not found - id: " + requestId).build();
        log.info("backupRequest: " + backupRequest.getId() + " - " + backupRequest.getFilePath());
        String downloadURL = config.getStaticResURL() + backupRequest.getFilePath();
        resp.sendRedirect(downloadURL);
        return Response.ok().build();
    }

    @Path("/request/id/{requestId}") @GET
    public Object getRequest(@PathParam("requestId") long requestId, @Context ServletContext context,
                   @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        BackupRequestVS backupRequest = dao.find(BackupRequestVS.class, requestId);
        if(backupRequest == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - BackupRequestVS not found - id: " + requestId).build();
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(backupRequest.getMessageCMS().getContent()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return RequestUtils.processRequest(backupRequest.getMessageCMS(), context, req, resp);
    }

    @Path("/") @GET
    public Object genBackup(@QueryParam("eventId") Long eventId,
                        @QueryParam("email") String email,
                        @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        if(eventId == null) return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - missing param 'eventId'").build();
        if(email == null) return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - missing param 'email'").build();
        EventVS eventVS = dao.find(EventVS.class, eventId);
        if(eventVS == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - EventVS not found - eventId: " + eventId).build();
        if(!eventVS.getBackupAvailable()) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - EventVS without backup - eventId: " + eventId).build();
        eventVSElectionBean.generateBackup((EventVSElection) eventVS);
        dao.persist(new BackupRequestVS(null, TypeVS.VOTING_EVENT, email));
        return Response.ok().entity(messages.get("backupRequestOKMsg", email)).build();
    }

}