package org.votingsystem.android.service;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import org.bouncycastle2.util.encoders.Base64;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.WebSocketMessage;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.KeyStoreUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ResponseVS;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService extends Service {

    public static final String TAG = WebSocketService.class.getSimpleName();

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private AppContextVS contextVS;
    private Handler handler;
    private Session session;
    CountDownLatch latch = new CountDownLatch(1);

    @Override public void onCreate(){
        contextVS = (AppContextVS) getApplicationContext();
        handler = new Handler();
        LOGD(TAG + ".onCreate", "WebSocketService created");
    }

    @Override public void onDestroy(){
        LOGD(TAG + ".onDestroy() ", "onDestroy");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        LOGD(TAG + ".onStartCommand", "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(intent != null) {
            Bundle arguments = intent.getExtras();
            final ResponseVS responseVS = (ResponseVS)arguments.getParcelable(ContextVS.RESPONSEVS_KEY);
            if(responseVS != null) {
                new Thread(null, new Runnable() {
                    @Override public void run() {
                        try {
                            String response = responseVS.getMessageJSON().toString();
                            LOGD(TAG + ".onStartCommand", "message: " + response);
                            session.getBasicRemote().sendText(response); }
                        catch(Exception ex) {ex.printStackTrace();}
                    }
                }, "websocket_message_proccessor_thread").start();
                return START_STICKY;
            }
            TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
            OperationVS operationVS = (OperationVS)arguments.getParcelable(ContextVS.OPERATIONVS_KEY);
            String messageToSend = arguments.getString(ContextVS.MESSAGE_KEY);
            String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
            if(contextVS.getCooinServer() == null) {
                contextVS.sendWebSocketBroadcast(new WebSocketMessage(
                        ResponseVS.SC_ERROR, getString(R.string.connection_error_msg), operationType));
            }
            if(session == null || !session.isOpen()) {
                WebSocketListener socketListener = new WebSocketListener(
                        contextVS.getCooinServer().getWebSocketURL());
                new Thread(null, socketListener, "websocket_service_thread").start();
            }
            try {
                if(latch.getCount() > 0) {
                    LOGD(TAG + ".onStartCommand", "starting websocket session");
                    latch.await();
                    LOGD(TAG + ".onStartCommand", "websocket session started");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Thread(null, new MessageProccessor(
                    operationType, operationVS, messageToSend, serviceCaller),
                    "websocket_message_proccessor_thread").start();
        }
        //We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    private WebSocketMessage initAuthenticatedSession() {
        CooinServer cooinServer = contextVS.getCooinServer();
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.INIT_VALIDATED_SESSION.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        String msgSubject = getString(R.string.init_authenticated_session_msg_subject);
        try {
            JSONObject requestJSON = new JSONObject(mapToSend);
            ResponseVS responseVS = contextVS.signMessage(cooinServer.getNameNormalized(),
                    requestJSON.toString(), msgSubject, contextVS.getCooinServer().getTimeStampServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return WebSocketMessage.load(responseVS);
            SMIMEMessage smimeMessage = responseVS.getSMIME();
            session.getBasicRemote().sendText(getMessageJSON(TypeVS.INIT_VALIDATED_SESSION, null, null,
                    smimeMessage).toString());
        } catch(Exception ex) {
            ex.printStackTrace();
            return new WebSocketMessage(ResponseVS.SC_ERROR, ex.getMessage(), TypeVS.INIT_VALIDATED_SESSION);
        }
        return new WebSocketMessage(ResponseVS.SC_PROCESSING, null, TypeVS.INIT_VALIDATED_SESSION);
    }

    public JSONObject getMessageJSON(TypeVS operation, String message, Map data,
        SMIMEMessage smimeMessage) {
        Map messageToServiceMap = new HashMap();
        messageToServiceMap.put("locale", contextVS.getResources().getConfiguration().locale.getLanguage());
        messageToServiceMap.put("operation", operation.toString());
        if(message != null) messageToServiceMap.put("message", message);
        if(data != null) messageToServiceMap.put("data", data);
        if(smimeMessage != null) {
            try {
                String smimeMessageStr = new String(Base64.encode(smimeMessage.getBytes()));
                messageToServiceMap.put("smimeMessage", smimeMessageStr);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        JSONObject messageJSON = new JSONObject(messageToServiceMap);
        return messageJSON;
    }


    @Override public IBinder onBind(Intent intent){
        return mBinder;
    }

    private final IBinder mBinder = new Binder()  {
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

    private void setWebSocketSession(Session session) {
        this.session = session;
        latch.countDown();
    }

    private class WebSocketListener implements Runnable {

        private String serviceURL = null;
        final ClientManager client = ClientManager.createClient();

        public WebSocketListener(String serviceURL) {
            this.serviceURL = serviceURL;
            if(serviceURL.startsWith("wss")) {
                LOGD(TAG + ".WebsocketListener", "setting SECURE connection");
                try {
                    KeyStore p12Store = KeyStore.getInstance("PKCS12");
                    p12Store.load(null, null);
                    byte[] certBytes = FileUtils.getBytesFromInputStream(
                            getAssets().open("VotingSystemSSLCert.pem"));
                    Collection<X509Certificate> votingSystemSSLCerts =
                            CertUtils.fromPEMToX509CertCollection(certBytes);
                    X509Certificate serverCert = votingSystemSSLCerts.iterator().next();
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                    byte[] p12KeyStoreBytes = KeyStoreUtils.getBytes(p12Store, "".toCharArray());
                    // Grizzly ssl configuration
                    SSLContextConfigurator sslContext = new SSLContextConfigurator();
                    sslContext.setTrustStoreType("PKCS12");
                    sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                    sslContext.setTrustStorePass("");
                    SSLEngineConfigurator sslEngineConfigurator =
                            new SSLEngineConfigurator(sslContext, true, false, false);
                    client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else LOGD(TAG + ".WebsocketListener", "setting INSECURE connection");
        }

        @Override public void run() {
            try {
                if(latch.getCount() == 0) latch = new CountDownLatch(1);
                LOGD(TAG + ".WebsocketListener", "connecting to '" + serviceURL + "'...");
                // sets the incoming buffer size to 1000000 bytes ~ 900K
                //client.getProperties().put("org.glassfish.tyrus.incomingBufferSize", 1000000);
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override public void onMessage(String message) {
                                contextVS.sendWebSocketBroadcast(new WebSocketMessage(message));
                            }
                        });
                        setWebSocketSession(session);
                    }
                    @Override public void onClose(Session session, CloseReason closeReason) {
                        contextVS.sendWebSocketBroadcast(new WebSocketMessage(
                                ResponseVS.SC_OK, null, TypeVS.WEB_SOCKET_CLOSE));
                    }
                }, ClientEndpointConfig.Builder.create().build(), URI.create(serviceURL));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class MessageProccessor implements Runnable {

        private String messageToSend = null;
        private String serviceCaller = null;
        private TypeVS operationType = null;
        private OperationVS operationVS = null;

        public MessageProccessor(TypeVS operationType, OperationVS operationVS,
                 String messageToSend, String serviceCaller) {
            this.messageToSend = messageToSend;
            this.operationType = operationType;
            this.operationVS = operationVS;
            this.serviceCaller = serviceCaller;
        }

        @Override public void run() {
            try {
                if(operationType == null && operationVS != null)
                    operationType = operationVS.getTypeVS();
                switch(operationType) {
                    case MESSAGEVS_GET:
                    case WEB_SOCKET_MESSAGE:
                        session.getBasicRemote().sendText(messageToSend);
                        break;
                    case WEB_SOCKET_INIT:
                        contextVS.sendWebSocketBroadcast(initAuthenticatedSession());
                        break;
                    case WEB_SOCKET_CLOSE:
                        session.close();
                        break;
                    case MESSAGEVS_SIGN:
                        ResponseVS responseVS = contextVS.signMessage(operationVS.getToUser(),
                                operationVS.getTextToSign(), operationVS.getSignedMessageSubject(),
                                contextVS.getCooinServer().getTimeStampServiceURL());
                        JSONObject responseJSON = WebSocketMessage.getSignResponse(ResponseVS.SC_OK,
                                null, operationVS, responseVS.getSMIME(), contextVS);
                        session.getBasicRemote().sendText(responseJSON.toString());
                        break;
                    default:
                        LOGD(TAG + ".onStartCommand() ", "unknown operation: " + operationType.toString());
                }
            } catch(Exception ex) {ex.printStackTrace();}
        }
    }
}