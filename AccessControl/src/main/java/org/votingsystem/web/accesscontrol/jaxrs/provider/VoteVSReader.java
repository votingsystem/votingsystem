package org.votingsystem.web.accesscontrol.jaxrs.provider;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.signature.smime.SMIMEMessage;
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
@Consumes(MediaTypeVS.VOTE)
public class VoteVSReader implements MessageBodyReader<MessageSMIME> {

    private static final Logger log = Logger.getLogger(VoteVSReader.class.getSimpleName());

    @Inject SignatureBean signatureBean;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return MessageSMIME.class.isAssignableFrom(aClass);
    }

    @Override
    public MessageSMIME readFrom(Class<MessageSMIME> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                 MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        try {
            return signatureBean.validatedVoteFromControlCenter(new SMIMEMessage(inputStream)).getMessageSMIME();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException(ex);
        }
    }
}
