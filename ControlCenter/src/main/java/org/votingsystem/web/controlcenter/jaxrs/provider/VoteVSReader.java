package org.votingsystem.web.controlcenter.jaxrs.provider;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.ejb.CMSBean;

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
public class VoteVSReader implements MessageBodyReader<CMSDto> {

    private static final Logger log = Logger.getLogger(VoteVSReader.class.getName());

    @Inject CMSBean cmsBean;

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return CMSDto.class.isAssignableFrom(aClass);
    }

    @Override
    public CMSDto readFrom(Class<CMSDto> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> multivaluedMap, InputStream inputStream) throws IOException, WebApplicationException {
        try {
            return cmsBean.validatedVote(CMSSignedMessage.FROM_PEM(inputStream));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new WebApplicationException(ex);
        }
    }
}
