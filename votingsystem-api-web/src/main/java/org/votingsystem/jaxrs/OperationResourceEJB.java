package org.votingsystem.jaxrs;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.dto.AdminRequestDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/operation")
@Singleton
public class OperationResourceEJB {

    private static final Logger log = Logger.getLogger(OperationResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private Config config;
    @Inject private SignatureServiceEJB signatureService;
    @Inject private SignerInfoService signerInfoService;
    @Inject private TrustedServicesEJB trustedServices;

    @POST @Path("/process")
    public Response checkUser(@FormParam("signedXML") String signedXML, @FormParam("userType") String userTypeStr,
                              @Context HttpServletRequest req) throws Exception {
        User.Type userType = User.Type.valueOf(userTypeStr);
        SignatureParams signatureParams = new SignatureParams(null, userType,
                OperationType.ADMIN_OPERATION).setWithTimeStampValidation(true);

        SignedDocument signedDocument = signatureService.validateXAdESAndSave(
                new InMemoryDocument(signedXML.getBytes()), signatureParams);
        User user = signedDocument.getSignatures().iterator().next().getSigner();
        if(!config.isAdmin(user))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        AdminRequestDto adminRequest = new XML().getMapper().readValue(signedDocument.getBody().getBytes(), AdminRequestDto.class);
        String message = null;
        log.info("Admin operation: " + adminRequest.getOperationType());
        switch (adminRequest.getOperationType()) {
            case CERT_USER_CHECK:
                User checkedUser = signerInfoService.checkSigner(adminRequest.getKey().getX509Certificate(),
                        User.Type.ID_CARD_USER, null);
                message = "user " + checkedUser.getId() + " - cert: " + checkedUser.getX509Certificate().getSerialNumber().longValue();
                break;
            case GET_METADATA:
                Set<String> trustedServiceSet = trustedServices.getLoadedEntities();
                message = trustedServiceSet.toString();
                break;
            case LOAD_TRUSTED_ENTITY:
                trustedServices.addTrustedEntity(adminRequest.getEntityId());
                message = "added trusted entity: " + adminRequest.getEntityId();
                break;
            default:
                log.severe("Unknown operation: " + adminRequest.getOperationType());
        }
        signedDocument =  em.find(SignedDocument.class, signedDocument.getId());
        signedDocument.setOperationType(adminRequest.getOperationType());
        em.persist(signedDocument);
        return Response.ok().entity(message).build();
    }

}