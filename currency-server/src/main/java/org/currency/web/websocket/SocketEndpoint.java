package org.currency.web.websocket;

import org.currency.web.ejb.ConfigCurrencyServer;
import org.currency.web.ejb.WebSocketEJB;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.socket.SocketRequest;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;
import org.votingsystem.dto.ResponseDto;

import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@ServerEndpoint(value = "/websocket/service", configurator = SocketConfigurator.class)
public class SocketEndpoint {

    private static Logger log = Logger.getLogger(SocketEndpoint.class.getName());

    @Inject private WebSocketEJB webSocketBean;
    @Inject private ConfigCurrencyServer config;

    @OnMessage
    public void onTextMessage(String msg, Session session) {
        MessageDto messageDto = null;
        try {
            if (session.isOpen()) {
                messageDto = XML.getMapper().readValue(msg, MessageDto.class);
                Messages.setCurrentInstance(
                        Locale.forLanguageTag(messageDto.getLocale()), Constants.BUNDLE_BASE_NAME);
                webSocketBean.processRequest(new SocketRequest(messageDto, msg).setSession(session));
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            String message = ex.getMessage();
            if(message == null)
                message = Messages.currentInstance().get("socketRequestErrorMsg");
            try {
                if(messageDto != null) session.getBasicRemote().sendText(XML.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseDto.SC_ERROR, message)));
                session.close();
            } catch (Exception ex1) {
                log.severe(ex1.getMessage());
            }
        }
    }

    @OnError public void onError(Throwable t) {
        log.log(Level.SEVERE, t.getMessage(), t);
    }

    @OnOpen public void onOpen(Session session, EndpointConfig config) { }

    @OnClose public void onClose(Session session, CloseReason closeReason) {
        log.info(String.format("Session %s closed because of %s", session.getId(), closeReason.getCloseCode() + " - " +
            closeReason.getReasonPhrase()));
        SessionManager.getInstance().remove(session);
    }

}