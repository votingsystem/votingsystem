package org.votingsystem.client.service;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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
import org.votingsystem.client.util.SessionVSUtils;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.client.util.WebSocketMessage;

import javax.websocket.*;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

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
        if(instance == null) instance =  new WebSocketServiceAuthenticated(ContextVS.getInstance().getVotingSystemSSLCerts(),
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
                        WebSocketServiceAuthenticated.this.session = session;
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

    public void setConnectionEnabled(boolean isConnectionEnabled, Map connectionDataMap){
        if(isConnectionEnabled) {
            Platform.runLater(new Runnable() {
                @Override public void run() {
                    String password = null;
                    if(CryptoTokenVS.MOBILE != SessionVSUtils.getCryptoTokenType()) {
                        PasswordDialog passwordDialog = new PasswordDialog();
                        passwordDialog.showWithoutPasswordConfirm(
                                ContextVS.getMessage("initAuthenticatedSessionPasswordMsg"));
                        password = passwordDialog.getPassword();
                        if(password == null) {
                            broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                            return;
                        } else new Thread(new InitValidatedSessionTask((String) connectionDataMap.get("nif"),
                                password, targetServer)).start();
                    } else showMessage(null, ContextVS.getMessage("mobileCryptoTokenNotAllowedErrorMsg"));
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

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void consumeMessage(final String messageStr) {
        try {
            WebSocketMessage message = new WebSocketMessage((JSONObject) JSONSerializer.toJSON(messageStr));
            log.debug("consumeMessage - type: " + message.getOperation() + " - status: " + message.getStatusCode());
            switch(message.getOperation()) {
                case INIT_VALIDATED_SESSION:
                    if(ResponseVS.SC_OK == message.getStatusCode()) {
                        SessionVSUtils.getInstance().initAuthenticatedSession(message, userVS);
                    } else log.error("ERROR - INIT_VALIDATED_SESSION - statusCode: " + message.getStatusCode());
                    break;
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
                    if(message.isEncrypted()) {
                        message.decryptMessage(VotingSystemApp.getInstance().getSessionKeys(message.getUUID()));
                        SessionVSUtils.setWebSocketMessage(message);
                    }

                    break;
                default:
                    log.debug("unprocessed message");
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

    public static JSONObject getMessageJSON(TypeVS operation, String message, Map data, SMIMEMessage smimeMessage) {
        Map messageToServiceMap = new HashMap<>();
        messageToServiceMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        messageToServiceMap.put("operation", operation.toString());
        if(message != null) messageToServiceMap.put("message", message);
        if(data != null) messageToServiceMap.put("data", data);
        if(smimeMessage != null) {
            try {
                String smimeMessageStr = Base64.getEncoder().encodeToString(smimeMessage.getBytes());
                messageToServiceMap.put("smimeMessage", smimeMessageStr);
            } catch (Exception ex) {
                log.debug(ex.getMessage(), ex);
            }
        }
        JSONObject messageToServiceJSON = (JSONObject) JSONSerializer.toJSON(messageToServiceMap);
        return messageToServiceJSON;
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
            documentToSignMap.put("operation", TypeVS.INIT_VALIDATED_SESSION.toString());
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            ResponseVS responseVS = null;
            try {
                JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
                SMIMEMessage smimeMessage = SessionVSUtils.getSMIME(null,
                        targetServer.getNameNormalized(), documentToSignJSON.toString(),
                        password.toCharArray(), ContextVS.getMessage("initAuthenticatedSessionMsgSubject"));
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
                userVS = smimeMessage.getSigner();
                responseVS = timeStamper.call();
                smimeMessage = timeStamper.getSMIME();
                connectionMessage = getMessageJSON(TypeVS.INIT_VALIDATED_SESSION, null, null, smimeMessage).toString();
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() { WebSocketServiceAuthenticated.this.restart();}
                });
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                broadcastConnectionStatus(WebSocketMessage.ConnectionStatus.CLOSED);
                showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            return responseVS;
        }
    }

}