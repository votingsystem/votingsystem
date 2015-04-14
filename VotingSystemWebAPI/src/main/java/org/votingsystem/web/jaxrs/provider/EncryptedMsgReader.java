package org.votingsystem.web.jaxrs.provider;

import org.apache.commons.io.IOUtils;
import org.votingsystem.dto.EncryptedMsgDto;
import org.votingsystem.util.JSON;
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
@Consumes(MediaTypeVS.ENCRYPTED)
public class EncryptedMsgReader implements MessageBodyReader<EncryptedMsgDto> {

    private static final Logger log = Logger.getLogger(EncryptedMsgReader.class.getSimpleName());

    @Inject SignatureBean signatureBean;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return EncryptedMsgDto.class.isAssignableFrom(aClass);
    }

    @Override
    public EncryptedMsgDto readFrom(Class<EncryptedMsgDto> aClass, Type type, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        try {
            byte[] decryptedBytes = signatureBean.decryptMessage(IOUtils.toByteArray(inputStream));
            return JSON.getMapper().readValue(decryptedBytes, EncryptedMsgDto.class);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException(ex);
        }
    }
}
