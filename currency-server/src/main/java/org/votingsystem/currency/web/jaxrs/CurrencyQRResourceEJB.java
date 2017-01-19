package org.votingsystem.currency.web.jaxrs;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.votingsystem.currency.web.http.HttpSessionManager;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.BrowserCertificationDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.qr.QRRequestBundle;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.Messages;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/currency-qr")
@Stateless
public class CurrencyQRResourceEJB {

    private static final Logger log = Logger.getLogger(CurrencyQRResourceEJB.class.getName());

    @Inject private Config config;
    @EJB private QRSessionsEJB qrSessions;

    @POST @Path("/info")
    @Produces(MediaType.TEXT_XML)
    public Response info(@Context HttpServletRequest req) throws Exception {
        String uuidInfo = FileUtils.getStringFromStream(req.getInputStream());
        QRRequestBundle qrRequest = qrSessions.getOperation(uuidInfo);
        if(qrRequest != null)
            return Response.ok().entity(qrRequest.generateResponse(req, LocalDateTime.now())).build();
        else
            return Response.status(Response.Status.NOT_FOUND).entity(new XmlMapper().writeValueAsBytes(
                    new ResponseDto(ResponseDto.SC_NOT_FOUND, Messages.currentInstance().get("itemNotFoundErrorMsg")))).build();
    }

    @POST @Path("/browser-certificate")
    @Produces(MediaType.TEXT_XML)
    public Response browserCertificate(@Context HttpServletRequest req) throws Exception {
        String userUUID = FileUtils.getStringFromStream(req.getInputStream());
        BrowserCertificationDto csrRequest = (BrowserCertificationDto) HttpSessionManager.getInstance()
                .getHttpSession(userUUID).getAttribute(Constants.CSR);
        if(csrRequest != null)
            return HttpResponse.getResponse(req, Response.Status.OK.getStatusCode(), csrRequest);
        else
            return HttpResponse.getResponse(req, Response.Status.NOT_FOUND.getStatusCode(),
                    Messages.currentInstance().get("itemNotFoundErrorMsg"));
    }
}
