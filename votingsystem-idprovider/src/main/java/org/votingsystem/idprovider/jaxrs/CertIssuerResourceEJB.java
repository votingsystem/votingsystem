package org.votingsystem.idprovider.jaxrs;

import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.FileUtils;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.HttpRequest;
import org.votingsystem.http.MediaType;
import org.votingsystem.idprovider.ejb.CertIssuerEJB;

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
    @Inject
    CmsEJB cmsBean;
    @Inject SignatureService signatureService;

    @POST @Path("/session-csr")
    public Response sessionCsrXML(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        String reqContentType = HttpRequest.getContentType(req, false);
        SignedDocument signedDocument = null;
        if(MediaType.PKCS7_SIGNED.equals(reqContentType)) {
            CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(req.getInputStream());
            signedDocument = cmsBean.validateCMS(cmsSignedMessage, null).getCmsDocument();
        } else {
            signedDocument = signatureService.validateXAdESAndSave(FileUtils.getBytesFromStream(req.getInputStream()));
        }
        SignedDocument response = certIssuer.signSessionCSR(signedDocument);
        return Response.ok().type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE).entity(response.getBody()).build();
    }

}