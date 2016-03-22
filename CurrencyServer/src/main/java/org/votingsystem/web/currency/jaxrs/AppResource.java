package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.DashBoardDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;
import org.votingsystem.web.currency.ejb.DashBoardBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/app")
public class AppResource {

    private static final Logger log = Logger.getLogger(AppResource.class.getName());

    @Inject DashBoardBean dashBoardBean;

    @GET @Path("/androidClient")
    public Response androidClient(@QueryParam("androidClientLoaded") @DefaultValue("false") Boolean androidClientLoaded,
                      @Context ServletContext context, @Context HttpServletRequest req,
                      @Context HttpServletResponse resp) throws ServletException, IOException, URISyntaxException {
        if(androidClientLoaded) {
            return Response.temporaryRedirect(new URI("../spa.xhtml")).build();
        } else {
            String uri = req.getParameter("refererURL");
            if(uri != null && uri.contains("androidClientLoaded=false")) {
                uri = uri + "?androidClientLoaded=false";
                context.getRequestDispatcher(uri).forward(req, resp);
                return Response.ok().build();
            } else return Response.status(Response.Status.BAD_REQUEST).entity("missing param 'refererURL'").build();
        }
    }

    @GET @Path("/userDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response user(@Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws ServletException, IOException {
        Interval timePeriod = DateUtils.addHours(Calendar.getInstance(), -1);//default to 1 hour
        DashBoardDto dto = dashBoardBean.getUserInfo(timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @GET @Path("/userDashboard/hoursAgo/{numHours}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response user(@PathParam("numHours") Integer numHours,
                         @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        Interval timePeriod = DateUtils.addHours(Calendar.getInstance(), - numHours);
        DashBoardDto dto = dashBoardBean.getUserInfo(timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

}