package org.votingsystem.web.currency.filter;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class RestFilterVS implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(RestFilterVS.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        //requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build() );
        //requestContext.setProperty("contenttypevs", ContentType.JSON);
    }
}
