package org.votingsystem.web.jaxrs.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.util.JSON;

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
import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class ListReader implements MessageBodyReader<List<String>> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return List.class.isAssignableFrom(aClass);
    }

    @Override
    public List<String> readFrom(Class<List<String>> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                         MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws
            IOException, WebApplicationException {
        return JSON.getMapper().readValue(inputStream, new TypeReference<List<String>>() {});
    }


}
