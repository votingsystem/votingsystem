package org.votingsystem.util;

import org.votingsystem.dto.SocketMessageDto;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CoreSignal {

    public static final String NEW_TRANSACTIONVS = "transactionvs-new";

    public static String getWebSocketCoreSignalJSCommand(Object message, SocketMessageDto.ConnectionStatus status) {
        Map coreSignal = new HashMap<>();
        Map messageMap = new HashMap();
        messageMap.put("socketStatus", status.toString());
        messageMap.put("message", message);
        //this.fire('iron-signal', {name: "vs-websocket-message", data: messageMap});
        coreSignal.put("name", "vs-websocket-message");
        coreSignal.put("data", messageMap);
        String jsCommand = null;
        try {
            jsCommand = "fireCoreSignal('" + Base64.getEncoder().encodeToString(
                    JSON.getMapper().writeValueAsString(messageMap).getBytes("UTF-8")) + "')";
        } catch (Exception ex) { ex.printStackTrace(); }
        return jsCommand;
    }

}
