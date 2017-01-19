package org.votingsystem.jaxrs.provider;

import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.FileUtils;
import org.votingsystem.xml.XML;

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

    @Inject SignatureService signatureService;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, javax.ws.rs.core.MediaType mediaType) {
        return SignedDocument.class.isAssignableFrom(aClass);
    }

    @Override
    public SignedDocument readFrom(Class<SignedDocument> aClass, Type type, Annotation[] annotations,
            javax.ws.rs.core.MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
            throws IOException, WebApplicationException {
        try {
            byte[] requestBytes = FileUtils.getBytesFromStream(inputStream);
            OperationCheckerDto checkerDto = XML.getMapper().readValue(requestBytes, OperationCheckerDto.class);
            if(checkerDto.getOperation() == null)
                throw new WebApplicationException("Signed document without operation info");
            SignatureParams signatureParams = null;
            if(checkerDto.getOperation().isCurrencyOperation()) {
                switch ((CurrencyOperation)checkerDto.getOperation().getType()) {
                    case BROWSER_CERTIFICATION:
                        signatureParams = new SignatureParams(checkerDto.getOperation().getEntityId(),
                                User.Type.IDENTITY_SERVER, SignedDocumentType.BROWSER_CERTIFICATION_REQUEST_RECEIPT)
                                .setWithTimeStampValidation(true);
                        break;
                    default:
                        signatureParams = new SignatureParams(checkerDto.getOperation().getEntityId(),
                            User.Type.ID_CARD_USER, SignedDocumentType.SIGNED_DOCUMENT).setWithTimeStampValidation(true);
                        break;
                }
            } else if(checkerDto.getOperation().isOperationType()) {
                signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                        SignedDocumentType.SIGNED_DOCUMENT).setWithTimeStampValidation(true);
            }
            return signatureService.validateXAdESAndSave(new InMemoryDocument(requestBytes), signatureParams);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException("Signed document with errors");
        }
    }

}
