package org.votingsystem.web.jaxrs.provider;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
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
@Consumes(MediaTypeVS.JSON_SIGNED)
public class MessageCMSReader implements MessageBodyReader<MessageCMS> {

    private static final Logger log = Logger.getLogger(MessageCMSReader.class.getName());

    @Inject
    SignatureBean signatureBean;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return MessageCMS.class.isAssignableFrom(aClass);
    }

    @Override
    public MessageCMS readFrom(Class<MessageCMS> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        try {
            return signatureBean.validateCMS(CMSSignedMessage.FROM_PEM(inputStream), ContentTypeVS.JSON_SIGNED).getMessageCMS();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException(ex);
        }
    }
}
