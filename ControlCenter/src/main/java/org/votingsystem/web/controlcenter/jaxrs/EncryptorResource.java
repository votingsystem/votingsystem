package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.EncryptedMsgDto;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

@Path("/encryptor")
public class EncryptorResource {

    private static final Logger log = Logger.getLogger(EncryptorResource.class.getName());

    @Inject ConfigVS config;
    private MessagesVS messages = MessagesVS.getCurrentInstance();
    @Inject CMSBean cmsBean;
    @Inject DAOBean dao;

    @Path("/") @POST
    public Response request(EncryptedMsgDto request, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        if(request.getPublicKey() == null) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - missing publicKey").build();
        byte[] decodedPK = Base64.getDecoder().decode(request.getPublicKey());
        PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
        request.setMessage(format("Hello ''{0}'' from ''{1}''", request.getFrom(), config.getServerName()));
        byte[] responseBytes = JSON.getMapper().writeValueAsBytes(request);
        //if(requestMap.receiverCert) cmsBean.encryptToCMS(responseBytes, requestMap.receiverCert)
        return Response.ok().entity(cmsBean.encryptToCMS(responseBytes, receiverPublic)).type(
                MediaTypeVS.MULTIPART_ENCRYPTED).build();
    }

    @Path("/getMultiSignedMessage") @POST
    public Response getMultiSignedMessage(MessageCMS messageCMS, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        CMSSignedMessage cmsMessage = cmsBean.addSignature(messageCMS.getCMS());
        return  Response.ok().entity(cmsMessage.toPEM()).type(MediaTypeVS.JSON_SIGNED).build();
    }

    @Path("/validateTimeStamp") @POST
    public Response validateTimeStamp(MessageCMS messageCMS, @Context ServletContext context,
                                      @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        UserVS userVS = messageCMS.getUserVS();
        //Date dateFinish = DateUtils.getDateFromString("2014-01-01 00:00:00")
        Map requestMap = messageCMS.getSignedContent(Map.class);
        EventVS eventVS = dao.find(EventVS.class, ((Number)requestMap.get("eventId")).longValue());
        Date signatureTime = userVS.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!eventVS.isActive(signatureTime)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(messages.get("checkedDateRangeErrorMsg",
                    signatureTime, eventVS.getDateBegin(), eventVS.getDateFinish())).build();
        } else return Response.ok().build();
    }

}
