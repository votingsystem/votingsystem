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
import org.votingsystem.client.dialog.CertNotFoundDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.client.util.WebSocketSession;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.HttpHelper;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketAuthenticatedService extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(WebSocketAuthenticatedService.class);

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketAuthenticatedService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private UserVS userVS;
    private String connectionMessage = null;

    private WebSocketAuthenticatedService(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
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

    public static WebSocketAuthenticatedService getInstance() {
        try {
            if(instance == null) instance =  new WebSocketAuthenticatedService(ContextVS.getInstance().
                    getVotingSystemSSLCerts(), ContextVS.getInstance().getCooinServer());
        } catch (Exception ex) { log.error(ex.getMessage(), ex);}
        return instance;
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
            WebSocketAuthenticatedService.this.session = session;
        }

        @OnClose public void onClose(Session session, CloseReason closeReason) {
            broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
            SessionService.getInstance().setIsConnected(false);
            EventBusService.getInstance().post(new ResponseVS(TypeVS.DISCONNECT));
        }

        @OnMessage public void onMessage(String message) {
            consumeMessage(message);
        }
    }

    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
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

    private void connect(Map connectionDataMap, String password) {
        ProgressDialog.showDialog(new InitValidatedSessionTask((String) connectionDataMap.get("nif"),
                password, targetServer), ContextVS.getMessage("connectLbl"), BrowserVS.getInstance().getScene().getWindow());
    }

    public void setConnectionEnabled(boolean isConnectionEnabled, Map connectionDataMap){
        if(SessionService.getInstance().getUserVS() == null) {
            CertNotFoundDialog.show(BrowserVS.getInstance().getScene().getWindow());
            return;
        }
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
                    } else connect(connectionDataMap, password);
                } else if(CryptoTokenVS.MOBILE == SessionService.getCryptoTokenType()) {
                    connect(connectionDataMap, null);
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

    public boolean isConnectedWithAlert() {
        if(isConnected()) return true;
        else {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionRequiredForServiceErrorMsg"));
            return false;
        }
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
            socketMsg.setWebSocketSession(socketSession);
            ResponseVS responseVS = null;
            switch(socketMsg.getOperation()) {
                case MESSAGEVS:
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().newMessage(new InboxMessage(socketMsg));
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
                        responseVS = new ResponseVS(null, socketSession.getTypeVS(), socketMsg);
                    }
                    break;
                case MESSAGEVS_FROM_DEVICE:
                    responseVS = new ResponseVS(null, socketSession.getTypeVS(), socketMsg);
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    SessionService.setSignResponse(socketMsg);
                    break;
                default:
                    log.debug("unprocessed socketMsg: " + socketMsg.getOperation());
            }
            if(responseVS != null) EventBusService.getInstance().post(responseVS);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void broadcastConnectionStatus(WebSocketMessage.ConnectionStatus status) {
        if(session == null) log.debug("broadcastConnectionStatus - status: " + status.toString());
        else log.debug("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        switch (status) {
            case CLOSED:
                BrowserVS.getInstance().runJSCommand(WebSocketMessage.getWebSocketCoreSignalJSCommand(
                        null, WebSocketMessage.ConnectionStatus.CLOSED));
                break;
            case OPEN:
                BrowserVS.getInstance().runJSCommand(WebSocketMessage.getWebSocketCoreSignalJSCommand(
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
                if(SessionService.getCryptoTokenType() == CryptoTokenVS.MOBILE) {
                    updateMessage(ContextVS.getMessage("checkDeviceVSCryptoTokenMsg"));
                } else updateMessage(ContextVS.getMessage("connectionMsg"));
                SMIMEMessage smimeMessage = SessionService.getSMIME(null, targetServer.getName(),
                        documentToSignJSON.toString(), password, ContextVS.getMessage("initAuthenticatedSessionMsgSubject"));
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
                userVS = smimeMessage.getSigner();
                responseVS = timeStamper.call();
                smimeMessage = timeStamper.getSMIME();
                connectionMessage = WebSocketMessage.getAuthenticationRequest(smimeMessage, randomUUID).toString();
                PlatformImpl.runLater(() -> WebSocketAuthenticatedService.this.restart());
            } catch(InterruptedException ex) {
                log.error(ex.getMessage(), ex);
                broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            return responseVS;
        }
    }

}