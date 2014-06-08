package org.votingsystem.client.util;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.*;
import org.bouncycastle.util.encoders.Base64;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.ContentSignerHelper;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.websocket.*;

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
    private Set<WebSocketListener> listeners = new HashSet<WebSocketListener>();

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
                }
            }
        });
    }


    public static WebSocketService getInstance() {
        return instance;
    }

    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }
    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                logger.debug("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
                initAuthenticatedSession();
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override public void onMessage(String message) {
                                consumeMessage(message);
                            }
                        });
                        WebSocketService.this.session = session;
                        broadcastConnectionStatus(WebSocketListener.ConnectionStatus.OPEN);
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
        if(isConnectionEnabled && (session == null || !session.isOpen())) restart();
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

    public void sendMessage(String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    private void consumeMessage(String message) {
        JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(message);
        TypeVS operation = TypeVS.valueOf(messageJSON.getString("operation"));
        logger.debug("consumeMessage - num. listeners: " + listeners.size() + " - operation: " + operation +
                " - status: " + messageJSON.getString("status"));
        switch(operation) {
            case INIT_VALIDATED_SESSION:
                if(messageJSON.containsKey("messageVSList") &&
                        messageJSON.getJSONArray("messageVSList").size() > 0) {

                }
                break;
        }
        for(WebSocketListener listener : listeners) {
            if(listener != null) listener.consumeWebSocketMessage(messageJSON);
            else listeners.remove(listener);
        }
    }

    private void broadcastConnectionStatus(WebSocketListener.ConnectionStatus status) {
        logger.debug("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
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

    public class InitValidatedSessionTask extends Task<ResponseVS> {

        private String password;
        private ActorVS targetServer;

        public InitValidatedSessionTask (String password, ActorVS targetServer) {
            this.password = password;
            this.targetServer = targetServer;
        }

        @Override protected ResponseVS call() throws Exception {
            Map documentToSignMap = new HashMap<>();
            documentToSignMap.put("operation", TypeVS.INIT_VALIDATED_SESSION);
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
            SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null, targetServer.getNameNormalized(),
                    documentToSignJSON.toString(), password.toCharArray(), ContextVS.getMessage("initAuthenticatedSessionMsgSubject"), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
            ResponseVS responseVS = timeStamper.call();
            smimeMessage = timeStamper.getSmimeMessage();
            sendMessage(getMessageJSON(TypeVS.INIT_VALIDATED_SESSION, null, null, smimeMessage).toString());
            return responseVS;
        }
    }

}