package org.votingsystem;

import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.socket.WebSocketSession;
import org.votingsystem.util.CurrencyOperation;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AppContext {

    private static final Map<String, WebSocketSession> socketSessionMap = new HashMap();

    private static final AppContext INSTANCE = new AppContext();

    private AppContext() { }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public <T> WebSocketSession checkWebSocketSession (DeviceDto deviceTo, T data, OperationTypeDto operation)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = socketSessionMap.get(deviceTo.getUUID());
        if(webSocketSession == null) {
            webSocketSession = new WebSocketSession(deviceTo).setUUID(
                    java.util.UUID.randomUUID().toString());
            socketSessionMap.put(webSocketSession.getUUID(), webSocketSession);
        }
        webSocketSession.setData(data);
        webSocketSession.setOperation(operation);
        return webSocketSession;
    }

    public WebSocketSession getWSSession(String sessionUUID) {
        return socketSessionMap.get(sessionUUID);
    }
}
