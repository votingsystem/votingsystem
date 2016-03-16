package org.votingsystem.web.jaxrs.provider;

import org.apache.commons.io.IOUtils;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.ejb.CMSBean;

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
@Consumes(MediaType.JSON_SIGNED_ENCRYPTED)
public class CMSEncryptedMessageReader implements MessageBodyReader<CMSMessage> {

    private static final Logger log = Logger.getLogger(CMSEncryptedMessageReader.class.getName());

    @Inject CMSBean cmsBean;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType) {
        return CMSMessage.class.isAssignableFrom(aClass);
    }

    @Override
    public CMSMessage readFrom(Class<CMSMessage> aClass, Type type, Annotation[] annotations,
               javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> multivaluedMap,
               InputStream inputStream) throws IOException, WebApplicationException {
        try {
            byte[] decryptedBytes = cmsBean.decryptCMS(IOUtils.toByteArray(inputStream));
            CMSSignedMessage signedData = new CMSSignedMessage(decryptedBytes);
            return cmsBean.validateCMS(signedData, ContentType.JSON_SIGNED).getCmsMessage();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException(ex);
        }
    }
}
