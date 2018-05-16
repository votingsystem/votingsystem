package org.votingsystem.idprovider.jaxrs;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/signature")
@Stateless
public class SignatureResourceEJB {

    private static final Logger log = Logger.getLogger(SignatureResourceEJB.class.getName());

    @Inject private Config config;
    @EJB private SignatureServiceEJB signatureService;

    @POST @Path("/validate")
    @Produces({"application/xml"})
    public Response validate(@FormParam("signedXML") String signedXML,
                                             @FormParam("withTimeStampValidation") Boolean withTimeStampValidation) throws Exception {
        log.info("withTimeStampValidation: " + withTimeStampValidation + " - signedXML: " + signedXML);
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                SignedDocumentType.SIGNED_DOCUMENT).setWithTimeStampValidation(withTimeStampValidation);
        try {
            SignedDocument signedDocument = signatureService.validateXAdESAndSave(
                    new InMemoryDocument(signedXML.getBytes()), signatureParams);
            ResponseDto response = ResponseDto.OK().setMessage("signedDocument id: " + signedDocument.getId());
            return Response.ok().entity(XML.getMapper().writeValueAsBytes(response)).build();
        } catch (Exception ex) {
            ResponseDto response = ResponseDto.ERROR(ex.getMessage());
            return Response.status(ResponseDto.SC_ERROR).entity(XML.getMapper().writeValueAsBytes(response)).build();
        }
    }
}
