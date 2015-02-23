package org.votingsystem.client.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.client.util.WebSocketSession;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.KeyStoreUtil;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(WebSocketService.class);

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private String connectionMessage = null;

    private WebSocketService(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
        this.targetServer = targetServer;
        if(targetServer.getWebSocketURL().startsWith("wss")) {
            log.debug("settings for SECURE connetion");
            try {
                KeyStore p12Store = KeyStore.getInstance("PKCS12");
                p12Store.load(null, null);
                for(X509Certificate serverCert: sslServerCertCollection) {
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                }
                byte[] p12KeyStoreBytes = KeyStoreUtil.getBytes(p12Store, keyStorePassw.toCharArray());
                // Grizzly ssl configuration
                SSLContextConfigurator sslContext = new SSLContextConfigurator();
                sslContext.setTrustStoreType("PKCS12");
                sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                sslContext.setTrustStorePass(keyStorePassw);
                SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, true, false, false);
                client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        } else log.debug("settings for INSECURE connection");
        instance = this;
    }

    public static WebSocketService getInstance() {
        try {
            if(instance == null) instance =  new WebSocketService(ContextVS.getInstance().getVotingSystemSSLCerts(),
                    ContextVS.getInstance().getCooinServer());
        } catch(Exception ex) { log.error(ex.getMessage(), ex);}
        return instance;
    }


    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }

    public static class EndpointConfigurator extends ClientEndpointConfig.Configurator {

        @Override public void beforeRequest(Map<String, List<String>> headers) {
            //headers.put("Cookie", Arrays.asList("sessionVS=7180db71-3331-4e57-a448-5e7755e5dd3c"));
            headers.put("Origin", Arrays.asList(ContextVS.getInstance().getCooinServer().getServerURL()));
        }

        @Override public void afterResponse(HandshakeResponse handshakeResponse) {
            //final Map<String, List<String>> headers = handshakeResponse.getHeaders();
        }
    }

    @ClientEndpoint(configurator = EndpointConfigurator.class)
    public class WSEndpoint {

        @OnOpen public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText(connectionMessage);
            WebSocketService.this.session = session;
        }

        @OnClose public void onClose(Session session, CloseReason closeReason) {
            broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
            SessionService.getInstance().setIsConnected(false);
        }

        @OnMessage public void onMessage(String message) {
            consumeMessage(message);
        }
    }

    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                log.debug("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
                client.connectToServer(new WSEndpoint(), URI.create(targetServer.getWebSocketURL()));
            }catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            return null;
        }
    }

    public void close() {
        log.debug("close");
        if(session != null && session.isOpen()) {
            try {session.close();}
            catch(Exception ex) {log.error(ex.getMessage(), ex);}
        } else broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
    }

    public void sendMessage(String message) throws IOException {
        if(session != null && session.isOpen()) session.getBasicRemote().sendText(message);
        else {
            connectionMessage = message;
            this.restart();
        }
    }

    private void consumeMessage(final String messageStr){
        try {
            WebSocketMessage socketMsg = new WebSocketMessage((JSONObject) JSONSerializer.toJSON(messageStr));
            log.debug("consumeMessage - type: " + socketMsg.getOperation() +
                    " - status: " + socketMsg.getStatusCode());
            WebSocketSession socketSession = VotingSystemApp.getInstance().getWSSession(socketMsg.getUUID());
            if(socketMsg.getStatusCode() != null && ResponseVS.SC_ERROR == socketMsg.getStatusCode()) {
                showMessage(socketMsg.getStatusCode(), socketMsg.getMessage());
                return;
            }
            if(socketMsg.isEncrypted() && socketSession != null)
                socketMsg.decryptMessage(socketSession.getAESParams());
            switch(socketMsg.getOperation()) {
                case INIT_VALIDATED_SESSION:
                    BrowserVS.getInstance().execCommandJS(
                            socketMsg.getWebSocketCoreSignalJSCommand(WebSocketMessage.ConnectionStatus.OPEN));
                    break;
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().newMessage(new InboxMessage(socketMsg));
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    SessionService.setSignResponse(socketMsg);
                    break;
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null && socketSession.getTypeVS() != null) {
                        switch(socketSession.getTypeVS()) {
                            case MESSAGEVS_SIGN:
                                SessionService.setSignResponse(socketMsg);
                                return;
                        }
                    }
                default: log.debug("unprocessed socketMsg");
            }
        } catch(Exception ex) { log.error(ex.getMessage(), ex);}
    }

    private void broadcastConnectionStatus(WebSocketMessage.ConnectionStatus status) {
        if(session == null) log.debug("broadcastConnectionStatus - status: " + status.toString());
        else log.debug("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        switch (status) {
            case CLOSED:
                BrowserVS.getInstance().execCommandJS(WebSocketMessage.getWebSocketCoreSignalJSCommand(
                        null, WebSocketMessage.ConnectionStatus.CLOSED));
                break;
            case OPEN:
                BrowserVS.getInstance().execCommandJS(WebSocketMessage.getWebSocketCoreSignalJSCommand(
                        null, WebSocketMessage.ConnectionStatus.OPEN));
                break;
        }
    }

}