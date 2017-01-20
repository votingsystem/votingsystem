package org.votingsystem.currency.web.jaxrs;

import org.votingsystem.currency.web.ejb.DeviceEJB;
import org.votingsystem.currency.web.http.CurrencyPrincipal;
import org.votingsystem.currency.web.util.AuthRole;
import org.votingsystem.currency.web.websocket.SessionManager;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.indentity.BrowserCertificationDto;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.*;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;

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

    @RolesAllowed(AuthRole.USER)
    @POST @Path("/close-session")
    @Transactional
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

    @POST @Path("/browser-csr")
    @Produces(MediaType.TEXT_PLAIN)
    public Response browserCsr(@Context HttpServletRequest req) throws Exception {
        BrowserCertificationDto csrRequest = JSON.getMapper().readValue(
                FileUtils.getBytesFromStream(req.getInputStream()), BrowserCertificationDto.class);
        req.getSession().setAttribute(Constants.CSR, csrRequest);
        return Response.ok().entity("OK").build();
    }

    @POST @Path("/init-browser-session")
    @Produces(MediaType.TEXT_PLAIN)
    @TransactionAttribute(REQUIRES_NEW)
    public Response initBrowserSession(SignedDocument signedDocument, @Context HttpServletRequest req) throws Exception{
        deviceEJB.initBrowserSession(signedDocument);
        return Response.ok().entity("OK").build();
    }

}