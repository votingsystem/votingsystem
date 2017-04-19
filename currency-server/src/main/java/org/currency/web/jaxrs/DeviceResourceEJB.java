package org.currency.web.jaxrs;

import org.currency.web.ejb.DeviceEJB;
import org.currency.web.websocket.SessionManager;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.websocket.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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

    @Path("/uuid/{uuid}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("uuid") Long UUID) throws Exception {
        List<Device> devicelist = em.createQuery("select d from Device d where d.UUID =:UUID")
                .setParameter("UUID", UUID).getResultList();
        if(devicelist.isEmpty()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - Device not found - id:" + UUID).build();
        DeviceDto dto = new DeviceDto(devicelist.iterator().next());
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

}