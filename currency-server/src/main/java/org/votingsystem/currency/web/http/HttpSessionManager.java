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

    private static final ConcurrentHashMap<String, HttpSession> userSessionMap = new ConcurrentHashMap();
    private static final ConcurrentHashMap<String, HttpSession> sessionIdMap = new ConcurrentHashMap();

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
        userSessionMap.put(userUUID, sessionEvent.getSession());
        sessionIdMap.put(sessionEvent.getSession().getId(), sessionEvent.getSession());
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
            String userUUID = (String)sessionEvent.getSession().getAttribute(Constants.USER_KEY);
            userSessionMap.remove(userUUID);
            sessionIdMap.remove(sessionEvent.getSession().getId());
        } catch (Exception ex) {
            log.log(Level.SEVERE,"EXCEPTION CLOSING CONNECTION: " + ex.getMessage());
        }
    }

    public HttpSession getHttpSession(String userUUID) {
        return userSessionMap.get(userUUID);
    }

    public static HttpSessionManager getInstance() {
        return INSTANCE;
    }

    public int getSessionCount() {
        return userSessionMap.size();
    }

    public Set<String> getSessionUUIDSet() {
        return userSessionMap.keySet();
    }

    public void updateSession(String previousUserUUID, String newUserUUID, String httpSessionId, User user) {
        log.info("httpSession id: " + httpSessionId + " - previousUserUUID: " + previousUserUUID + " - newUserUUID: " + newUserUUID);
        HttpSession httpSession = sessionIdMap.get(httpSessionId);
        httpSession.setAttribute(Constants.USER_UUID, newUserUUID);
        httpSession.setAttribute(Constants.USER_KEY, user);
        userSessionMap.remove(previousUserUUID);
        userSessionMap.put(newUserUUID, httpSession);
    }

}