package org.votingsystem.web.currency.filter;

import org.votingsystem.web.ejb.CMSBean;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(AuthenticationFilter.class.getName());

    @Inject CMSBean cmsBean;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        PrincipalVS principal = (PrincipalVS) requestContext.getSecurityContext().getUserPrincipal();
        if(principal != null) {
            if(cmsBean.isAdmin(principal.getUser().getNif())) {
                requestContext.setSecurityContext(new SecurityContext() {

                    @Override public Principal getUserPrincipal() {
                        return principal;
                    }

                    @Override public boolean isUserInRole(String role) {
                        return PrincipalVS.ADMIN_ROLES.contains(role);
                    }

                    @Override public boolean isSecure() {
                        return requestContext.getSecurityContext().isSecure();
                    }

                    @Override public String getAuthenticationScheme() {
                        return null;
                    }
                });
            }
        }
    }
}
