package org.votingsystem.idprovider.jaxrs;

import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.idprovider.ejb.CertIssuerEJB;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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


    @POST @Path("/mobile-browser-session")
    public Response mobileBrowserSession(@Context HttpServletRequest req, CMSDocument signedDocument) throws Exception {
        SignedDocument response = certIssuer.signSessionCSR(signedDocument);
        return Response.ok().type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE).entity(response.getBody()).build();
    }

    @Path("/register-device")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response registerDevice(@Context HttpServletRequest req, CMSDocument cmsMessage) throws Exception {
        User signer = cmsMessage.getSignatures().iterator().next().getSigner();
        if(signer.getCertificateCA().getType() != Certificate.Type.CERTIFICATE_AUTHORITY_ID_CARD)
            throw new ValidationException("the request must be signed with the user id card");
        RegisterDto registerDto = cmsMessage.getSignedContent(RegisterDto.class);
        if(registerDto.getOperation().getType() != CurrencyOperation.REGISTER_DEVICE)
            throw new ValidationException("Request invalid. Expected document type REGISTER_DEVICE " +
                    "found " + registerDto.getOperation().getType());
        if(registerDto.getCsr() == null)
            throw new ValidationException("Request invalid. CSR is missing");
        Certificate certificate = certIssuer.signUserCert(cmsMessage, registerDto);
        byte[] issuedCert = PEMUtils.getPEMEncoded(certificate.getX509Certificate());
        registerDto.setIssuedCertificate(new String(issuedCert));
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(registerDto)).build();
    }

}