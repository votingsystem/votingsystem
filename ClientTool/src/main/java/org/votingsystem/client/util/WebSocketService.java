package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.ContentSignerHelper;
import org.votingsystem.signature.util.KeyStoreUtil;

import javax.websocket.*;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class WebSocketService extends Service<ResponseVS> {

    private static Logger logger = Logger.getLogger(WebSocketService.class);

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private UserVS userVS;
    private BrowserVS browserVS;
    private Set<WebSocketListener> listeners = new HashSet<WebSocketListener>();
    private String connectionMessage = null;

    public WebSocketService(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
        this.targetServer = targetServer;
        if(targetServer.getWebSocketURL().startsWith("wss")) {
            logger.debug("settings for SECURE connetion");
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
                logger.error(ex.getMessage(), ex);
            }
        } else logger.debug("settings for INSECURE connection");
        instance = this;
    }

    public void initAuthenticatedSession() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                PasswordDialog passwordDialog = new PasswordDialog();
                passwordDialog.show(ContextVS.getMessage("initAuthenticatedSessionPasswordMsg"));
                String password = passwordDialog.getPassword();
                if(password != null) {
                    new Thread(new InitValidatedSessionTask(password,targetServer)).start();
                } else broadcastConnectionStatus(WebSocketListener.ConnectionStatus.CLOSED);
            }
        });
    }

    MessageDialog messageDialog;
    public void showMessage(final int statusCode, final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                if (messageDialog == null) messageDialog = new MessageDialog();
                messageDialog.showMessage(statusCode, message);
            }
        });
    }

    public static WebSocketService getInstance() {
        return instance;
    }

    public boolean isOpen() {
        if(session != null) return session.isOpen();
        return false;
    }

    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }
    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                logger.debug("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
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
                            logger.error(ex.getMessage(), ex);
                        }
                        WebSocketService.this.session = session;
                    }

                    @Override public void onClose(Session session, CloseReason closeReason) {
                        broadcastConnectionStatus(WebSocketListener.ConnectionStatus.CLOSED);
                    }

                    @Override public void onError(Session session, Throwable thr) {
                        logger.error("WebSocketTask.onError(...) - " + thr.getMessage(), thr);
                    }
                }, ClientEndpointConfig.Builder.create().build(), URI.create(targetServer.getWebSocketURL()));
            }catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            return null;
        }
    }

    public void setConnectionEnabled(boolean isConnectionEnabled){
        if(isConnectionEnabled) initAuthenticatedSession();
        if(!isConnectionEnabled && session != null && session.isOpen()) {
            try {session.close();}
            catch(Exception ex) {logger.error(ex.getMessage(), ex);}
        }
    }

    public void addListener(WebSocketListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WebSocketListener listener) {
        listeners.remove(listener);
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void consumeMessage(String message) {
        ResponseVS responseVS = ResponseVS.parseWebSocketResponse(message);
        logger.debug("consumeMessage - num. listeners: " + listeners.size() + " - type: " + responseVS.getType() +
                " - status: " + responseVS.getStatusCode());
        switch(responseVS.getType()) {
            case INIT_VALIDATED_SESSION:
                if(responseVS.getMessageJSON().containsKey("messageVSList") &&
                        responseVS.getMessageJSON().getJSONArray("messageVSList").size() > 0) {
                    String callbackMsg = responseVS.getMessageJSON().toString();
                    String callback = "updateMessageVSList";
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            browserVS = new BrowserVS();
                            browserVS.loadURL(targetServer.getMessageVSInboxURL(), callback,
                                    callbackMsg, ContextVS.getMessage("messageVSInboxCaption"), false);
                        }
                    });
                }
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    broadcastConnectionStatus(WebSocketListener.ConnectionStatus.OPEN);
                } else broadcastConnectionStatus(WebSocketListener.ConnectionStatus.CLOSED);
                break;
            case MESSAGEVS_EDIT:
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) showMessage(
                        responseVS.getStatusCode(), responseVS.getMessage());
            case MESSAGEVS_GET:
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) showMessage(
                        responseVS.getStatusCode(), responseVS.getMessage());
                else {
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            browserVS.executeScript("updateMessageVSList(" + message + ")");
                        }
                    });
                }
                break;
        }
        for(WebSocketListener listener : listeners) {
            if(listener != null) listener.consumeWebSocketMessage(responseVS.getMessageJSON());
            else listeners.remove(listener);
        }
    }

    private void broadcastConnectionStatus(WebSocketListener.ConnectionStatus status) {
        if(session == null) logger.debug("broadcastConnectionStatus - status: " + status.toString());
        else logger.debug("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        for(WebSocketListener listener : listeners) {
            if(listener != null) listener.setConnectionStatus(status);
            else listeners.remove(listener);
        }
    }

    public static JSONObject getMessageJSON(TypeVS operation, String message, Map data, SMIMEMessageWrapper smimeMessage) {
        Map messageToServiceMap = new HashMap<>();
        messageToServiceMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        messageToServiceMap.put("operation", operation.toString());
        if(message != null) messageToServiceMap.put("message", message);
        if(data != null) messageToServiceMap.put("data", data);
        if(smimeMessage != null) {
            try {
                String smimeMessageStr = new String(Base64.encode(smimeMessage.getBytes()));
                messageToServiceMap.put("smimeMessage", smimeMessageStr);
            } catch (Exception ex) {
                logger.debug(ex.getMessage(), ex);
            }
        }
        JSONObject messageToServiceJSON = (JSONObject) JSONSerializer.toJSON(messageToServiceMap);
        return messageToServiceJSON;
    }

    public UserVS getSessionUser() {
        return userVS;
    }

    public class InitValidatedSessionTask extends Task<ResponseVS> {

        private String password;
        private ActorVS targetServer;

        public InitValidatedSessionTask (String password, ActorVS targetServer) {
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
                SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null, targetServer.getNameNormalized(),
                        documentToSignJSON.toString(), password.toCharArray(), ContextVS.getMessage("initAuthenticatedSessionMsgSubject"), null);
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
                userVS = smimeMessage.getSigner();
                responseVS = timeStamper.call();
                smimeMessage = timeStamper.getSmimeMessage();
                connectionMessage = getMessageJSON(TypeVS.INIT_VALIDATED_SESSION, null, null, smimeMessage).toString();
                WebSocketService.this.restart();
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                broadcastConnectionStatus(WebSocketListener.ConnectionStatus.CLOSED);
                showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            return responseVS;
        }
    }

}