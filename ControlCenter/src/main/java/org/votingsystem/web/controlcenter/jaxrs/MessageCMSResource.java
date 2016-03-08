package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
            return Response.ok().entity(messageCMS.getContent()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return processRequest(messageCMS, context, req, resp);
    }


    private Object processRequest(MessageCMS messageCMS, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        CMSSignedMessage cmsMessage = messageCMS.getCMS();
        String cmsMessageStr = cmsMessage.toPEMStr();
        Date timeStampDate = null;
        Map signedContentMap;
        String viewer = "message-cms";
        if(cmsMessage.getTimeStampToken() != null) {
            timeStampDate = cmsMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        }
        signedContentMap = messageCMS.getSignedContentMap();
        TypeVS operation = TypeVS.valueOf((String) signedContentMap.get("operation"));
        switch(operation) {
            case SEND_VOTE:
                viewer = "message-cms-votevs";
                break;
            case CANCEL_VOTE:
                viewer = "message-cms-votevs-canceler";
                break;
        }
        if(contentType.contains("json")) {
            Map resultMap = new HashMap<>();
            resultMap.put("operation", operation);
            resultMap.put("cmsMessage", cmsMessageStr);
            resultMap.put("signedContentMap", signedContentMap);
            resultMap.put("timeStampDate", timeStampDate.getTime());
            resultMap.put("viewer", viewer);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultMap)).type(MediaTypeVS.JSON).build();
        } else {
            req.getSession().setAttribute("operation", operation);
            req.getSession().setAttribute("cmsMessage", cmsMessageStr);
            req.getSession().setAttribute("signedContentMap", JSON.getMapper().writeValueAsString(signedContentMap));
            req.getSession().setAttribute("timeStampDate", timeStampDate.getTime());
            req.getSession().setAttribute("viewer", viewer);
            return Response.temporaryRedirect(new URI("../messageCMS/contentViewer.xhtml")).build();

        }
    }



}
