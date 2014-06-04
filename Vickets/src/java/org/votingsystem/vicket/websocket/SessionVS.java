package org.votingsystem.vicket.websocket;

import org.votingsystem.model.UserVS;

import javax.websocket.Session;
/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SessionVS {

    private Session session;
    private UserVS userVS;

    public SessionVS(Session session, UserVS userVS) {
        this.setSession(session);
        this.setUserVS(userVS);
    }

    public SessionVS(Session session) {
        this.setSession(session);
    }

    public SessionVS(UserVS userVS) {
        this.setUserVS(userVS);
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }
}
