package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.currency.websocket.SessionVSManager;
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
@Path("/deviceVS")
public class DeviceVSResource {

    private static final Logger log = Logger.getLogger(DeviceVSResource.class.getName());

    @Inject DAOBean dao;

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long deviceId) throws Exception {
        DeviceVS deviceVS = dao.find(DeviceVS.class, deviceId);
        if(deviceVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - DeviceVS not found - id:" + deviceId).build();
        DeviceVSDto dto = new DeviceVSDto(deviceVS);
        dto.setSessionId(SessionVSManager.getInstance().getDeviceSessionId(deviceVS.getId()));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/nif/{nif}/list")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("nif") String nifStr) throws Exception {
        String nif = NifUtils.validate(nifStr);
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.userVS.nif =:nif").setParameter("nif", nif);
        List<DeviceVS> deviceVSList = query.getResultList();
        List<DeviceVSDto> resultList = new ArrayList<>();
        for(DeviceVS deviceVS : deviceVSList) {
            resultList.add(new DeviceVSDto(deviceVS));
        }
        ResultListDto<DeviceVSDto> resultListDto = new ResultListDto<DeviceVSDto>(resultList, 0, resultList.size(),
                Long.valueOf(resultList.size()));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/nif/{nif}/connected")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response connected(@PathParam("nif") String nifStr) throws Exception {
        String nif = NifUtils.validate(nifStr);
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", nif);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - UserVS not found - nif:" + nif).build();
        Set<DeviceVS> deviceVSSet = SessionVSManager.getInstance().getUserVSDeviceVSSet(userVS.getId());
        Set<DeviceVSDto> deviceSetDto = new HashSet<>();
        for(DeviceVS deviceVS : deviceVSSet) {
            deviceSetDto.add(new DeviceVSDto(deviceVS));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(UserVSDto.DEVICES(userVS, deviceSetDto, null))).build();
    }

    @Path("/id/{deviceId}/connected")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response connectedDevice(@PathParam("deviceId") Long deviceId,
            @DefaultValue("false") @QueryParam("getAllDevicesFromOwner") boolean getAllDevicesFromOwner) throws Exception {
        Set<DeviceVSDto> deviceSetDto = new HashSet<>();
        String sessionId = SessionVSManager.getInstance().getDeviceSessionId(deviceId);
        UserVS userVS = null;
        DeviceVS deviceVS = null;
        if(sessionId != null) {
            Session session = SessionVSManager.getInstance().getAuthenticatedSession(sessionId);
            userVS = (UserVS) session.getUserProperties().get("userVS");
            deviceVS = (DeviceVS) session.getUserProperties().get("deviceVS");
            if(deviceVS != null) deviceSetDto.add(new DeviceVSDto(deviceVS));
        }
        if(getAllDevicesFromOwner) {
            if(userVS == null) {
                Query query = dao.getEM().createQuery("select d.userVS from DeviceVS d where d.id =:id").setParameter("id", deviceId);
                userVS = dao.getSingleResult(UserVS.class, query);
            }
            if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                    "ERROR - User not found for device with id:" + deviceId).build();
            Set<DeviceVS> deviceVSSet = SessionVSManager.getInstance().getUserVSDeviceVSSet(userVS.getId());
            for(DeviceVS device : deviceVSSet) {
                if(deviceVS == null || !deviceVS.getId().equals(device.getId())) deviceSetDto.add(new DeviceVSDto(device));
            }
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(UserVSDto.DEVICES(userVS, deviceSetDto, null))).build();
    }

}