package org.votingsystem.jaxrs.provider;

import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.IOUtils;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Provider
@Consumes(MediaType.PKCS7_SIGNED)
public class CMSMessageReader implements MessageBodyReader<CMSDocument> {

    private static final Logger log = Logger.getLogger(CMSMessageReader.class.getName());

    @Inject
    CmsEJB cmsBean;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType) {
        return CMSDocument.class.isAssignableFrom(aClass);
    }

    @Override
    public CMSDocument readFrom(Class<CMSDocument> aClass, Type type, Annotation[] annotations,
                javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> multivaluedMap,
                InputStream inputStream) throws IOException, WebApplicationException {
        try {
            String contentType = multivaluedMap.getFirst("Content-Type");
            if(MediaType.PKCS7_SIGNED.equals(contentType)) {
                return cmsBean.validateCMS(CMSSignedMessage.FROM_PEM(inputStream), null).getCmsDocument();
            } else if (MediaType.PKCS7_SIGNED_ENCRYPTED.equals(contentType)) {
                byte[] decryptedBytes = cmsBean.decryptCMS(IOUtils.toByteArray(inputStream));
                CMSSignedMessage signedData = new CMSSignedMessage(decryptedBytes);
                return cmsBean.validateCMS(signedData, null).getCmsDocument();
            } else throw new ValidationException("Invalid content type: " + contentType);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException(ex.getMessage(), ex);
        }
    }
}
