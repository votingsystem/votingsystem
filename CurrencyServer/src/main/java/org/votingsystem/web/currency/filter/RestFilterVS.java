package org.votingsystem.web.currency.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class RestFilterVS implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(RestFilterVS.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        //requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build() );
        //requestContext.setProperty("contenttypevs", ContentTypeVS.JSON);
    }
}
