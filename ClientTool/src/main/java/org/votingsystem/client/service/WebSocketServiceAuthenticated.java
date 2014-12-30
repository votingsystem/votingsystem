package org.votingsystem.client.service;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.client.util.WebSocketSession;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.HttpHelper;
import javax.websocket.*;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketServiceAuthenticated extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(WebSocketServiceAuthenticated.class);

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketServiceAuthenticated instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private UserVS userVS;
    private String connectionMessage = null;

    private WebSocketServiceAuthenticated(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
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
    }

    public static WebSocketServiceAuthenticated getInstance() {
        if(instance == null) instance =  new WebSocketServiceAuthenticated(ContextVS.getInstance().
                getVotingSystemSSLCerts(), ContextVS.getInstance().getCooinServer());
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
                        WebSocketServiceAuthenticated.this.session = session;
                    }

                    @Override public void onClose(Session session, CloseReason closeReason) {
                        broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                        SessionService.getInstance().setIsConnected(false);
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

    public void setConnectionEnabled(boolean isConnectionEnabled, Map connectionDataMap){
        if(isConnectionEnabled) {
            Platform.runLater(() -> {
                if(CryptoTokenVS.MOBILE != SessionService.getCryptoTokenType()) {
                    String password = null;
                    PasswordDialog passwordDialog = new PasswordDialog();
                    passwordDialog.showWithoutPasswordConfirm(
                            ContextVS.getMessage("initAuthenticatedSessionPasswordMsg"));
                    password = passwordDialog.getPassword();
                    if(password == null) {
                        broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                        return;
                    } else new Thread(new InitValidatedSessionTask((String) connectionDataMap.get("nif"),
                            password, targetServer)).start();
                } else if(CryptoTokenVS.MOBILE == SessionService.getCryptoTokenType()) {
                    showMessage(ContextVS.getMessage("checkDeviceVSCryptoTokenMsg"));
                    new Thread(new InitValidatedSessionTask((String) connectionDataMap.get("nif"),
                            null, targetServer)).start();
                }
            });
        }
        if(!isConnectionEnabled) {
            if(session != null && session.isOpen()) {
                try {session.close();}
                catch(Exception ex) {log.error(ex.getMessage(), ex);}
            } else broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
        }
    }

    public boolean isConnected() {
        if(session != null && session.isOpen()) return true;
        else return false;
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public ResponseVS sendMessageVS(OperationVS operationVS) throws Exception {
        log.debug("sendMessageVS");
        ResponseVS responseVS = null;
        if(isConnected()) {
            responseVS = HttpHelper.getInstance().getData(((CooinServer) operationVS.getTargetServer()).
                    getDeviceVSConnectedServiceURL(operationVS.getNif()), ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONArray deviceArray = ((JSONObject) responseVS.getMessageJSON()).getJSONArray("deviceList");
                boolean isMessageDelivered = false;
                for (int i = 0; i < deviceArray.size(); i++) {
                    DeviceVS deviceVS = DeviceVS.parse((JSONObject) deviceArray.get(i));
                    if(!SessionService.getInstance().getDeviceId().equals(deviceVS.getDeviceId())) {
                        JSONObject socketMsg = WebSocketMessage.getMessageVSToDevice(deviceVS, operationVS.getNif(),
                                operationVS.getMessage());
                        sendMessage(socketMsg.toString());
                        isMessageDelivered = true;
                    }
                }
                if(isMessageDelivered) responseVS = new ResponseVS(ResponseVS.SC_OK);
                else responseVS = new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage(
                        "uservsWithoutDevicesConnectedMsg", operationVS.getNif()));
            }
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"));
        return responseVS;
    }

    private void consumeMessage(final String socketMsgStr) {
        try {
            WebSocketMessage socketMsg = new WebSocketMessage((JSONObject) JSONSerializer.toJSON(socketMsgStr));
            WebSocketSession socketSession = VotingSystemApp.getInstance().getWSSession(socketMsg.getUUID());
            log.debug("consumeMessage - type: " + socketMsg.getOperation() + " - status: " + socketMsg.getStatusCode());
            if(ResponseVS.SC_ERROR == socketMsg.getStatusCode()) {
                showMessage(socketMsg.getStatusCode(), socketMsg.getMessage());
                return;
            }
            if(socketSession != null && socketMsg.isEncrypted()) {
                socketMsg.decryptMessage(socketSession.getAESParams());
            }
            switch(socketMsg.getOperation()) {
                case MESSAGEVS:
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().addMessage(socketMsg);
                    break;
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null) {
                        socketMsg.setOperation(socketSession.getTypeVS());
                        switch(socketSession.getTypeVS()) {
                            case INIT_VALIDATED_SESSION:
                                SessionService.getInstance().initAuthenticatedSession(socketMsg, userVS);
                                break;
                            default:
                                log.error("MESSAGEVS_FROM_VS - TypeVS: " + socketSession.getTypeVS());
                        }
                    }
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    SessionService.setSignResponse(socketMsg);
                    break;
                default:
                    log.debug("unprocessed socketMsg: " + socketMsg.getOperation());
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
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

    public class InitValidatedSessionTask extends Task<ResponseVS> {

        private String password, nif;
        private ActorVS targetServer;

        public InitValidatedSessionTask (String nif, String password, ActorVS targetServer) {
            this.nif = nif;
            this.password = password;
            this.targetServer = targetServer;
        }

        @Override protected ResponseVS call() throws Exception {
            Map documentToSignMap = new HashMap<>();
            String randomUUID = UUID.randomUUID().toString();
            documentToSignMap.put("operation", TypeVS.INIT_VALIDATED_SESSION.toString());
            documentToSignMap.put("deviceFromId", SessionService.getInstance().getDeviceId());
            documentToSignMap.put("UUID", randomUUID);
            ResponseVS responseVS = null;
            try {
                JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
                SMIMEMessage smimeMessage = SessionService.getSMIME(null, targetServer.getNameNormalized(),
                        documentToSignJSON.toString(), password, ContextVS.getMessage("initAuthenticatedSessionMsgSubject"));
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
                userVS = smimeMessage.getSigner();
                responseVS = timeStamper.call();
                smimeMessage = timeStamper.getSMIME();
                connectionMessage = WebSocketMessage.getAuthenticationRequest(smimeMessage, randomUUID).toString();
                PlatformImpl.runLater(() -> WebSocketServiceAuthenticated.this.restart());
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            return responseVS;
        }
    }

}