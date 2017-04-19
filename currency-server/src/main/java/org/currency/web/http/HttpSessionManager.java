package org.currency.web.http;

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
import java.util.HashMap;
import java.util.Map;
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
        sessionEvent.getSession().setAttribute(Constants.SESSION_UUID, userUUID);
        log.info("session id: " + sessionEvent.getSession().getId() + " - userUUID: " + userUUID);
        userSessionMap.put(userUUID, sessionEvent.getSession());
        sessionIdMap.put(sessionEvent.getSession().getId(), sessionEvent.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        log.severe("session id: " + sessionEvent.getSession().getId() + " - userUUID: " +
                sessionEvent.getSession().getAttribute(Constants.SESSION_UUID));
        try {
            //User user = (User) sessionEvent.getSession().getAttribute(Constants.USER_KEY);
            String userUUID = (String)sessionEvent.getSession().getAttribute(Constants.SESSION_UUID);
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

    public Map<String, String> getSessionUUIDSessionIdMap() {
        Map<String,String> result = new HashMap<>();
        for(Map.Entry<String, HttpSession> entry : userSessionMap.entrySet()) {
            result.put(entry.getValue().getId(), entry.getKey());
        }
        return result;
    }

    public void updateSession(String previousUserUUID, String newUserUUID, String httpSessionId, User user) {
        log.info("httpSession id: " + httpSessionId + " - previousUserUUID: " + previousUserUUID + " - newUserUUID: " + newUserUUID);
        HttpSession httpSession = sessionIdMap.get(httpSessionId);
        httpSession.setAttribute(Constants.SESSION_UUID, newUserUUID);
        httpSession.setAttribute(Constants.USER_KEY, user);
        userSessionMap.remove(previousUserUUID);
        userSessionMap.put(newUserUUID, httpSession);
    }

}