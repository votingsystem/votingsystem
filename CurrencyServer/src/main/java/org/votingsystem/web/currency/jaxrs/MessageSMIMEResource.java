package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.*;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/messageSMIME")
public class MessageSMIMEResource {

    private static final Logger log = Logger.getLogger(MessageSMIMEResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject UserVSBean userVSBean;

    @Path("/id/{id}") @GET @Transactional
    public Response index(@PathParam("id") long id, @Context ServletContext context,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        MessageSMIME messageSMIME = dao.find(MessageSMIME.class, id);
        if(messageSMIME == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "MessageSMIME not found - id: " + id).build();
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return processRequest(messageSMIME, context, req, resp);
    }

    @Path("/transactionVS/id/{id}") @GET @Transactional
    public Response transactionVS(@PathParam("id") long id, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        TransactionVS transactionVS = dao.find(TransactionVS.class, id);
        if(transactionVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "TransactionVS not found - transactionVSId: " + id).build();
        return processRequest(transactionVS.getMessageSMIME(), context, req, resp);
    }

    private Response processRequest(MessageSMIME messageSMIME, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        String smimeMessageStr = Base64.getEncoder().encodeToString(messageSMIME.getContent());
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        Date timeStampDate = null;
        Map signedContentMap;
        String viewer = "message-smime";
        if(smimeMessage.getTimeStampToken() != null) {
            timeStampDate = smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        }
        signedContentMap = messageSMIME.getSignedContentMap();
        if(signedContentMap.containsKey("timeLimited")) {
            signedContentMap.put("validTo", DateUtils.getDayWeekDateStr(
                    DateUtils.getNexMonday(DateUtils.getCalendar(timeStampDate)).getTime()));
        }
        TypeVS operation = TypeVS.valueOf((String) signedContentMap.get("operation"));
        if(TypeVS.CURRENCY_SEND != operation) {
            if(signedContentMap.containsKey("fromUserVS")) {
                signedContentMap.put("fromUserVS", UserVSDto.BASIC(messageSMIME.getUserVS()));
            }
        }
        switch(operation) {
            case CURRENCY_GROUP_NEW:
                viewer = "message-smime-groupvs-new";
                break;
            case FROM_BANKVS:
                viewer = "message-smime-transactionvs-from-bankvs";
                break;
            case CURRENCY_REQUEST:
                viewer = "message-smime-transactionvs-currency-request";
                break;
            case FROM_GROUP_TO_ALL_MEMBERS:
                viewer = "message-smime-transactionvs";
                break;
        }
        if(contentType.contains("json")) {
            Map resultMap = new HashMap<>();
            resultMap.put("operation", signedContentMap.get("operation"));
            resultMap.put("smimeMessage", smimeMessageStr);
            resultMap.put("signedContentMap", JSON.getMapper().writeValueAsString(signedContentMap));
            resultMap.put("timeStampDate", timeStampDate.getTime());
            resultMap.put("viewer", viewer);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultMap)).type(MediaTypeVS.JSON).build();
        } else {
            req.setAttribute("operation", signedContentMap.get("operation"));
            req.setAttribute("smimeMessage", smimeMessageStr);
            req.setAttribute("signedContentMap", JSON.getMapper().writeValueAsString(signedContentMap));
            req.setAttribute("timeStampDate", timeStampDate.getTime());
            req.setAttribute("viewer",  viewer);
            context.getRequestDispatcher("/messageSMIME/contentViewer.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

}
