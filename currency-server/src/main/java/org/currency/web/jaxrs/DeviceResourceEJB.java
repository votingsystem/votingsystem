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
@Path("/device")
public class DeviceResourceEJB {

    private static final Logger log = Logger.getLogger(DeviceResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private SignerInfoService signerInfoService;
    @Inject private BeanManager beanManager;
    @Inject private DeviceEJB deviceEJB;

    @Transactional
    @POST @Path("/close-session")
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

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long deviceId) throws Exception {
        Device device = em.find(Device.class, deviceId);
        if(device == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - Device not found - id:" + deviceId).build();
        DeviceDto dto = new DeviceDto(device);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/nif/{nif}/list")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("nif") String nifStr) throws Exception {
        String nif = NifUtils.validate(nifStr);
        Query query = em.createQuery("select d from Device d where d.user.numId =:numId").setParameter("numId", nif);
        List<Device> deviceList = query.getResultList();
        List<DeviceDto> resultList = new ArrayList<>();
        for(Device device : deviceList) {
            resultList.add(new DeviceDto(device));
        }
        ResultListDto<DeviceDto> resultListDto = new ResultListDto<DeviceDto>(resultList, 0, resultList.size(),
                Long.valueOf(resultList.size()));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/nif/{nif}/connected")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response connected(@PathParam("nif") String nifStr) throws Exception {
        String nif = NifUtils.validate(nifStr);
        List<User> userList = em.createQuery("select u from User u where u.numId=:numId")
                .setParameter("numId", nif).getResultList();
        if(userList.isEmpty()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - nif:" + nif).build();
        User user = userList.iterator().next();
        Set<Device> deviceSet = SessionManager.getInstance().getUserDeviceSet(user.getId());
        Set<DeviceDto> deviceSetDto = new HashSet<>();
        for(Device device : deviceSet) {
            deviceSetDto.add(new DeviceDto(device));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(UserDto.DEVICES(user, deviceSetDto, null))).build();
    }

    @Path("/id/{deviceUUID}/connected")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response connectedDevice(@PathParam("deviceUUID") String deviceUUID,
            @DefaultValue("false") @QueryParam("getAllDevicesFromOwner") boolean getAllDevicesFromOwner) throws Exception {
        Set<DeviceDto> deviceSetDto = new HashSet<>();
        Session session = SessionManager.getInstance().getDeviceSession(deviceUUID);
        User user = null;
        Device device = null;
        if(session != null) {
            user = (User) session.getUserProperties().get("user");
            device = (Device) session.getUserProperties().get("device");
            if(device != null) deviceSetDto.add(new DeviceDto(device));
        }
        if(getAllDevicesFromOwner) {
            if(user == null) {
                List<User> userList = em.createQuery("select d.user from Device d where d.UUID =:UUID")
                        .setParameter("UUID", deviceUUID).getResultList();
                user = userList.iterator().next();
            }
            if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                    "ERROR - User not found for device with UUID:" + deviceUUID).build();
            Set<Device> deviceSet = SessionManager.getInstance().getUserDeviceSet(user.getId());
            for(Device dev : deviceSet) {
                if(dev == null || !dev.getId().equals(dev.getId())) deviceSetDto.add(new DeviceDto(dev));
            }
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(UserDto.DEVICES(user, deviceSetDto, null))).build();
    }

    /**
     * Called from the browser to provide the public key that encrypts the message the mobile sends to the browser with the
     * 'browser session certificate'
     * @param req
     * @param csrRequestBytes
     * @return
     * @throws Exception
     */
    @POST @Path("/browser-publickey")
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
    @POST @Path("/session-certification-data")
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

    @POST @Path("/init-browser-session")
    @TransactionAttribute(REQUIRES_NEW)
    public Response initBrowserSession(CMSDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        deviceEJB.initBrowserSession(signedDocument, req.getSession());
        return  Response.ok().entity("OK - init-browser-session").build();
    }

    @POST @Path("/init-mobile-session")
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