package org.votingsystem.idprovider.jaxrs;

import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.HttpRequest;
import org.votingsystem.http.MediaType;
import org.votingsystem.idprovider.ejb.CertIssuerEJB;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
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

    @Transactional
    @PermitAll
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