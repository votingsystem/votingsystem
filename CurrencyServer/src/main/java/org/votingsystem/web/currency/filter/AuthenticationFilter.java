package org.votingsystem.web.currency.filter;

import org.apache.commons.io.IOUtils;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.currency.util.PrincipalVS;
import org.votingsystem.web.ejb.CMSBean;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 *http://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(AuthenticationFilter.class.getName());

    @Inject CMSBean cmsBean;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        CMSMessage cmsMessage;
        try {
            String contentType = requestContext.getHeaders().getFirst("Content-Type");
            if(MediaType.JSON_SIGNED.equals(contentType)) {
                cmsMessage = cmsBean.validateCMS(CMSSignedMessage.FROM_PEM(requestContext.getEntityStream()),
                        ContentType.JSON_SIGNED).getCmsMessage();
            } else if (MediaType.JSON_SIGNED_ENCRYPTED.equals(contentType)) {
                byte[] decryptedBytes = cmsBean.decryptCMS(IOUtils.toByteArray(requestContext.getEntityStream()));
                CMSSignedMessage signedData = new CMSSignedMessage(decryptedBytes);
                cmsMessage = cmsBean.validateCMS(signedData, ContentType.JSON_SIGNED).getCmsMessage();
            } else throw new ExceptionVS("Invalid content type: " + contentType);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NotAuthorizedException(ex.getMessage());
        }
        //to avoid process again the CMSMessage with org.votingsystem.web.jaxrs.provider.CMSMessageReader
        requestContext.getHeaders().remove("Content-Type");
        PrincipalVS principal = new PrincipalVS(cmsMessage);
        try {
            requestContext.setSecurityContext(new SecurityContext() {

                @Override public Principal getUserPrincipal() {
                    return principal;
                }
                @Override public boolean isUserInRole(String role) {
                    return true;
                }
                @Override public boolean isSecure() {
                    return requestContext.getSecurityContext().isSecure();
                }
                @Override public String getAuthenticationScheme() {
                    return null;
                }
            });
        } catch (Exception e) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

}
