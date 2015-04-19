package org.votingsystem.web.currency.jaxrs.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.util.JSON;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Consumes(MediaType.APPLICATION_JSON)
public class CurrencyBatchReader implements MessageBodyReader<CurrencyBatch> {


    private static final Logger log = Logger.getLogger(CurrencyBatchReader.class.getSimpleName());

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return CurrencyBatch.class.isAssignableFrom(aClass);
    }

    @Override
    public CurrencyBatch readFrom(Class<CurrencyBatch> aClass, Type type, Annotation[] annotations,
                 MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream in) throws IOException, WebApplicationException {
        CurrencyBatchDto dto = JSON.getMapper().readValue(in, CurrencyBatchDto.class);
        try {
            return dto.loadCurrencyBatch();
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

}
