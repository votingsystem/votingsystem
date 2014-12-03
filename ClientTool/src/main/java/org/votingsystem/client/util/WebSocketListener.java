package org.votingsystem.client.util;

import org.votingsystem.util.WebSocketMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface WebSocketListener {

    public void consumeWebSocketMessage(WebSocketMessage message);
    public void setConnectionStatus(WebSocketMessage.ConnectionStatus status);

}
