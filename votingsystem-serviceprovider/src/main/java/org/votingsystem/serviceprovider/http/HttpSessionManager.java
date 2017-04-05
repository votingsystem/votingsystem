package org.votingsystem.serviceprovider.http;

import org.votingsystem.ejb.QRSessionsEJB;
import org.votingsystem.util.Constants;

import javax.ejb.EJB;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebListener
public class HttpSessionManager implements HttpSessionListener {

    private static Logger log = Logger.getLogger(HttpSessionManager.class.getName());

    @EJB private QRSessionsEJB qrSessions;

    private static HttpSessionManager INSTANCE;

    public HttpSessionManager() {
        INSTANCE = this;
    }

    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        log.info("Session id: " + sessionEvent.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        Set<String> qrOperations = (Set<String>) sessionEvent.getSession().getAttribute(Constants.QR_OPERATIONS);
        log.info("Session id: " + sessionEvent.getSession().getId() + " - closing qrOperations: " + qrOperations);
        if(qrOperations != null) {
            for(String qrOperation:qrOperations) {
                qrSessions.removeOperation(qrOperation);
            }
        }
    }

    public static HttpSessionManager getInstance() {
        return INSTANCE;
    }

}
