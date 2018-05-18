package org.currency.web.jaxrs;

import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.ejb.Config;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.SignedDocument;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/test-pkcs7")
public class TestPKCS7Resource {

    private static final Logger log = Logger.getLogger(TestPKCS7Resource.class.getName());

    @Inject Config config;
    @Inject
    CmsEJB cmsBean;


    @POST @Path("/")
    public Response test(@Context HttpServletRequest req, @Context HttpServletResponse res,
                         SignedDocument signedDocument) throws Exception {
        log.info("signedDocument: " + signedDocument.getBody());
        return new HttpResponse().getResponse(req, ResponseDto.SC_OK, "OK");
    }

    @Path("/sign") @POST
    public Response sign(CMSDocument cmsMessage, @Context HttpServletRequest req, @Context HttpServletResponse res)
            throws Exception {
        log.info("SignedContentStr: " + cmsMessage.getCmsMessage().getSignedContentStr());
        return  Response.ok().entity("OK").type(MediaType.PKCS7_SIGNED).build();
    }

    @POST @Path("/currency")
    public Response testCMS(@Context HttpServletRequest req, @Context HttpServletResponse res,
                            CMSDocument signedDocument) throws Exception {
        log.info("signedDocument: " + signedDocument.getBody());
        return new HttpResponse().getResponse(req, ResponseDto.SC_OK, "OK");
    }

    @Path("/multiSign") @POST
    public Response multiSign(CMSDocument cmsMessage, @Context HttpServletRequest req, @Context HttpServletResponse res)
            throws Exception {
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        return  Response.ok().entity(cmsSignedMessage.toPEM()).type(MediaType.PKCS7_SIGNED).build();
    }

    @Path("/encrypted") @POST
    public Response encrypted(CMSDocument cmsMessage, @Context HttpServletRequest req, @Context HttpServletResponse res)
            throws Exception {
        MessageDto encryptedContentDto = cmsMessage.getSignedContent(MessageDto.class);
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        byte[] result = null;
        if(encryptedContentDto.getCertificatePEM() != null) {
            result = cmsBean.encryptToCMS(cmsSignedMessage.getEncoded(),
                    PEMUtils.fromPEMToX509Cert(encryptedContentDto.getCertificatePEM().getBytes()));
        } else if(encryptedContentDto.getPublicKeyPEM() != null) {
            result = cmsBean.encryptToCMS(cmsSignedMessage.getEncoded(),
                    PEMUtils.fromPEMToRSAPublicKey(encryptedContentDto.getPublicKeyPEM()));
        }
        if(result == null)
            return  Response.ok().entity(result).type(MediaType.PKCS7_SIGNED).build();
        else
            return  Response.ok().entity(result).type(MediaType.PKCS7_SIGNED_ENCRYPTED).build();
    }

}