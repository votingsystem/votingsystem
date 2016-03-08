package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.currency.TransactionVS;
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
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/messageCMS")
public class MessageCMSResource {

    private static final Logger log = Logger.getLogger(MessageCMSResource.class.getName());

    private static final List<TypeVS> anonymousTransaction = Arrays.asList(TypeVS.CURRENCY_SEND, TypeVS.CURRENCY_CHANGE);

    @Inject DAOBean dao;
    @Inject UserVSBean userVSBean;

    @Path("/id/{id}") @GET @Transactional
    public Response index(@PathParam("id") long id, @Context ServletContext context,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        MessageCMS messageCMS = dao.find(MessageCMS.class, id);
        if(messageCMS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "MessageCMS not found - id: " + id).build();
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(messageCMS.getContent()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return processRequest(messageCMS, context, req, resp);
    }

    @Path("/transactionVS/id/{id}") @GET @Transactional
    public Response transactionVS(@PathParam("id") long id, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        TransactionVS transactionVS = dao.find(TransactionVS.class, id);
        if(transactionVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "TransactionVS not found - transactionVSId: " + id).build();
        return processRequest(transactionVS.getMessageCMS(), context, req, resp);
    }

    private Response processRequest(MessageCMS messageCMS, @Context ServletContext context,
                                    @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        String cmsMessageStr = Base64.getEncoder().encodeToString(messageCMS.getContent());
        CMSSignedMessage cmsMessage = messageCMS.getCMS();
        Date timeStampDate = null;
        Map signedContentMap;
        String viewer = "message-cms";
        if(cmsMessage.getTimeStampToken() != null) {
            timeStampDate = cmsMessage.getTimeStampToken().getTimeStampInfo().getGenTime();
        }
        signedContentMap = messageCMS.getSignedContentMap();
        if(signedContentMap.containsKey("timeLimited")) {
            signedContentMap.put("validTo", DateUtils.getDayWeekDateStr(
                    DateUtils.getNexMonday(DateUtils.getCalendar(timeStampDate)).getTime(), "HH:mm"));
        }
        TypeVS operation = TypeVS.valueOf((String) signedContentMap.get("operation"));
        if(!anonymousTransaction.contains(operation)) {
            signedContentMap.put("fromUserVS", UserVSDto.BASIC(messageCMS.getUserVS()));
        }
        if(messageCMS.getUserVS() != null)
            signedContentMap.put("fromUserIBAN", messageCMS.getUserVS().getIBAN());
        switch(operation) {
            case FROM_BANKVS:
                viewer = "message-cms-transactionvs-from-bankvs";
                break;
            case CURRENCY_REQUEST:
                viewer = "message-cms-transactionvs-currency-request";
                break;
            case FROM_GROUP_TO_ALL_MEMBERS:
                viewer = "message-cms-transactionvs";
                break;
            case CURRENCY_CHANGE:
                signedContentMap.remove("currencySet");
                signedContentMap.remove("currencyChangeCSR");
                signedContentMap.remove("leftOverCSR");
                viewer = "message-cms-transactionvs-currency-change";
                break;
        }
        if(contentType.contains("json")) {
            Map resultMap = new HashMap<>();
            resultMap.put("operation", signedContentMap.get("operation"));
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
