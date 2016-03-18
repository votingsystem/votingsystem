package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.logging.Logger;

@Path("/testEncryptor")
public class TestEncryptorResource {

    private static final Logger log = Logger.getLogger(TestEncryptorResource.class.getName());

    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;
    @Inject DAOBean dao;

    /*@Path("/multiSign") @POST
    public Response multiSign(CMSMessage cmsMessage, @Context ServletContext context,
                                  @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        CMSSignedMessage cmsSignedMessage = cmsBean.addSignature(cmsMessage.getCMS());
        return  Response.ok().entity(cmsSignedMessage.toPEM()).type(MediaType.JSON_SIGNED).build();
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
        if(result == null) return  Response.ok().entity(result).type(MediaType.JSON_SIGNED).build();
        else return  Response.ok().entity(result).type(MediaType.JSON_SIGNED_ENCRYPTED).build();
    }*/

}