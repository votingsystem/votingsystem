package org.votingsystem.cooin.websocket;

import org.apache.log4j.Logger;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SocketConfigurator extends ServerEndpointConfig.Configurator {

    private static Logger log = Logger.getLogger(SocketConfigurator.class);

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {  }
    @Override public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

}
