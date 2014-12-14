package org.votingsystem.client.service;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.util.SessionVSUtils;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.client.util.WebSocketMessage;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.votingsystem.client.VotingSystemApp.showMessage;

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
        if(instance == null) instance =  new WebSocketService(ContextVS.getInstance().getVotingSystemSSLCerts(),
                ContextVS.getInstance().getCooinServer());
        return instance;
    }


    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }
    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                log.debug("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override public void onMessage(String message) {
                                consumeMessage(message);
                            }
                        });
                        try {
                            session.getBasicRemote().sendText(connectionMessage);
                        } catch(Exception ex) {
                            log.error(ex.getMessage(), ex);
                        }
                        WebSocketService.this.session = session;
                    }

                    @Override public void onClose(Session session, CloseReason closeReason) {
                        broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                        SessionVSUtils.getInstance().setIsConnected(false);
                    }

                    @Override public void onError(Session session, Throwable thr) {
                        log.error("WebSocketTask.onError(...) - " + thr.getMessage(), thr);
                    }
                }, ClientEndpointConfig.Builder.create().build(), URI.create(targetServer.getWebSocketURL()));
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
            WebSocketMessage message = new WebSocketMessage((JSONObject) JSONSerializer.toJSON(messageStr));
            log.debug("consumeMessage - type: " + message.getOperation() +
                    " - status: " + message.getStatusCode());
            switch(message.getOperation()) {
                case MESSAGEVS_SIGN:
                case MESSAGEVS_EDIT:
                    if(ResponseVS.SC_OK != message.getStatusCode()) showMessage(
                            message.getStatusCode(), message.getMessage());
                case MESSAGEVS_GET:
                    if(ResponseVS.SC_OK != message.getStatusCode()) showMessage(
                            message.getStatusCode(), message.getMessage());
                    else {
                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                log.debug(" ==== TODO - SEND MESSAGE TO BROWSER ==== ");
                                BrowserVS.getInstance().execCommandJSCurrentView("alert('" + message.getMessage() + "')");
                                //browserVS.executeScript("updateMessageVSList(" + message + ")");
                            }
                        });
                    }
                    break;
            }
            if(message.getStatusCode() != null && ResponseVS.SC_ERROR == message.getStatusCode()) {
                showMessage(message.getStatusCode(), message.getMessage());
                return;
            }
            switch(message.getOperation()) {
                case INIT_VALIDATED_SESSION:
                    BrowserVS.getInstance().execCommandJS(
                            message.getWebSocketCoreSignalJSCommand(WebSocketMessage.ConnectionStatus.OPEN));
                    break;
                //case MESSAGEVS_SIGN: break;
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().addMessage(message);
                    break;
                case MESSAGEVS_FROM_DEVICE:
                    SessionVSUtils.setWebSocketMessage(message);
                    break;
                default:
                    log.debug("unprocessed message");
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