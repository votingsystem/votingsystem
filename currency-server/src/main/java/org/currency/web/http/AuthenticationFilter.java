package org.currency.web.http;

import org.currency.web.ejb.ConfigCurrencyServer;
import org.currency.web.util.AuthRole;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(AuthenticationFilter.class.getName());

    @EJB
    ConfigCurrencyServer config;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        CurrencyPrincipal principal = (CurrencyPrincipal) requestContext.getSecurityContext().getUserPrincipal();
        if(principal != null) {
            try {
                if(config.isAdmin(principal.getUser())) {
                    requestContext.setSecurityContext(new SecurityContext() {

                        @Override public Principal getUserPrincipal() {
                            return principal;
                        }

                        @Override public boolean isUserInRole(String role) {
                            return AuthRole.ADMIN_ROLES.contains(role);
                        }

                        @Override public boolean isSecure() {
                            return requestContext.getSecurityContext().isSecure();
                        }

                        @Override public String getAuthenticationScheme() {
                            return null;
                        }
                    });
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
