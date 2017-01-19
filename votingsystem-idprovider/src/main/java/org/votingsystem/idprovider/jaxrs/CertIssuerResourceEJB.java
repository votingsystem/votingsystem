package org.votingsystem.idprovider.jaxrs;

import org.votingsystem.idprovider.ejb.CertIssuerEJB;
import org.votingsystem.model.SignedDocument;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/cert-issuer")
@Stateless
public class CertIssuerResourceEJB {

    private static final Logger log = Logger.getLogger(CertIssuerResourceEJB.class.getName());

    @Inject private CertIssuerEJB certIssuer;

    @POST @Path("/browser-csr")
    public Response browserCsr(@Context HttpServletRequest req, @Context HttpServletResponse res,
                               SignedDocument signedDocument) throws Exception {
        SignedDocument response = certIssuer.signBrowserCSR(signedDocument);
        return Response.ok().type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE).entity(response.getBody()).build();
    }

}