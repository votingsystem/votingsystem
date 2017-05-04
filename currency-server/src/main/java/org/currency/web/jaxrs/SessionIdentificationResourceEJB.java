package org.currency.web.jaxrs;

import org.currency.web.cdi.SocketPushEvent;
import org.currency.web.ejb.DeviceEJB;
import org.currency.web.http.CurrencyPrincipal;
import org.currency.web.util.AuthRole;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.JSON;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.TransactionAttribute;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

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
        deviceEJB.closeDeviceSession(signedDocument, req.getSession());
        return Response.ok().entity("session closed").build();
    }

    /**
     * Called from the mobile with the browser and mobile certificates in a PKCS7 document signed by the user and the
     * Id Provider. Once validated the request, the server sends a push message to the browser with the certificate and the private
     * key (encrypted with the public key previously provided by the browser)
     *
     * @param cmsMessage
     * @param browserUUID
     * @param socketMsg
     * @param req
     * @return
     * @throws Exception
     */
    @PermitAll
    @POST @Path("/validate-mobile-browser-session-certificates")
    @TransactionAttribute(REQUIRES_NEW)
    public Response sessionCertificationData(@FormParam("cmsMessage") String cmsMessage,
            @FormParam("browserUUID") String browserUUID,
            @FormParam("socketMsg") String socketMsg, @Context HttpServletRequest req) throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(cmsMessage.getBytes());
        deviceEJB.sessionCertification(cmsSignedMessage);

        MessageDto message = JSON.getMapper().readValue(socketMsg, MessageDto.class);
        SocketPushEvent pushEvent = new SocketPushEvent(socketMsg, message.getDeviceToUUID(),
                SocketPushEvent.Type.TO_USER);
        beanManager.fireEvent(pushEvent);
        return  Response.ok().entity("OK").build();
    }

    @PermitAll
    @POST @Path("/init-device-session")
    public Response initDeviceSession(CMSDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        deviceEJB.initDeviceSession(signedDocument, req.getSession());
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