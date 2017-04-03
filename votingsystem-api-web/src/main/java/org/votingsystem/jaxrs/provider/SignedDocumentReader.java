package org.votingsystem.jaxrs.provider;

import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.FileUtils;

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
@Consumes(MediaType.XML)
public class SignedDocumentReader implements MessageBodyReader<SignedDocument> {

    private static final Logger log = Logger.getLogger(SignedDocumentReader.class.getName());

    @Inject
    SignatureService signatureService;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType) {
        return SignedDocument.class.isAssignableFrom(aClass);
    }

    @Override
    public SignedDocument readFrom(Class<SignedDocument> aClass, Type type, Annotation[] annotations,
            javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
            throws IOException, WebApplicationException {
        try {
            return signatureService.validateXAdESAndSave(FileUtils.getBytesFromStream(inputStream));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException("Signed document with errors");
        }
    }

}
