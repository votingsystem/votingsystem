package org.votingsystem.client.webextension.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.service.EventBusService;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.WebSocketSession;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(WebSocketService.class.getName());

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
            log.info("settings for SECURE connetion");
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
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else log.info("settings for INSECURE connection");
        instance = this;
    }

    public static WebSocketService getInstance() {
        try {
            if(instance == null) instance =  new WebSocketService(ContextVS.getInstance().getVotingSystemSSLCerts(),
                    ContextVS.getInstance().getCurrencyServer());
        } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
        return instance;
    }


    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }

    public static class EndpointConfigurator extends ClientEndpointConfig.Configurator {

        @Override public void beforeRequest(Map<String, List<String>> headers) {
            //headers.put("Cookie", Arrays.asList("sessionVS=7180db71-3331-4e57-a448-5e7755e5dd3c"));
            headers.put("Origin", Arrays.asList(ContextVS.getInstance().getCurrencyServer().getServerURL()));
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
            broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
        }

        @OnMessage public void onMessage(String message) {
            try {
                consumeMessage( JSON.getMapper().readValue(message, SocketMessageDto.class));
            } catch (IOException e) { e.printStackTrace(); }
        }

    }

    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                log.info("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
                client.connectToServer(new WSEndpoint(), URI.create(targetServer.getWebSocketURL()));
            }catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return null;
        }
    }

    public void close() {
        log.info("close");
        if(session != null && session.isOpen()) {
            try {session.close();}
            catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
        } else broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
    }

    public void sendMessage(String message) throws IOException {
        if(session != null && session.isOpen()) session.getBasicRemote().sendText(message);
        else {
            connectionMessage = message;
            this.restart();
        }
    }

    private void consumeMessage(final SocketMessageDto messageDto){
        try {
            log.info("consumeMessage - type: " + messageDto.getOperation() + " - status: " + messageDto.getStatusCode());
            if(ResponseVS.SC_ERROR == messageDto.getStatusCode()) {
                showMessage(messageDto.getStatusCode(), messageDto.getMessage());
                return;
            }
            WebSocketSession socketSession = ContextVS.getInstance().getWSSession(messageDto.getUUID());
            if(messageDto.isEncrypted() && socketSession != null) {
                messageDto.decryptMessage(socketSession.getAESParams());
                log.info("consumeMessage - type: " + messageDto.getOperation() + " - status: " + messageDto.getStatusCode());
            }
            switch(messageDto.getOperation()) {
                case MESSAGEVS_FROM_VS:
                    switch (socketSession.getTypeVS()) {
                        case MESSAGEVS_SIGN:
                            BrowserSessionService.setSignResponse(messageDto);
                            break;
                    }
                    break;
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().newMessage(new InboxMessage(messageDto));
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    BrowserSessionService.setSignResponse(messageDto);
                    break;
                default: log.info("unprocessed messageDto - operation: " +  messageDto.getOperation());
            }
        } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
    }

    private void broadcastConnectionStatus(SocketMessageDto.ConnectionStatus status) {
        if(session == null) log.info("broadcastConnectionStatus - status: " + status.toString());
        else log.info("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        switch (status) {
            case CLOSED:
                BrowserHost.sendMessageToBrowser(MessageDto.WEB_SOCKET(ResponseVS.SC_CANCELED, null));
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.DISCONNECT));
                break;
            case OPEN:
                BrowserHost.sendMessageToBrowser(MessageDto.WEB_SOCKET(ResponseVS.SC_OK, null));
                EventBusService.getInstance().post(new SocketMessageDto().setOperation(TypeVS.CONNECT));
                break;
        }
    }

}