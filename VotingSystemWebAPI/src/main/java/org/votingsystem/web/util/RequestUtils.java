package org.votingsystem.web.util;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.TypeVS;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class RequestUtils {

    public static Object processRequest(MessageSMIME messageSMIME, @Context ServletContext context,
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
        TypeVS operation = TypeVS.valueOf((String) signedContentMap.get("operation"));
        switch(operation) {
            case SEND_VOTE:
                viewer = "message-smime-votevs";
                break;
            case CANCEL_VOTE:
                viewer = "message-smime-votevs-canceler";
                break;
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                viewer = "message-smime-representative-anonymousdelegation-request";
                break;
            case ACCESS_REQUEST:
                viewer = "message-smime-access-request";
                break;
        }
        if(contentType.contains("json")) {
            Map resultMap = new HashMap<>();
            resultMap.put("operation", operation);
            resultMap.put("smimeMessage", smimeMessageStr);
            resultMap.put("signedContentMap", signedContentMap);
            resultMap.put("timeStampDate", timeStampDate.getTime());
            resultMap.put("viewer", viewer);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultMap)).type(MediaTypeVS.JSON).build();
        } else {
            req.setAttribute("operation", operation);
            req.setAttribute("smimeMessage", smimeMessageStr);
            req.setAttribute("signedContentMap", JSON.getMapper().writeValueAsString(signedContentMap));
            req.setAttribute("timeStampDate", timeStampDate.getTime());
            req.setAttribute("viewer",  viewer);
            context.getRequestDispatcher("/messageSMIME/contentViewer.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }


}
