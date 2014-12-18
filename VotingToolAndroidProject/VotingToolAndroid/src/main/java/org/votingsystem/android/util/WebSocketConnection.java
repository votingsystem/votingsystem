package org.votingsystem.android.util;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketConnection {
    private String sessionId = null;
    private Long userId = null;

    public WebSocketConnection(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
