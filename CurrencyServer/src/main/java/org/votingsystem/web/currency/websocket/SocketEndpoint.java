package org.votingsystem.web.currency.websocket;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.currency.ejb.WebSocketBean;
import org.votingsystem.web.currency.util.PrincipalVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@ServerEndpoint(value = "/websocket/service", configurator = SocketConfigurator.class)
public class SocketEndpoint {

    private static Logger log = Logger.getLogger(SocketEndpoint.class.getName());

    private MessagesVS messages = null;
    @Inject WebSocketBean webSocketBean;
    @Inject DAOBean dao;
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
        SessionManager.getInstance().put(session);
        try {
            HttpSession httpSession = ((HttpSession)session.getUserProperties().get(HttpSession.class.getName()));
            if(httpSession != null) { //connecting from browser
                Set<Session> sessionSet = (Set<Session>) httpSession.getAttribute(Session.class.getName());
                if(sessionSet == null) {
                    sessionSet = new HashSet<>();
                    httpSession.setAttribute(Session.class.getName(), sessionSet);
                }
                sessionSet.add(session);
            }
            Device device = new Device(SessionManager.getInstance().getAndIncrementBrowserDeviceId())
                    .setType(Device.Type.BROWSER).setDeviceId(UUID.randomUUID().toString());
            session.getUserProperties().put("device", device);
            SessionManager.getInstance().putBrowserDevice(session, device);
            session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(
                    SocketMessageDto.INIT_SESSION_RESPONSE(device.getId())));
        }catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


    @OnClose public void onClose(Session session, CloseReason closeReason) {
        log.info(String.format("Session %s closed because of %s", session.getId(), closeReason.getCloseCode() + " - " +
            closeReason.getReasonPhrase()));
        try {
            Device device = (Device) session.getUserProperties().get("device");
            if(device != null && device.getCertificate() != null) {
                dao.merge(device.getCertificate().setState(Certificate.State.SESSION_FINISHED));
                log.info("session finished - certificate: " + device.getCertificate().getId() + " - state: " +
                        device.getCertificate().getState());
            }
            HttpSession httpSession = ((HttpSession)session.getUserProperties().get(HttpSession.class.getName()));
            if(httpSession != null) { //disconnecting from browser
                httpSession.removeAttribute(PrincipalVS.USER_KEY);
            }
            SessionManager.getInstance().remove(session);
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
    }

}