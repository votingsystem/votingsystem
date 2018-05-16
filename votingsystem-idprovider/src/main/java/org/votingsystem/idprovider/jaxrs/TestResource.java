package org.votingsystem.idprovider.jaxrs;

import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.idprovider.ejb.CertIssuerEJB;
import org.votingsystem.util.JSON;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
@Stateless
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getName());

    @Inject private CertIssuerEJB certIssuerEJB;
    @Inject private CmsEJB cmsEJB;

    @GET @Path("/")
    public Response test(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        ResponseDto responseDto = new ResponseDto(ResponseDto.SC_OK, "message");
        CMSSignedMessage cmsSignedMessage = cmsEJB.signDataWithTimeStamp(JSON.getMapper().writeValueAsBytes(responseDto));
        try {
            return Response.ok().entity(cmsSignedMessage.toPEM()).build();
        } catch (Exception ex) {
            ResponseDto response = ResponseDto.ERROR(ex.getMessage());
            return Response.status(ResponseDto.SC_ERROR).entity(XML.getMapper().writeValueAsBytes(response)).build();
        }
    }

    @GET @Path("/")
    public Response query(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        return Response.ok().entity("OK").build();
    }

}
