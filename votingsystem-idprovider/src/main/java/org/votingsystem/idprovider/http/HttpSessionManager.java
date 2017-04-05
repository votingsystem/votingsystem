package org.votingsystem.idprovider.http;

import org.votingsystem.ejb.QRSessionsEJB;

import javax.ejb.EJB;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Logger;

@WebListener
public class HttpSessionManager implements HttpSessionListener {

    private static Logger log = Logger.getLogger(HttpSessionManager.class.getName());

    private static HttpSessionManager INSTANCE;

    public HttpSessionManager() {
        INSTANCE = this;
    }

    @EJB private QRSessionsEJB asyncSessionHolder;

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        log.info("Session id: " + sessionEvent.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        log.info("Session id: " + sessionEvent.getSession().getId());
    }

    public static HttpSessionManager getInstance() {
        return INSTANCE;
    }

}
