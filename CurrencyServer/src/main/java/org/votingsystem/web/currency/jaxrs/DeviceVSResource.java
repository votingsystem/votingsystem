package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/deviceVS")
public class DeviceVSResource {

    private static final Logger log = Logger.getLogger(DeviceVSResource.class.getSimpleName());

    @Inject DAOBean dao;

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
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.userVS.nif =:nif").setParameter("nif", nif);
        List<DeviceVS> deviceVSList = query.getResultList();
        List<DeviceVSDto> resultList = new ArrayList<>();
        for(DeviceVS deviceVS : deviceVSList) {
            if(SessionVSManager.getInstance().get(deviceVS.getId()) != null) {
                resultList.add(new DeviceVSDto(deviceVS));
            }
        }
        ResultListDto<DeviceVSDto> resultListDto = new ResultListDto<DeviceVSDto>(resultList, 0, resultList.size(),
                Long.valueOf(resultList.size()));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }


}
