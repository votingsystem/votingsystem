package org.currency.web.jaxrs;

import org.currency.web.ejb.ConfigCurrencyServer;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Stateless
@Path("/testEncryptor")
public class TestEncryptorResourceEJB {

    private static final Logger log = Logger.getLogger(TestEncryptorResourceEJB.class.getName());

    @Inject private ConfigCurrencyServer config;

    @PersistenceContext
    private EntityManager em;

    @Path("/encrypted") @GET
    public Response encrypted() throws Exception {
        return javax.ws.rs.core.Response.ok().build();
    }
    /*@Path("/multiSign") @POST
    public Response multiSign(CMSMessage cmsMessage, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        return  Response.ok().entity(cmsSignedMessage.toPEM()).type(MediaType.PKCS7_SIGNED).build();
    }

    @Path("/encrypted") @POST
    public Response encrypted(CMSMessage cmsMessage, @Context ServletContext context,
                      @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EncryptedContentDto encryptedContentDto = cmsMessage.getSignedContent(EncryptedContentDto.class);
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        byte[] result = null;
        if(encryptedContentDto.getX509CertificatePEM() != null) {
            result = cmsBean.encryptToCMS(cmsSignedMessage.getEncoded(), encryptedContentDto.getX509Certificate());
        } else if(encryptedContentDto.getPublicKeyPEM() != null) {
            result = cmsBean.encryptToCMS(cmsSignedMessage.getEncoded(), encryptedContentDto.getPublicKey());
        }
        if(result == null) return  Response.ok().entity(result).type(MediaType.PKCS7_SIGNED).build();
        else return  Response.ok().entity(result).type(MediaType.PKCS7_SIGNED_ENCRYPTED).build();
    }*/

}