package org.votingsystem.web.controlcenter.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.controlcenter.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

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

    private static final Logger log = Logger.getLogger(EncryptorResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject MessagesBean messages;
    @Inject SignatureBean signatureBean;
    @Inject DAOBean dao;

    @Path("/") @POST
    public Response request(Map requestMap, @Context ServletContext context,
                    @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        if(!requestMap.containsKey("publicKey")) return Response.status(Response.Status.BAD_REQUEST).entity(
                "ERROR - missing publicKey").build();
        byte[] decodedPK = Base64.getDecoder().decode((String) requestMap.get("publicKey"));
        PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
        requestMap.put("message", format("Hello '{0}' from '{1}'", requestMap.get("from"), config.getServerName()));
        byte[] responseBytes = new ObjectMapper().writeValueAsBytes(requestMap);
        //if(requestMap.receiverCert) signatureBean.encryptToCMS(responseBytes, requestMap.receiverCert)
        byte[] encryptedResponse = signatureBean.encryptMessage(responseBytes, receiverPublic);
        return Response.ok().entity(encryptedResponse).type(ContentTypeVS.MULTIPART_ENCRYPTED.getName()).build();
    }

    @Path("/getMultiSignedMessage") @POST
    public Response getMultiSignedMessage(MessageSMIME messageSMIME, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String fromUser = "EncryptorController";
        String toUser = "MultiSignatureTestClient";
        String subject = "Multisigned response";
        SMIMEMessage smimeMessage = signatureBean.getSMIMEMultiSigned(fromUser, toUser,
                messageSMIME.getSMIME(), subject);
        return  Response.ok().entity(smimeMessage.getBytes()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/validateTimeStamp") @POST
    public Response validateTimeStamp(MessageSMIME messageSMIME, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        UserVS userVS = messageSMIME.getUserVS();
        //Date dateFinish = DateUtils.getDateFromString("2014-01-01 00:00:00")
        Map requestMap = messageSMIME.getSignedContent(Map.class);
        EventVS eventVS = dao.find(EventVS.class, ((Number)requestMap.get("eventId")).longValue());
        Date signatureTime = userVS.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!eventVS.isActive(signatureTime)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(messages.get("checkedDateRangeErrorMsg",
                    signatureTime, eventVS.getDateBegin(), eventVS.getDateFinish())).build();
        } else return Response.ok().build();
    }

}
