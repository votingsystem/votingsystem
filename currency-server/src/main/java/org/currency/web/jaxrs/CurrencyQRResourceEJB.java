package org.currency.web.jaxrs;

import org.currency.web.http.HttpSessionManager;
import org.votingsystem.dto.indentity.PublickeyDto;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.qr.QRRequestBundle;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
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
    public Response info(@Context HttpServletRequest req, @FormParam("UUID") String UUID,
                         @FormParam("operation") String operation) throws Exception {
        if(operation != null) {
            switch (operation) {
                case QRUtils.GEN_BROWSER_CERTIFICATE:
                    HttpSession httpSession = HttpSessionManager.getInstance().getHttpSession(UUID);
                    if(httpSession != null) {
                        PublickeyDto browserPublickey = (PublickeyDto) httpSession.
                                getAttribute(Constants.BROWSER_PLUBLIC_KEY);
                        if(browserPublickey != null) {
                            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                                    browserPublickey)).build();
                        } else httpSession.invalidate();
                    }
                    return Response.status(Response.Status.NOT_FOUND).entity(
                            Messages.currentInstance().get("itemNotFoundErrorMsg")).build();
                case QRUtils.MESSAGE_INFO:
                    break;
                case QRUtils.CURRENCY_SEND:
                    break;
            }
        }
        QRRequestBundle qrRequest = qrSessions.getOperation(UUID);
        if(qrRequest != null)
            return Response.ok().entity(qrRequest.generateResponse(req, LocalDateTime.now())).build();
        else
            return Response.status(Response.Status.NOT_FOUND).entity(
                    Messages.currentInstance().get("itemNotFoundErrorMsg")).build();
    }

    /**
     * Called from the browser to provide the public key that encrypts the message the mobile sends to the browser with the
     * 'browser session certificate' and the private key
     * @param req
     * @param csrRequestBytes
     * @return
     * @throws Exception
     */
    @POST @Path("/put-publickey")
    @Produces(MediaType.TEXT_PLAIN)
    public Response publickey(@Context HttpServletRequest req, byte[] csrRequestBytes) throws Exception {
        PublickeyDto publickeyDto = JSON.getMapper().readValue(csrRequestBytes, PublickeyDto.class);
        req.getSession().setAttribute(Constants.BROWSER_PLUBLIC_KEY, publickeyDto);
        return Response.ok().entity("OK").build();
    }

}
