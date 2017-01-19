package org.votingsystem.currency.web.jaxrs;

import org.votingsystem.currency.web.ejb.DashBoardEJB;
import org.votingsystem.dto.currency.DashBoardDto;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/app")
public class AppResourceEJB {

    private static final Logger log = Logger.getLogger(AppResourceEJB.class.getName());

    @Inject private DashBoardEJB dashBoardBean;

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
        Interval timePeriod = new Interval(ZonedDateTime.now(), ZonedDateTime.now().minus(1, ChronoUnit.HOURS));
        DashBoardDto dto = dashBoardBean.getUserInfo(timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @GET @Path("/userDashboard/hoursAgo/{numHours}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response user(@PathParam("numHours") Integer numHours, @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws ServletException, IOException {
        Interval timePeriod = new Interval(ZonedDateTime.now(), ZonedDateTime.now().minus(numHours, ChronoUnit.HOURS));
        DashBoardDto dto = dashBoardBean.getUserInfo(timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }


}