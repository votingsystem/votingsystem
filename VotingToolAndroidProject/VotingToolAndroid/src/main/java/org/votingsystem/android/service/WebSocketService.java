package org.votingsystem.android.service;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.bouncycastle2.util.encoders.Base64;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.KeyStoreUtils;
import org.votingsystem.util.FileUtils;

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
        Log.i(TAG + ".onCreate(...) ", "WebSocketService created");
    }

    @Override public void onDestroy(){
        Log.i(TAG + ".onDestroy() ", "onDestroy");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG + ".onStartCommand(...) ", "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(intent != null) {
            Bundle arguments = intent.getExtras();
            TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
            String messageToSend = arguments.getString(ContextVS.MESSAGE_KEY);
            ResponseVS responseVS = null;
            if(contextVS.getVicketServer() == null) {
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, getString(R.string.connection_error_msg));
                contextVS.sendBroadcast(responseVS);
            }
            if(session == null || !session.isOpen()) {
                WebSocketListener socketListener = new WebSocketListener(
                        contextVS.getVicketServer().getWebSocketURL());
                Thread websocketThread = new Thread(null, socketListener, "websocket_service_thread");
                websocketThread.start();
            }
            try {
                Log.d(TAG + ".onStartCommand(...) ", "starting session");
                latch.await();
                Log.d(TAG + ".onStartCommand(...) ", "session started");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            WebSocketMessageProccessor messageProccessor = new WebSocketMessageProccessor(
                    operationType, messageToSend);
            Thread messageProccessorThread = new Thread(null, messageProccessor, "websocket_message_proccessor_thread");
            messageProccessorThread.start();
        }
        //We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }


    private ResponseVS initAuthenticatedSession() {
        VicketServer vicketServer = contextVS.getVicketServer();
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.INIT_VALIDATED_SESSION.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        String msgSubject = getString(R.string.init_authenticated_session_msg_subject);
        JSONObject requestJSON = new JSONObject(mapToSend);
        ResponseVS responseVS = contextVS.signMessage(vicketServer.getNameNormalized(),
                requestJSON.toString(), msgSubject);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        SMIMEMessage smimeMessage = null;
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(responseVS.getSmimeMessage(),
                    contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setCaption(getString(R.string.timestamp_service_error_caption));
                return responseVS;
            }
            smimeMessage = timeStamper.getSmimeMessage();
        } catch(Exception ex) {
            ex.printStackTrace();
            new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        try {
            session.getBasicRemote().sendText(getMessageJSON(TypeVS.INIT_VALIDATED_SESSION, null, null,
                    smimeMessage).toString());
        } catch(Exception ex) {
            ex.printStackTrace();
            new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return new ResponseVS(ResponseVS.SC_OK);
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

    private void consumeMessage(ResponseVS responseVS) {
        Log.d(TAG + "consumeMessage", "status: " + responseVS.getStatusCode() +
                " - typeVS: " + responseVS.getTypeVS());
        responseVS.setServiceCaller(ContextVS.WEB_SOCKET_BROADCAST_ID);
        contextVS.sendBroadcast(responseVS);
    }

    private class WebSocketListener implements Runnable {

        private String serviceURL = null;

        final ClientManager client = ClientManager.createClient();

        public WebSocketListener(String serviceURL) {
            this.serviceURL = serviceURL;
            if(serviceURL.startsWith("wss")) {
                Log.d(TAG + ".WebsocketListener", "setting SECURE connection");
                try {
                    KeyStore p12Store = KeyStore.getInstance("PKCS12");
                    p12Store.load(null, null);
                    byte[] certBytes = FileUtils.getBytesFromInputStream(getAssets().open("VotingSystemSSLCert.pem"));
                    Collection<X509Certificate> votingSystemSSLCerts =  CertUtils.fromPEMToX509CertCollection(certBytes);
                    X509Certificate serverCert = votingSystemSSLCerts.iterator().next();
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                    byte[] p12KeyStoreBytes = KeyStoreUtils.getBytes(p12Store, "".toCharArray());

                    // Grizzly ssl configuration
                    SSLContextConfigurator sslContext = new SSLContextConfigurator();
                    sslContext.setTrustStoreType("PKCS12");
                    sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                    sslContext.setTrustStorePass("");
                    SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, true, false, false);
                    client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                Log.d(TAG + ".WebsocketListener", "setting INSECURE connection");
            }
        }

        @Override public void run() {
            try {
                Log.d(TAG + ".WebsocketListener", "connecting to '" + serviceURL + "'...");
                // sets the incoming buffer size to 1000000 bytes ~ 900K
                //client.getProperties().put("org.glassfish.tyrus.incomingBufferSize", 1000000);
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override public void onMessage(String message) {
                                consumeMessage(ResponseVS.parseWebSocketResponse(message));
                            }
                        });
                        WebSocketService.this.session = session;
                        latch.countDown();
                    }

                    @Override public void onClose(Session session, CloseReason closeReason) {
                        latch = new CountDownLatch(1);
                        consumeMessage(new ResponseVS(ResponseVS.SC_OK, TypeVS.WEB_SOCKET_CLOSE));
                    }
                }, ClientEndpointConfig.Builder.create().build(), URI.create(serviceURL));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class WebSocketMessageProccessor implements Runnable {

        private String messageToSend = null;
        private TypeVS operationType = null;

        public WebSocketMessageProccessor(TypeVS operationType, String messageToSend) {
            this.messageToSend = messageToSend;
            this.operationType = operationType;
        }

        @Override public void run() {
            ResponseVS responseVS;
            switch(operationType) {
                case MESSAGEVS_GET:
                case WEB_SOCKET_MESSAGE:
                    try {
                        session.getBasicRemote().sendText(messageToSend);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case WEB_SOCKET_INIT:
                    responseVS = initAuthenticatedSession();
                    contextVS.sendBroadcast(responseVS);
                    break;
                case WEB_SOCKET_CLOSE:
                    try {
                        session.close();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                default:
                    Log.i(TAG + ".onStartCommand() ", "unknown operation: " + operationType.toString());
            }
        }
    }
}