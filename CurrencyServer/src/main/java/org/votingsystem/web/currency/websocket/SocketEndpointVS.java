package org.votingsystem.web.currency.websocket;


import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
//@ServerEndpoint(value = "/websocket/service", configurator = SocketConfigurator.class)
public class SocketEndpointVS {

    private static Logger log = Logger.getLogger(SocketEndpointVS.class.getSimpleName());

    /*private WebSocketService webSocketService = null;

    @OnMessage public void onTextMessage(Session session, String msg, boolean last) {
        try {
            if (session.isOpen()) {
                webSocketService.onTextMessage(session, msg, last);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
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
            log.log(Level.SEVERE,ex.getMessage(), ex);
            try {
                session.close();
            } catch (IOException ex1) { // Ignore
            }
        }
    }

    @OnError public void onError(Throwable t) {
        log.log(Level.SEVERE,t.getMessage(), t);
    }

    @OnOpen public void onOpen(Session session, EndpointConfig config) {
        if(webSocketService == null) {
            webSocketService = (WebSocketService) ApplicationContextHolder.getBean("webSocketService");
        }
        webSocketService.onOpen(session);
    }


    @OnClose public void onClose(Session session, CloseReason closeReason) {
        //log.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
        try {
            webSocketService.onClose(session, closeReason);
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
    }


    @OnMessage public void echoPongMessage(PongMessage pm) {
        // Ignored
    }*/
}