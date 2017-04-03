package org.votingsystem.idprovider.jaxrs;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/auth")
@Stateless
public class AuthenticationResource {

    private static final Logger log = Logger.getLogger(AuthenticationResource.class.getName());

    @Inject
    Config config;
    @EJB QRSessionsEJB qrSessionsEJB;

    //The point where we receive the data from the service provider
    @POST @Path("/initAuthentication")
    @Produces(MediaType.APPLICATION_JSON)
    public Response initAuthentication(@FormParam("xmlInput") String xmlInput,
               @Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        byte[] requestBytes = Base64.getDecoder().decode(xmlInput);
        IdentityRequestDto identityRequest = new XmlMapper().readValue(requestBytes, IdentityRequestDto.class);
        log.severe("TODO");
        return Response.ok().build();
    }

    //Called from the mobile with the signed data that identifies the user and the operation
    @POST @Path("/validate")
    public Response validate(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        log.severe("TODO");
        return Response.ok().entity("TODO").build();
    }

}