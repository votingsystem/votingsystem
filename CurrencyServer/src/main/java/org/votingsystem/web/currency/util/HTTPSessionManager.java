package org.votingsystem.web.currency.util;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@WebListener
public class HTTPSessionManager implements HttpSessionListener {

    private static Logger log = Logger.getLogger(HTTPSessionManager.class.getName());

    private static final ConcurrentHashMap<String, HttpSession> sessionMap = new ConcurrentHashMap();
    private static HTTPSessionManager INSTANCE;

    public HTTPSessionManager() {
        INSTANCE = this;
    }

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        sessionMap.put(sessionEvent.getSession().getId(), sessionEvent.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
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
}
