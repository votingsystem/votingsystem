package org.votingsystem.web.controlcenter.jaxrs;

import org.votingsystem.web.util.ConfigVS;

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
            context.getRequestDispatcher("/app/index.xhtml").forward(req, resp);
            return Response.ok().build();
        } else {
            String uri = config.getContextURL() + "/eventVSElection/main?androidClientLoaded=false";
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

}