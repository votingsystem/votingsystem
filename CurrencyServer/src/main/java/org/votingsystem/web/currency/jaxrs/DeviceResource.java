package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.currency.websocket.SessionManager;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
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
public class DeviceResource {

    private static final Logger log = Logger.getLogger(DeviceResource.class.getName());

    @Inject DAOBean dao;

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long deviceId) throws Exception {
        Device device = dao.find(Device.class, deviceId);
        if(device == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - Device not found - id:" + deviceId).build();
        DeviceDto dto = new DeviceDto(device);
        dto.setSessionId(SessionManager.getInstance().getDeviceSessionId(device.getId()));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/nif/{nif}/list")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("nif") String nifStr) throws Exception {
        String nif = NifUtils.validate(nifStr);
        Query query = dao.getEM().createQuery("select d from Device d where d.user.nif =:nif").setParameter("nif", nif);
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
        Query query = dao.getEM().createQuery("select u from User u where u.nif =:nif").setParameter("nif", nif);
        User user = dao.getSingleResult(User.class, query);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - nif:" + nif).build();
        Set<Device> deviceSet = SessionManager.getInstance().getUserDeviceSet(user.getId());
        Set<DeviceDto> deviceSetDto = new HashSet<>();
        for(Device device : deviceSet) {
            deviceSetDto.add(new DeviceDto(device));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(UserDto.DEVICES(user, deviceSetDto, null))).build();
    }

    @Path("/id/{deviceId}/connected")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response connectedDevice(@PathParam("deviceId") Long deviceId,
            @DefaultValue("false") @QueryParam("getAllDevicesFromOwner") boolean getAllDevicesFromOwner) throws Exception {
        Set<DeviceDto> deviceSetDto = new HashSet<>();
        String sessionId = SessionManager.getInstance().getDeviceSessionId(deviceId);
        User user = null;
        Device device = null;
        if(sessionId != null) {
            Session session = SessionManager.getInstance().getAuthenticatedSession(sessionId);
            user = (User) session.getUserProperties().get("user");
            device = (Device) session.getUserProperties().get("device");
            if(device != null) deviceSetDto.add(new DeviceDto(device));
        }
        if(getAllDevicesFromOwner) {
            if(user == null) {
                Query query = dao.getEM().createQuery("select d.user from Device d where d.id =:id").setParameter("id", deviceId);
                user = dao.getSingleResult(User.class, query);
            }
            if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                    "ERROR - User not found for device with id:" + deviceId).build();
            Set<Device> deviceSet = SessionManager.getInstance().getUserDeviceSet(user.getId());
            for(Device dev : deviceSet) {
                if(dev == null || !dev.getId().equals(dev.getId())) deviceSetDto.add(new DeviceDto(dev));
            }
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(UserDto.DEVICES(user, deviceSetDto, null))).build();
    }

}