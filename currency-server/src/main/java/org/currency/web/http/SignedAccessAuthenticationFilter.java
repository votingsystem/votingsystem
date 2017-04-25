package org.currency.web.http;

import org.currency.web.ejb.ConfigEJB;
import org.currency.web.ejb.CurrencySignatureEJB;
import org.currency.web.util.AuthRole;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.FileUtils;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 *http://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
 */
@SignedAccessResource
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SignedAccessAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(SignedAccessAuthenticationFilter.class.getName());

    @EJB CurrencySignatureEJB signatureService;
    @EJB ConfigEJB config;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SignedDocument signedDocument;
        try {
            signedDocument = signatureService.validateXAdESAndSave(FileUtils.getBytesFromStream(requestContext.getEntityStream()));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NotAuthorizedException(ex.getMessage());
        }
        CurrencyPrincipal principal = new CurrencyPrincipal(signedDocument);
        try {
            Set<String> roles = config.isAdmin(signedDocument.getFirstSignature().getSigner()) ?
                    AuthRole.ADMIN_ROLES : AuthRole.USER_ROLES;
            requestContext.setSecurityContext(new SecurityContext() {
                @Override public Principal getUserPrincipal() {
                    return principal;
                }
                @Override public boolean isUserInRole(String role) {
                    return roles.contains(role);
                }
                @Override public boolean isSecure() {
                    return requestContext.getSecurityContext().isSecure();
                }
                @Override public String getAuthenticationScheme() {
                    return null;
                }
            });
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

}
