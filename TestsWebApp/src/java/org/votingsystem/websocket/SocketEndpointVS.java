package org.votingsystem.websocket;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.simulation.WebSocketService;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.OnMessage;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/websocket/service")
public class SocketEndpointVS {

    private static Logger logger = Logger.getLogger(SocketEndpointVS.class);

    private WebSocketService webSocketService = null;

    @OnMessage public void onTextMessage(Session session, String msg, boolean last) {
        logger.debug("onTextMessage:" + msg);
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

    @OnOpen public void onOpen(Session session) {
        if(webSocketService == null) {
            webSocketService = ((WebSocketService)((GrailsApplication) ApplicationContextHolder.
                    getBean("grailsApplication")).getMainContext().getBean("webSocketService"));
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