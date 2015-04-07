package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/editor")
public class EditorResource {
    
    private static final Logger log = Logger.getLogger(EditorResource.class.getSimpleName());

    @Inject DAOBean dao;

    @Path("/vote") @GET
    public Object vote(@Context ServletContext context, @Context HttpServletRequest req,
                                @Context HttpServletResponse resp) throws Exception {
        Query query = dao.getEM().createQuery("select c from ControlCenterVS c where c.state =:state")
                .setParameter("state", ActorVS.State.OK);
        ControlCenterVS controlCenterVS = dao.getSingleResult(ControlCenterVS.class, query);
        Map controlCenterMap = new HashMap<>();
        controlCenterMap.put("id", controlCenterVS.getId());
        controlCenterMap.put("name", controlCenterVS.getName());
        controlCenterMap.put("state", controlCenterVS.getState());
        controlCenterMap.put("serverURL", controlCenterVS.getServerURL());
        controlCenterMap.put("dateCreated", controlCenterVS.getDateCreated());
        List<Map> controlCenterList = Arrays.asList(controlCenterMap);
        req.setAttribute("controlCenters", JSON.getEscapingMapper().writeValueAsString(controlCenterList));
        context.getRequestDispatcher("/jsf/eventVSElection/editor.jsp").forward(req, resp);
        return Response.ok().build();
    }


    @Path("/claim") @GET
    public Object getIssuedCert(@Context ServletContext context, @Context HttpServletRequest req,
                                @Context HttpServletResponse resp) throws Exception {
        context.getRequestDispatcher("/jsf/eventVSClaim/editor.jsp").forward(req, resp);
        return Response.ok().build();
    }

}
