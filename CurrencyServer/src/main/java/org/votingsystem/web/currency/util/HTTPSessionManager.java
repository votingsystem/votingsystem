package org.votingsystem.web.currency.util;

import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebListener
public class HTTPSessionManager implements HttpSessionListener {

    private static Logger log = Logger.getLogger(HTTPSessionManager.class.getName());

    public static final String WEBSOCKET_SESSION_KEY = "WEBSOCKET_SESSION_KEY";

    private static final ConcurrentHashMap<String, HttpSession> sessionMap = new ConcurrentHashMap();
    private static HTTPSessionManager INSTANCE;

    public HTTPSessionManager() {
        INSTANCE = this;
    }

    @Inject DAOBean dao;

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        sessionMap.put(sessionEvent.getSession().getId(), sessionEvent.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        log.info("sessionDestroyed: " + sessionEvent.getSession().getId());
        try {
            User user = (User) sessionEvent.getSession().getAttribute(PrincipalVS.USER_KEY);
            if(user != null) {
                Device device = user.getDevice();
                if(device != null && device.getCertificate() != null) {
                    dao.merge(device.getCertificate().setState(Certificate.State.SESSION_FINISHED));
                    log.info("sessionDestroyed - certificate: " + device.getCertificate().getId() + " - state: " +
                            device.getCertificate().getState());
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
        sessionMap.remove(sessionEvent.getSession().getId());
    }

    public HttpSession getHttpSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public static HTTPSessionManager getInstance() {
        return INSTANCE;
    }

    public int getSessionCount() {
        return sessionMap.size();
    }

    public Set<String> getSessionsIDSet() {
        return sessionMap.values().stream().map(session -> session.getId()).collect(Collectors.toSet());
    }

}
