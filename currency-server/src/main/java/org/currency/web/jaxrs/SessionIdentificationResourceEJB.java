package org.currency.web.jaxrs;

import org.currency.web.ejb.DeviceEJB;
import org.currency.web.http.CurrencyPrincipal;
import org.currency.web.managed.SocketPushEvent;
import org.currency.web.util.AuthRole;
import org.currency.web.websocket.SessionManager;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.*;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;

import javax.annotation.security.RolesAllowed;
import javax.ejb.TransactionAttribute;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.websocket.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/session")
public class SessionIdentificationResourceEJB {

    private static final Logger log = Logger.getLogger(SessionIdentificationResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private SignerInfoService signerInfoService;
    @Inject private BeanManager beanManager;
    @Inject private DeviceEJB deviceEJB;

    @Transactional
    @POST @Path("/close")
    public Response closeSession(CMSDocument signedDocument, @Context HttpServletRequest req,
                                 @Context HttpServletResponse resp) throws Exception {
        User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        User signer = signedDocument.getFirstSignature().getSigner();
        List<Certificate> certList = em.createQuery("select c from Certificate c where c.UUID=:UUID")
                .setParameter("UUID", signer.getNumId()).getResultList();
        if(certList.isEmpty())
            return Response.status(Response.Status.NOT_FOUND).entity("Certificate not found - UUID: " + signer.getNumId()).build();
        signer = certList.iterator().next().getSigner();
        if(!user.getNumId().equals(signer.getNumId()))
            throw new ValidationException("signer it's not session owner");
        OperationDto requestDto = JSON.getMapper().readValue(signedDocument.getCmsMessage().getSignedContentStr(), OperationDto.class);
        if(requestDto.getOperation().getType() != CurrencyOperation.CLOSE_SESSION) throw new ValidationException(format(
                "bad message type, expected ''{0}'' found ''{1}''", CurrencyOperation.CLOSE_SESSION, requestDto.getOperation()));
        req.getSession().removeAttribute(Constants.USER_KEY);
        return Response.ok().entity("session closed").build();
    }
    /**
     * Called from the browser to provide the public key that encrypts the message the mobile sends to the browser with the
     * 'browser session certificate'
     * @param req
     * @param csrRequestBytes
     * @return
     * @throws Exception
     */
    @POST @Path("/browser-init-publickey")
    @Produces(MediaType.TEXT_PLAIN)
    public Response browserPublickey(@Context HttpServletRequest req, byte[] csrRequestBytes) throws Exception {
        SessionCertificationDto certDto = JSON.getMapper().readValue(csrRequestBytes, SessionCertificationDto.class);
        req.getSession().setAttribute(Constants.BROWSER_PLUBLIC_KEY, certDto);
        return Response.ok().entity("OK").build();
    }

    /**
     * Called from the mobile with the browser and mobile certificates in a PKCS7 document signed by the user and the
     * Id provider. Once validated the request, the server sends a push message to the browser with the certificate and the private
     * key (encrypted with the public key previously provided by the browser)
     *
     * @param cmsMessage
     * @param browserUUID
     * @param socketMsg
     * @param req
     * @return
     * @throws Exception
     */
    @POST @Path("/verify-device-certification")
    @TransactionAttribute(REQUIRES_NEW)
    public Response sessionCertificationData(@FormParam("cmsMessage") String cmsMessage,
            @FormParam("browserUUID") String browserUUID,
            @FormParam("socketMsg") String socketMsg, @Context HttpServletRequest req) throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(cmsMessage.getBytes());
        deviceEJB.sessionCertification(cmsSignedMessage);

        MessageDto message = JSON.getMapper().readValue(socketMsg, MessageDto.class);
        switch (message.getSocketOperation()) {
            case MSG_TO_DEVICE:
                if(SessionManager.getInstance().hasSession(message.getDeviceToUUID())) {
                    SessionManager.getInstance().sendMessage(socketMsg, message.getDeviceToUUID());
                } else {
                    SocketPushEvent pushEvent = new SocketPushEvent(socketMsg,
                            SocketPushEvent.Type.TO_USER).setUserUUID(message.getDeviceToUUID());
                    beanManager.fireEvent(pushEvent);
                }
                break;
            default:
                log.info("unprocessed socket operation: " + message.getSocketOperation());
        }
        return  Response.ok().entity("OK").build();
    }

    @POST @Path("/init-browser")
    @TransactionAttribute(REQUIRES_NEW)
    public Response initBrowserSession(CMSDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        deviceEJB.initBrowserSession(signedDocument, req.getSession());
        return  Response.ok().entity("OK - init-browser-session").build();
    }

    @POST @Path("/init-mobile")
    @TransactionAttribute(REQUIRES_NEW)
    public Response initMobileSession(CMSDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        deviceEJB.initMobileSession(signedDocument, req.getSession());
        return  Response.ok().entity("OK - init-mobile-session").build();
    }

    @RolesAllowed(AuthRole.USER)
    @Path("/authenticated-device")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response authenticatedDevice(SignedDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        User sessionUser = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        User signer = signedDocument.getFirstSignature().getSigner();
        if(!signer.getNumId().equals(sessionUser.getNumId()))
            throw new ValidationException("signer NIF doesn't match session NIF");
        DeviceDto dto = new DeviceDto(sessionUser.getDevice());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

}