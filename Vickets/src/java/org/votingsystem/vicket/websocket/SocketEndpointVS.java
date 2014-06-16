package org.votingsystem.vicket.websocket;

import org.apache.log4j.Logger;
import org.votingsystem.vicket.util.ApplicationContextHolder;
import org.votingsystem.vicket.service.WebSocketService;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/websocket/service")
public class SocketEndpointVS {

    private static Logger logger = Logger.getLogger(SocketEndpointVS.class);

    private WebSocketService webSocketService = null;

    @OnMessage public void onTextMessage(Session session, String msg, boolean last) {
        try {
            if (session.isOpen()) {
                webSocketService.onTextMessage(session, msg, last);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            try {
                session.close();
            } catch (IOException e1) { // Ignore
             }
        }
    }

    @OnMessage public void onBinaryMessage(Session session, ByteBuffer bb, boolean last) {
        try {
            if (session.isOpen()) {
                webSocketService.onBinaryMessage(session, bb, last);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            try {
                session.close();
            } catch (IOException ex1) { // Ignore
            }
        }
    }

    @OnError public void onError(Throwable t) {
        logger.error(t.getMessage(), t);
    }

    @OnOpen public void onOpen(Session session) {
        if(webSocketService == null) {
            webSocketService = (WebSocketService) ApplicationContextHolder.getBean("webSocketService");
        }
        webSocketService.onOpen(session);
    }

    @OnClose public void onClose(Session session, CloseReason closeReason) {
        //logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
        webSocketService.onClose(session, closeReason);
    }

    /**
     * Process a received pong. This is a NO-OP.
     *
     * @param pm    Ignored.
     */
    @OnMessage public void echoPongMessage(PongMessage pm) {
        // NO-OP
    }
}