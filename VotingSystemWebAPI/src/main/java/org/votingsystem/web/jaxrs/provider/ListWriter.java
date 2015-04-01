package org.votingsystem.web.jaxrs.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.util.JSON;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ListWriter implements MessageBodyWriter<List> {


    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return Map.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(List list, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        // As of JAX-RS 2.0, the method has been deprecated and the value returned by the method is ignored by a JAX-RS runtime.
        // All MessageBodyWriter implementations are advised to return -1 from the method.
        return -1;
    }

    @Override
    public void writeTo(List list, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> multivaluedMap, OutputStream out) throws IOException,
            WebApplicationException {
        ObjectMapper mapper = JSON.getInstance().getMapper();
        mapper.writeValue(out, list);
    }
}
