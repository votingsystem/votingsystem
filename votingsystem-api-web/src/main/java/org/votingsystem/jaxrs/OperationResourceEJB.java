package org.votingsystem.jaxrs;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.AdminRequestDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.InsufficientPrivilegesException;
import org.votingsystem.xml.XML;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/operation")
@Singleton
public class OperationResourceEJB {

    private static final Logger log = Logger.getLogger(OperationResourceEJB.class.getName());

    @Inject private Config config;
    @Inject private SignatureServiceEJB signatureService;
    @Inject private SignerInfoService signerInfoService;
    @Inject private TrustedServicesEJB trustedServices;

    @POST @Path("/process")
    public Response checkUser(@FormParam("signedXML") String signedXML, @FormParam("userType") String userTypeStr,
                              @Context HttpServletRequest req) throws Exception {
        ResponseDto response = null;
        User.Type userType = User.Type.valueOf(userTypeStr);
        SignatureParams signatureParams = new SignatureParams(null, userType,
                SignedDocumentType.ADMIN_CHECK_USER).setWithTimeStampValidation(true);
        SignedDocument signedDocument = validateRequest(new InMemoryDocument(signedXML.getBytes()), signatureParams);
        User user = signedDocument.getSignatures().iterator().next().getSigner();
        AdminRequestDto adminRequest = XML.getMapper().readValue(signedDocument.getBody().getBytes(), AdminRequestDto.class);
        switch (adminRequest.getOperationType()) {
            case CERT_USER_CHECK:
                User checkedUser = signerInfoService.checkSigner(adminRequest.getKey().getX509Certificate(),
                        User.Type.ID_CARD_USER, null);
                response = ResponseDto.OK().setMessage("user " + checkedUser.getId() + " - cert: " +
                        checkedUser.getX509Certificate().getSerialNumber().longValue());
                break;
            case GET_METADATA:
                Set<String> trustedServiceSet = trustedServices.loadTrustedServices();
                response = ResponseDto.OK().setMessage(trustedServiceSet.toString());
                break;
        }
        return HttpResponse.getResponse(req, response.getStatusCode(), response);
    }


     private SignedDocument validateRequest(InMemoryDocument document, SignatureParams signatureParams) throws Exception {
         SignedDocument signedDocument = signatureService.validateXAdESAndSave(document, signatureParams);
         User user = signedDocument.getFirstSignature().getSigner();
         if(!Arrays.equals(config.getSigningCert().getEncoded(), user.getX509Certificate().getEncoded())) {
            throw new InsufficientPrivilegesException("ERROR - Insufficient privileges exception");
         }
         return signedDocument;
     }

}
