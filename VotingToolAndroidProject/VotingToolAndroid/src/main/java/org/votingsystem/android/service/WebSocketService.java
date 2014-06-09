package org.votingsystem.android.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertRequestActivity;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.activity.UserCertResponseActivity;
import org.votingsystem.android.fragment.VicketUserInfoFragment;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.UUID;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import static org.votingsystem.model.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.model.ContextVS.RESPONSEVS_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.URI_KEY;
import static org.votingsystem.model.ContextVS.URL_KEY;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class WebSocketService extends Service {

    public static final String TAG = WebSocketService.class.getSimpleName();

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private AppContextVS contextVS;
    private Handler handler;
    private Session session;

    @Override public void onCreate(){
        contextVS = (AppContextVS) getApplicationContext();
        handler = new Handler();
        Log.i(TAG + ".onCreate(...) ", "VotingAppService created");
    }

    @Override public void onDestroy(){
        Log.i(TAG + ".onDestroy() ", "onDestroy");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG + ".onStartCommand(...) ", "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(intent != null) {
            Bundle arguments = intent.getExtras();
            TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
            String messageToSend = arguments.getString(ContextVS.MESSAGE_KEY);
            if(session == null || !session.isOpen()) {
                WebSocketListener socketListener = new WebSocketListener(
                        contextVS.getVicketServer().getWebSocketURL());
                Thread websocketThread = new Thread(null, socketListener, "websocket_service_thread");
                websocketThread.start();
                //We want this service to continue running until it is explicitly stopped, so return sticky.
            } else {
                switch(operationType) {
                    case WEB_SOCKET_MESSAGE:
                        try {
                            session.getBasicRemote().sendText(messageToSend);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                        break;
                    default:
                        Log.i(TAG + ".onStartCommand() ", "unknown operation: " + operationType.toString());
                }

            }
        }
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

    private void consumeMessage(String message) {
        Log.d(TAG + "consumeMessage", "message: " + message);
        ResponseVS responseVS = ResponseVS.parseWebSocketResponse(message);
        contextVS.sendBroadcast(responseVS);
    }

    private class WebSocketListener implements Runnable {

        private String serviceURL = null;

        final ClientManager client = ClientManager.createClient();

        public WebSocketListener(String serviceURL) {
            this.serviceURL = serviceURL;
            try {
                KeyStore p12Store = KeyStore.getInstance("PKCS12");
                p12Store.load(null, null);
                byte[] certBytes = FileUtils.getBytesFromInputStream(getAssets().open("VotingSystemSSLCert.pem"));
                Collection<X509Certificate> votingSystemSSLCerts =  CertUtil.fromPEMToX509CertCollection(certBytes);
                X509Certificate serverCert = votingSystemSSLCerts.iterator().next();
                p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                byte[] p12KeyStoreBytes = KeyStoreUtil.getBytes(p12Store, "".toCharArray());

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
        }

        @Override public void run() {
            try {
                Log.d(TAG + ".WebsocketListener", "connectiong to: " + serviceURL);
                client.connectToServer(new Endpoint() {
                    @Override public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        try {
                            session.addMessageHandler(new MessageHandler.Whole<String>() {
                                @Override public void onMessage(String message) {
                                    consumeMessage(message);
                                }
                            });
                            session.getBasicRemote().sendText("Test message from Android Websocket client");
                            WebSocketService.this.session = session;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }, ClientEndpointConfig.Builder.create().build(), URI.create(serviceURL));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}