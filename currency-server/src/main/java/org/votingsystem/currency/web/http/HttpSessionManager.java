package org.votingsystem.currency.web.http;

import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.Constants;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class HttpSessionManager implements HttpSessionListener {

    private static Logger log = Logger.getLogger(HttpSessionManager.class.getName());

    private static final ConcurrentHashMap<String, HttpSession> sessionMap = new ConcurrentHashMap();
    private static HttpSessionManager INSTANCE;

    public HttpSessionManager() {
        INSTANCE = this;
    }

    @PersistenceContext
    private EntityManager em;

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        String userUUID = UUID.randomUUID().toString();
        sessionEvent.getSession().setAttribute(Constants.USER_UUID, userUUID);
        log.info("session id: " + sessionEvent.getSession().getId() + " - userUUID: " + userUUID);
        sessionMap.put(userUUID, sessionEvent.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        log.info("session id: " + sessionEvent.getSession().getId() + " - userUUID: " +
                sessionEvent.getSession().getAttribute(Constants.USER_UUID));
        try {
            User user = (User) sessionEvent.getSession().getAttribute(Constants.USER_KEY);
            if(user != null) {
                Device device = user.getDevice();
                if(device != null && device.getCertificate() != null) {
                    em.merge(device.getCertificate().setState(Certificate.State.SESSION_FINISHED));
                    log.info("certificate id: " + device.getCertificate().getId() + " - state: " +
                            device.getCertificate().getState());
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
        sessionMap.remove(sessionEvent.getSession().getId());
    }

    public HttpSession getHttpSession(String userUUID) {
        return sessionMap.get(userUUID);
    }

    public static HttpSessionManager getInstance() {
        return INSTANCE;
    }

    public int getSessionCount() {
        return sessionMap.size();
    }

    public Set<String> getSessionUUIDSet() {
        return sessionMap.keySet();
    }

    public void setUserInSession(String userUUID, User user) {
        if(sessionMap.containsKey(userUUID)) {
            sessionMap.get(userUUID).setAttribute(Constants.USER_KEY, user);
        } else log.log(Level.SEVERE, "HttpSession not found - userUUID: " + userUUID);
    }

}