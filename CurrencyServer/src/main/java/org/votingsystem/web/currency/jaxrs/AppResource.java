package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.currency.ejb.DashBoardBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/app")
public class AppResource {

    private static final Logger log = Logger.getLogger(AppResource.class.getSimpleName());

    @Inject
    DashBoardBean dashBoardBean;

    @GET @Path("/androidClient")
    public Response androidClient(@Context ServletContext context, @Context HttpServletRequest req,
                                  @Context HttpServletResponse resp) throws ServletException, IOException {
        if(req.getParameter("androidClientLoaded") != null && Boolean.getBoolean(req.getParameter("androidClientLoaded"))) {
            context.getRequestDispatcher("/jsf/app/index.jsp").forward(req, resp);
            return Response.ok().build();
        } else {
            String uri = req.getParameter("refererURL");
            if(uri != null && uri.contains("androidClientLoaded=false")) {
                uri = uri + "?androidClientLoaded=false";
                context.getRequestDispatcher(uri).forward(req, resp);
                return Response.ok().build();
            } else return Response.status(Response.Status.BAD_REQUEST).entity("missing param 'refererURL'").build();
        }
    }

    @GET @Path("/tools")
    public Response tools(@Context ServletContext context, @Context HttpServletRequest req,
                      @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/jsf/app/tools.jsp").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/accounts")
    public Response accounts(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/app/accounts.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/admin")
    public Response admin(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/app/admin.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/transactions")
    public Response transactions(@Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/app/transactions.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/messagevs")
    public Response messagevs(@Context ServletContext context, @Context HttpServletRequest req,
                                 @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/app/messagevs.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/contact")
    public Response contact(@Context ServletContext context, @Context HttpServletRequest req,
                              @Context HttpServletResponse resp) throws ServletException, IOException, ExceptionVS {
        context.getRequestDispatcher("/app/contact.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/jsonDocs")
    public Response jsonDocs(@Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/app/jsonDocs.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/userVS")
    @Produces(MediaType.APPLICATION_JSON)
    public Object userVS(@Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws ServletException, IOException {
        TimePeriod timePeriod = DateUtils.addHours(Calendar.getInstance(), -1);//default to 1 hour
        Map dataMap = new HashMap<>();
        dataMap.put("transactionVSData", dashBoardBean.getUserVSInfo(timePeriod));
        if(req.getContentType() != null && req.getContentType().contains("json")) {
            return dataMap;
        } else {
            req.setAttribute("dataMap", JSON.getMapper().writeValueAsString(dataMap));
            context.getRequestDispatcher("/app/user.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @GET @Path("/userVS/{numHours}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object userVS(@PathParam("numHours") Integer numHours,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        TimePeriod timePeriod = DateUtils.addHours(Calendar.getInstance(), numHours);
        Map dataMap = new HashMap<>();
        dataMap.put("transactionVSData", dashBoardBean.getUserVSInfo(timePeriod));
        if(req.getContentType() != null && req.getContentType().contains("json")) {
            return dataMap;
        } else {
            req.setAttribute("dataMap", JSON.getMapper().writeValueAsString(dataMap));
            context.getRequestDispatcher("/jsf/app/user.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

}