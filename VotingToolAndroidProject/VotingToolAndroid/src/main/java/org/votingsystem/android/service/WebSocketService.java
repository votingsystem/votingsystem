package org.votingsystem.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.SMIMESignerActivity;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.android.util.WebSocketMessage;
import org.votingsystem.android.util.WebSocketSession;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.KeyStoreUtils;
import org.votingsystem.util.ResponseVS;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketService extends Service {

    public static final String TAG = WebSocketService.class.getSimpleName();

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private AppContextVS contextVS;
    private Session session;
    CountDownLatch latch = new CountDownLatch(1);

    @Override public void onCreate(){
        contextVS = (AppContextVS) getApplicationContext();
        LOGD(TAG + ".onCreate", "WebSocketService started");
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
            if(responseVS != null && TypeVS.MESSAGEVS != responseVS.getTypeVS()) {
                LOGD(TAG + ".onStartCommand", "processing responseVS: " + responseVS.getTypeVS());
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
                sendWebSocketBroadcast(new WebSocketMessage(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                        getString(R.string.connection_error_msg), operationType));
            }
            try {
                if(contextVS.getCooinServer() != null) {
                    if(session == null || !session.isOpen()) {
                        WebSocketListener socketListener = new WebSocketListener(
                                contextVS.getCooinServer().getWebSocketURL());
                        new Thread(null, socketListener, "websocket_service_thread").start();
                    }
                    if(latch.getCount() > 0) {
                        LOGD(TAG + ".onStartCommand", "starting websocket session");
                        latch.await();
                        LOGD(TAG + ".onStartCommand", "websocket session started");
                    }
                } else sendWebSocketBroadcast(new WebSocketMessage(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                        getString(R.string.missing_server_connection), TypeVS.INIT_VALIDATED_SESSION).
                        setCaption(getString(R.string.connection_error_msg)));
            } catch (Exception ex) {
                LOGE(TAG + ".onStartCommand", "ERROR CONNECTING TO WEBSOCKET SERVICE: " + ex.getMessage());
            }
            new Thread(null, new MessageProccessor(operationType, operationVS, responseVS,
                    messageToSend, serviceCaller), "websocket_message_proccessor_thread").start();
        }
        //We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
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
                    X509Certificate serverCert = contextVS.getSSLServerCert();
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                    byte[] p12KeyStoreBytes = KeyStoreUtils.getBytes(p12Store, "".toCharArray());
                    SSLContextConfigurator sslContext = new SSLContextConfigurator();
                    sslContext.setTrustStoreType("PKCS12");
                    sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                    sslContext.setTrustStorePass("");
                    SSLEngineConfigurator sslEngineConfigurator =
                            new SSLEngineConfigurator(sslContext, true, false, false);
                    //BUG with Android 5.0 and Tyrus client!!! Not WSS secured connections for now
                    //https://java.net/projects/tyrus/lists/users/archive/2015-01/message/0
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
                //sets the incoming buffer size to 1000000 bytes ~ 900K
                //client.getProperties().put("org.glassfish.tyrus.incomingBufferSize", 1000000);
                //BUG with Android 5.0 and Tyrus client!!! Not WSS secured connections for now
                //https://java.net/projects/tyrus/lists/users/archive/2015-01/message/0
                final ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().
                        configurator(new ClientEndpointConfig.Configurator() {
                            @Override
                            public void beforeRequest(Map<String, List<String>> headers) {
                                //headers.put("Cookie", Arrays.asList("sessionVS=7180db71-3331-4e57-a448-5e7755e5dd3c"));
                                headers.put("Origin", Arrays.asList(contextVS.getCooinServerURL()));
                            }

                            @Override
                            public void afterResponse(HandshakeResponse handshakeResponse) {
                                //final Map<String, List<String>> headers = handshakeResponse.getHeaders();
                            }
                        }).build();
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig endpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override public void onMessage(String message) {
                                sendWebSocketBroadcast(new WebSocketMessage(message));
                            }
                        });
                        setWebSocketSession(session);
                    }
                    @Override public void onClose(Session session, CloseReason closeReason) {
                        sendWebSocketBroadcast(new WebSocketMessage(
                                ResponseVS.SC_OK, null, TypeVS.WEB_SOCKET_CLOSE));
                    }
                }, clientEndpointConfig, URI.create(serviceURL));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendWebSocketBroadcast(WebSocketMessage socketMsg) {
        LOGD(TAG + ".sendWebSocketBroadcast", "statusCode: " + socketMsg.getStatusCode() +
                " - type: " + socketMsg.getOperation() + " - serviceCaller: " + socketMsg.getServiceCaller());
        Intent intent =  new Intent(socketMsg.getServiceCaller());
        WebSocketSession socketSession = contextVS.getWSSession(socketMsg.getUUID());
        try {
            if(socketSession == null && socketMsg.isEncrypted()) {
                byte[] decryptedBytes = contextVS.decryptMessage(socketMsg.getEncryptedAESParams());
                AESParams aesParams = AESParams.load( new JSONObject(new String(decryptedBytes)));
                socketMsg.decryptMessage(aesParams);
                contextVS.putWSSession(socketMsg.getUUID(), new WebSocketSession(
                        socketMsg.getAESParams(), null, null, socketMsg.getOperation()));
            } else if(socketSession != null && socketMsg.isEncrypted()) {
                socketMsg.decryptMessage(socketSession.getAESParams());
            }
            intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY, socketMsg);
            switch(socketMsg.getOperation()) {
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null) {
                        LOGD(TAG , "MESSAGEVS_FROM_VS - TypeVS: " + socketSession.getTypeVS());
                        socketMsg.setOperation(socketSession.getTypeVS());
                        switch(socketSession.getTypeVS()) {
                            case INIT_VALIDATED_SESSION:
                                if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                                    contextVS.setDeviceId(socketMsg.getDeviceId());
                                    contextVS.setHasWebSocketConnection(true);
                                } else contextVS.setHasWebSocketConnection(false);
                                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                                break;
                            default: sendWebSocketBroadcast(socketMsg);
                        }
                    }
                    break;
                case WEB_SOCKET_CLOSE:
                    if(ResponseVS.SC_OK == socketMsg.getStatusCode())
                        contextVS.setHasWebSocketConnection(false);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case MESSAGEVS:
                    if(ResponseVS.SC_OK == socketMsg.getStatusCode()) {
                        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, socketMsg.getMessage());
                        responseVS.setCaption(getString(R.string.messagevs_caption)).
                                setNotificationMessage(socketMsg.getMessage());
                        contextVS.showNotification(responseVS);
                    } else LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case MESSAGEVS_SIGN:
                    intent = new Intent(this, SMIMESignerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ContextVS.WEBSOCKET_MSG_KEY, socketMsg);
                    startActivity(intent);
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case COOIN_WALLET_CHANGE:
                    if(ResponseVS.SC_OK == socketMsg.getStatusCode() && socketSession != null) {
                        Wallet.removeCooinList((Collection<Cooin>) socketSession.getData(), contextVS);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    }
                    break;
            }
        } catch(Exception ex) {ex.printStackTrace();}
    }

    private class MessageProccessor implements Runnable {

        private String messageToSend = null;
        private String serviceCaller = null;
        private TypeVS typeVS = null;
        private OperationVS operationVS = null;
        private ResponseVS responseData = null;

        public MessageProccessor(TypeVS typeVS, OperationVS operationVS, ResponseVS responseVS,
                                 String messageToSend, String serviceCaller) {
            this.messageToSend = messageToSend;
            this.typeVS = typeVS;
            this.operationVS = operationVS;
            this.serviceCaller = serviceCaller;
            this.responseData = responseVS;
        }

        @Override public void run() {
            ResponseVS responseVS = null;
            try {
                if(typeVS == null && operationVS != null) typeVS = operationVS.getTypeVS();
                switch(typeVS) {
                    case WEB_SOCKET_INIT:
                        CooinServer cooinServer = contextVS.getCooinServer();
                        String randomUUID = UUID.randomUUID().toString();
                        Map mapToSend = new HashMap();
                        mapToSend.put("operation", TypeVS.INIT_VALIDATED_SESSION.toString());
                        mapToSend.put("deviceFromId", PrefUtils.getApplicationId(contextVS));
                        mapToSend.put("UUID", randomUUID);
                        String msgSubject = getString(R.string.init_authenticated_session_msg_subject);
                        JSONObject requestJSON = new JSONObject(mapToSend);
                        responseVS = contextVS.signMessage(cooinServer.getName(),
                                requestJSON.toString(), msgSubject, contextVS.getCooinServer().getTimeStampServiceURL());
                        SMIMEMessage smimeMessage = responseVS.getSMIME();
                        session.getBasicRemote().sendText(WebSocketMessage.getMessageJSON(TypeVS.
                                INIT_VALIDATED_SESSION, null, null, smimeMessage, randomUUID, contextVS).toString());
                        contextVS.putWSSession(randomUUID, new WebSocketSession<>(
                                null, null, null, TypeVS.INIT_VALIDATED_SESSION));
                        break;
                    case WEB_SOCKET_CLOSE:
                        session.close();
                        break;
                    case MESSAGEVS_SIGN:
                        responseVS = contextVS.signMessage(operationVS.getToUser(),
                                operationVS.getTextToSign(), operationVS.getSignedMessageSubject(),
                                contextVS.getCooinServer().getTimeStampServiceURL());
                        JSONObject responseJSON = WebSocketMessage.getSignResponse(ResponseVS.SC_OK,
                                null, operationVS, responseVS.getSMIME(), contextVS);
                        session.getBasicRemote().sendText(responseJSON.toString());
                        break;
                    case MESSAGEVS:
                        JSONArray deviceArray = ((JSONObject) responseData.getMessageJSON()).
                                getJSONArray("deviceList");
                        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
                        String deviceId = telephonyManager.getDeviceId();
                        for (int i = 0; i < deviceArray.length(); i++) {
                            DeviceVS deviceVS = DeviceVS.parse((JSONObject) deviceArray.get(i));
                            JSONObject socketMsg = WebSocketMessage.getMessageVSToDevice(deviceVS,
                                    null, messageToSend, contextVS);
                            if(!deviceId.equals(deviceVS.getDeviceId())) {
                                session.getBasicRemote().sendText(socketMsg.toString());
                            }
                        }
                        break;
                    default:
                        LOGD(TAG + ".onStartCommand() ", "unknown operation: " + typeVS.toString());
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                Intent intent =  new Intent(serviceCaller);
                intent.putExtra(ContextVS.RESPONSEVS_KEY,
                        ResponseVS.getExceptionResponse(ex, WebSocketService.this));
                LocalBroadcastManager.getInstance(WebSocketService.this).sendBroadcast(intent);
            }
        }
    }
}