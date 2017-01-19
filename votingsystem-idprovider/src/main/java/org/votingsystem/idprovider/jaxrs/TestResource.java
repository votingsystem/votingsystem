package org.votingsystem.idprovider.jaxrs;


import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.BrowserCertificationDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getName());

    @PersistenceContext EntityManager em;
    @Inject Config config;
    @EJB QRSessionsEJB qrSessionsEJB;
    @Inject private SignatureService signatureService;

    @GET @Path("/")
    @Transactional
    public Response test(@Context HttpServletRequest req) throws Exception {
        SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                SignedDocumentType.BROWSER_CERTIFICATION_REQUEST_RECEIPT).setWithTimeStampValidation(false);
        BrowserCertificationDto csrResponse = new BrowserCertificationDto().setUserUUID(UUID.randomUUID().toString());
        SignedDocument signedDocument = signatureService.signXAdESAndSave(
                XML.getMapper().writeValueAsBytes(csrResponse), signatureParams);
        return Response.ok().entity(signedDocument.getBody()).build() ;
    }

    @GET @Path("/responsePage")
    @Transactional
    public Response test2(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        req.getSession().setAttribute("responseDto", new ResponseDto().setCaption("caption").setMessage("message"));
        res.sendRedirect(req.getContextPath() + "/response.xhtml");
        return Response.ok().build();
    }

}