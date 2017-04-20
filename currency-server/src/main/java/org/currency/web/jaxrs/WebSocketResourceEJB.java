package org.currency.web.jaxrs;

import org.currency.web.managed.SocketPushEvent;
import org.currency.web.websocket.SessionManager;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/web-socket")
public class WebSocketResourceEJB {

    private static final Logger log = Logger.getLogger(WebSocketResourceEJB.class.getName());

    @Inject private BeanManager beanManager;

    @POST @Path("/send-msg")
    public Response sendMessage(@Context HttpServletRequest req, @Context HttpServletResponse res,
                @FormParam("message") String message, @FormParam("deviceUUID") String deviceUUID) throws Exception {
        if(SessionManager.getInstance().hasSession(deviceUUID)) {
            SessionManager.getInstance().sendMessage(message, deviceUUID);
        } else {
            SocketPushEvent pushEvent = new SocketPushEvent(message, SocketPushEvent.Type.TO_USER).setSessionUUID(deviceUUID);
            beanManager.fireEvent(pushEvent);
        }
        return  Response.ok().entity("OK").type(MediaType.PKCS7_SIGNED).build();
    }

    @POST @Path("/send")
    public Response send(@Context HttpServletRequest req, @Context HttpServletResponse res, byte[] requestBytes)
            throws Exception {
        MessageDto message = JSON.getMapper().readValue(requestBytes, MessageDto.class);
        CurrencyOperation socketOperation = (CurrencyOperation)message.getOperation().getType();
        switch (socketOperation) {
            case MSG_TO_DEVICE:
                if(SessionManager.getInstance().hasSession(message.getDeviceToUUID())) {
                    SessionManager.getInstance().sendMessage(new String(requestBytes), message.getDeviceToUUID());
                } else {
                    SocketPushEvent pushEvent = new SocketPushEvent(new String(requestBytes),
                            SocketPushEvent.Type.TO_USER).setSessionUUID(message.getDeviceToUUID());
                    beanManager.fireEvent(pushEvent);
                }
                break;
            default:
                log.info("unprocessed socket operation: " + socketOperation);
        }
        return  Response.ok().entity("OK").build();
    }

}