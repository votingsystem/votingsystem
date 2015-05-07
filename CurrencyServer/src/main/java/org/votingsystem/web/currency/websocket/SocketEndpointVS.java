package org.votingsystem.web.currency.websocket;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.currency.ejb.WebSocketBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

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
public class SocketEndpointVS {

    private static Logger log = Logger.getLogger(SocketEndpointVS.class.getSimpleName());

    private MessagesVS messages = null;
    @Inject WebSocketBean webSocketBean;
    @Inject ConfigVS config;

    @OnMessage
    public void onTextMessage(String msg, Session session) {
        SocketMessageDto messageDto = null;
        try {
            if (session.isOpen()) {
                messageDto = JSON.getMapper().readValue(msg, SocketMessageDto.class);
                MessagesVS.setCurrentInstance(
                        Locale.forLanguageTag(messageDto.getLocale()), config.getProperty("vs.bundleBaseName"));
                messages = MessagesVS.getCurrentInstance();
                messageDto.setSession(session);
                webSocketBean.processRequest(messageDto);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            String message = ex.getMessage();
            if(message == null) message = messages.get("socketRequestErrorMsg");
            try {
                if(messageDto != null) session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                        messageDto.getServerResponse(ResponseVS.SC_ERROR, message)));
                session.close();
            } catch (Exception ex1) {
                log.severe(ex1.getMessage());
            }
        }
    }

    @OnError public void onError(Throwable t) {
        log.log(Level.SEVERE, t.getMessage(), t);
    }

    @OnOpen public void onOpen(Session session, EndpointConfig config) {
        SessionVSManager.getInstance().put(session);
    }


    @OnClose public void onClose(Session session, CloseReason closeReason) {
        log.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
        try {
            SessionVSManager.getInstance().remove(session);
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
    }

}