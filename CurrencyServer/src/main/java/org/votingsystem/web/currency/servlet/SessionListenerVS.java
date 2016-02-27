package org.votingsystem.web.currency.servlet;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@WebListener
public class SessionListenerVS  implements HttpSessionListener {

    private static Logger log = Logger.getLogger(SessionListenerVS.class.getName());

    private ConcurrentHashMap<String, HttpSession> sessionMap = null;
    private static SessionListenerVS INSTANCE;

    public SessionListenerVS() {
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

    public static SessionListenerVS getInstance() {
        return INSTANCE;
    }

    public int getSessionCount() {
        return sessionMap.size();
    }
}
