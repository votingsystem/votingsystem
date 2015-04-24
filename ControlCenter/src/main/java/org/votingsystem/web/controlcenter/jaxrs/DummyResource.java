package org.votingsystem.web.controlcenter.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Path("/dummy")
public class DummyResource {

    private static final Logger log = Logger.getLogger(DummyResource.class.getSimpleName());


    @Inject ConfigVS config;
    @Inject DAOBean dao;

    @Path("/test") @GET
    public Response doGet(@Context Request req, @Context HttpServletResponse resp) throws JsonProcessingException {
        Map result = new HashMap<>();
        result.put("statusCode", 200);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(result)).type(MediaTypeVS.JSON).build();
    }

}
