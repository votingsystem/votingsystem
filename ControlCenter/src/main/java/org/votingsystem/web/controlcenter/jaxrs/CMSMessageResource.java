package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.CMSMessage;
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
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(cmsMessage.getContentPEM()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return processRequest(cmsMessage, context, req, resp);
    }


    private Object processRequest(CMSMessage cmsMessage, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        CMSSignedMessage cmsSignedMessage = cmsMessage.getCMS();
        String cmsMessageStr = cmsSignedMessage.toPEMStr();
        Date timeStampDate = null;
        Map signedContentMap;
        String viewer = "message-cms";
        if(cmsSignedMessage.getTimeStampToken() != null) {
            timeStampDate = cmsSignedMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        }
        signedContentMap = cmsMessage.getSignedContentMap();
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
            return Response.temporaryRedirect(new URI("../cmsMessage/contentViewer.xhtml")).build();

        }
    }



}
