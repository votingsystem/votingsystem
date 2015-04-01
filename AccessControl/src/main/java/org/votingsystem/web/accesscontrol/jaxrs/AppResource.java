package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/app")
public class AppResource {

    private static final Logger log = Logger.getLogger(AppResource.class.getSimpleName());

    @Inject ConfigVS config;


    @GET @Path("/androidClient")
    public Response androidClient(@QueryParam("browserToken") String browserToken, @QueryParam("eventId") String eventId,
            @QueryParam("serverURL") String serverURL, @QueryParam("msg") String msg,
            @Context ServletContext context, @Context HttpServletRequest req,
                                  @Context HttpServletResponse resp) throws ServletException, IOException {
        if(req.getParameter("androidClientLoaded") != null && Boolean.getBoolean(req.getParameter("androidClientLoaded"))) {
            context.getRequestDispatcher("/jsf/app/index.jsp").forward(req, resp);
            return Response.ok().build();
        } else {
            String uri = config.getWebURL() + "/eventVSElection/main?androidClientLoaded=false";
            if(browserToken != null) uri = uri + "#" + browserToken;
            if(eventId != null) uri =  uri + "&eventId=" + eventId;
            if(serverURL != null) uri = uri + "&serverURL=" + serverURL;
            if(msg != null) {
                String encodedMsg = URLEncoder.encode(msg, "UTF-8");
                uri = uri + "&msg=" + encodedMsg;
            }
            log.log(Level.FINE, "uri: " + uri);
            context.getRequestDispatcher(uri).forward(req, resp);
            return Response.ok().build();
        }
    }

    @GET @Path("/tools")
    public Response tools(@Context ServletContext context, @Context HttpServletRequest req,
                      @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/jsf/app/tools.jsp").forward(req, resp);
        return Response.ok().build();
    }


    @GET @Path("/contact")
    public Response contact(@Context ServletContext context, @Context HttpServletRequest req,
                              @Context HttpServletResponse resp) throws ServletException, IOException, ExceptionVS {
        context.getRequestDispatcher("/jsf/app/contact.jsp").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/jsonDocs")
    public Response jsonDocs(@Context ServletContext context, @Context HttpServletRequest req,
                            @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/jsf/app/jsonDocs.jsp").forward(req, resp);
        return Response.ok().build();
    }

    @GET @Path("/backups")
    public Response backups(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/jsf/app/backups.jsp").forward(req, resp);
        return Response.ok().build();
    }

}