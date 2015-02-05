package org.votingsystem.android;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONObject;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.android.service.BootStrapService;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.WebSocketSession;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyGeneratorVS;
import org.votingsystem.util.ArgVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;
import static org.votingsystem.model.ContextVS.ALGORITHM_RNG;
import static org.votingsystem.model.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.STATE_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.USER_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AppContextVS extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = AppContextVS.class.getSimpleName();

    private State state = State.WITHOUT_CSR;
    private boolean hasWebSocketConnection;
    private static final Map<String, ActorVS> serverMap = new HashMap<String, ActorVS>();
    private static final Map<String, WebSocketSession> sessionsMap = new HashMap<String, WebSocketSession>();
    private AccessControlVS accessControl;
    private String accessControlURL;
    private ControlCenterVS controlCenter;
    private CooinServer cooinServer;
    private String cooinServerURL;
    private UserVS userVS;
    private Long deviceId;
    private X509Certificate sslServerCert;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private AtomicInteger notificationId = new AtomicInteger(1);

    @Override public void onCreate() {
        try {
            KeyGeneratorVS.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            PrefUtils.init(this);
            Properties props = new Properties();
            props.load(getAssets().open("VotingSystem.properties"));
            cooinServerURL = props.getProperty(ContextVS.COOIN_SERVER_URL);
            accessControlURL = props.getProperty(ContextVS.ACCESS_CONTROL_URL_KEY);
            LOGD(TAG + ".onCreate", "accessControlURL: " + accessControlURL +
                    " - cooinServerURL: " + cooinServerURL);
            /*if (!PrefUtils.isEulaAccepted(this)) {//Check if the EULA has been accepted; if not, show it.
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();}*/
            if(accessControl == null || cooinServer == null) {
                Intent startIntent = new Intent(this, BootStrapService.class);
                startIntent.putExtra(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL);
                startIntent.putExtra(ContextVS.COOIN_SERVER_URL, cooinServerURL);
                startService(startIntent);
            }
            PrefUtils.registerPreferenceChangeListener(this, this);
            state = PrefUtils.getAppCertState(this, accessControlURL);
            userVS = PrefUtils.getSessionUserVS(this);
            byte[] certBytes = FileUtils.getBytesFromInputStream(getAssets().open(
                    "VotingSystemSSLCert.pem"));
            Collection<X509Certificate> votingSystemSSLCerts =
                    CertUtils.fromPEMToX509CertCollection(certBytes);
            sslServerCert = votingSystemSSLCerts.iterator().next();
            HttpHelper.init(sslServerCert);
        } catch(Exception ex) { ex.printStackTrace(); }
	}

    public X509Certificate getSSLServerCert() {
        return sslServerCert;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public  String getAccessControlURL() {
        return  accessControlURL;
    }

    public  String getCooinServerURL() {
        return  cooinServerURL;
    }

    public void finish() {
        stopService(new Intent(getApplicationContext(), WebSocketService.class));
        HttpHelper.shutdown();
        UIUtils.killApp(true);
    }

    public void setServer(ActorVS actorVS) {
        if(serverMap.get(actorVS.getServerURL()) == null) {
            serverMap.put(actorVS.getServerURL(), actorVS);
        } else LOGD(TAG + ".setServer", "server with URL '" + actorVS.getServerURL() +
                "' already in context");
    }

    public ActorVS getServer(String serverURL) {
        return serverMap.get(serverURL);
    }

    @Override public void onTerminate() {
        super.onTerminate();
        PrefUtils.unregisterPreferenceChangeListener(this, this);
    }

    public State getState() {
        return state;
    }

    public X509Certificate getCert(String serverURL) {
        LOGD(TAG + ".getCert", "serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void putCert(String serverURL, X509Certificate cert) {
        LOGD(TAG + ".putCert", "serverURL: " + serverURL);
        certsMap.put(serverURL, cert);
    }

    public X509Certificate getTimeStampCert() {
        if(accessControl != null) return accessControl.getTimeStampCert();
        if(cooinServer != null) return cooinServer.getTimeStampCert();
        return null;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public AccessControlVS getAccessControl() {
        if(accessControl == null) {
            accessControl = (AccessControlVS) getActorVS(accessControlURL);
        }
        return accessControl;
    }

    public String getTimeStampServiceURL() {
        if(accessControl != null) return accessControl.getTimeStampServiceURL();
        if(cooinServer != null) return cooinServer.getTimeStampServiceURL();
        return null;
    }

    public void setAccessControlVS(AccessControlVS accessControl) {
        LOGD(TAG + ".setAccessControlVS", "serverURL: " + accessControl.getServerURL());
        this.accessControl = accessControl;
        serverMap.put(accessControl.getServerURL(), accessControl);
        PrefUtils.markAccessControlLoaded(accessControl.getServerURL(), this);
        state = PrefUtils.getAppCertState(this, this.accessControl.getServerURL());
    }

    public String getCurrentWeekLapseId() {
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        return DateUtils.getPath(currentLapseCalendar.getTime());
    }

    public KeyStore.PrivateKeyEntry getUserPrivateKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                USER_CERT_ALIAS, null);
        return keyEntry;
    }

    public X509Certificate getX509UserCert() throws CertificateException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        return (X509Certificate) keyEntry.getCertificateChain()[0];
    }

    public byte[] decryptMessage(byte[] base64EncryptedData) throws Exception {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        //X509Certificate cryptoTokenCert = (X509Certificate) keyEntry.getCertificateChain()[0];
        PrivateKey privateKey = keyEntry.getPrivateKey();
        return Encryptor.decryptCMS(privateKey, base64EncryptedData);
    }

    public ActorVS getActorVS(final String serverURL) {
        ActorVS targetServer = getServer(serverURL);
        if(targetServer == null) {
            if(Looper.getMainLooper().getThread() != Thread.currentThread()) { //not in main thread,
                //if invoked from main thread -> android.os.NetworkOnMainThreadException
                targetServer = getActorVSFromURL(serverURL);
            } else {
                /*LOGD(TAG + ".getActorVS", "FROM MAIN THREAD - CREATING NEW THREAD - " + serverURL);
                new Thread(new Runnable() {
                    @Override public void run() {  getActorVSFromURL(serverURL);  }
                }).start();*/
            }
        }
        return targetServer;
    }

    private ActorVS getActorVSFromURL(String serverURL) {
        ActorVS targetServer = null;
        try {
            ResponseVS responseVS = HttpHelper.getData(
                    ActorVS.getServerInfoURL(serverURL), ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                targetServer = ActorVS.parse(new JSONObject(responseVS.getMessage()));
                setServer(targetServer);
            }
        } catch(Exception ex) {
            LOGE(TAG + ".getActorVSFromURL", "ERROR fetching: " + serverURL);
        } finally { return targetServer; }
    }

    public void putWSSession(String UUID, WebSocketSession session) {
        sessionsMap.put(UUID, session.setUUID(UUID));
    }

    public WebSocketSession getWSSession(String UUID) {
        return sessionsMap.get(UUID);
    }

    public WebSocketSession getWSSession(Long deviceID) {
        for(String sessionUUID : sessionsMap.keySet()) {
            if(sessionsMap.get(sessionUUID) != null && sessionsMap.get(sessionUUID).
                    getDeviceVS() != null && sessionsMap.get(sessionUUID).getDeviceVS().
                    getId() == deviceID)
                return sessionsMap.get(sessionUUID);
        }
        return null;
    }

    public AESParams getSessionKey(String UUID) {
        return sessionsMap.get(UUID).getAESParams();
    }

    //http connections, if invoked from main thread -> android.os.NetworkOnMainThreadException
    public ResponseVS signMessage(String toUser, String textToSign, String subject,
              String timeStampServiceURL) {
        ResponseVS responseVS = null;
        try {
            String userVS = getUserVS().getNif();
            LOGD(TAG + ".signMessage", "subject: " + subject);
            KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyEntry.getPrivateKey(),
                    (X509Certificate) keyEntry.getCertificateChain()[0],
                    SIGNATURE_ALGORITHM, ANDROID_PROVIDER);
            SMIMEMessage smimeMessage = signedMailGenerator.getSMIME(userVS, toUser,
                    textToSign, subject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage,
                    timeStampServiceURL , this);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setCaption(getString(R.string.timestamp_service_error_caption));
                return responseVS;
            }
            responseVS = new ResponseVS(ResponseVS.SC_OK, timeStamper.getSMIME());
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    public ResponseVS signMessage(String toUser, String textToSign, String subject) {
        return signMessage(toUser, textToSign, subject, getTimeStampServiceURL());
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    public void setCooinServer(CooinServer cooinServer) {
        serverMap.put(cooinServer.getServerURL(), cooinServer);
        this.cooinServer = cooinServer;
    }

    public CooinServer getCooinServer() {
        if(cooinServer == null) cooinServer = (CooinServer) getActorVS(cooinServerURL);
        return cooinServer;
    }

    public void showNotification(ResponseVS responseVS){
        final NotificationManager mgr = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId.getAndIncrement(), clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentIntent(pendingIntent).setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle(responseVS.getCaption())
                .setContentText(responseVS.getNotificationMessage()).setSound(soundUri);
        if(responseVS.getStatusCode() == ResponseVS.SC_ERROR)
            builder.setSmallIcon(R.drawable.cancel_22);
        else if(responseVS.getStatusCode() == ResponseVS.SC_OK)
            builder.setSmallIcon(R.drawable.fa_check_32);
        else builder.setSmallIcon(R.drawable.mail_mark_unread_32);
        mgr.notify(notificationId.getAndIncrement(), builder.build());
    }

    public void broadcastResponse(ResponseVS responseVS, ArgVS... args ) {
        LOGD(TAG + ".broadcastResponse", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        if(args != null) {
            for(ArgVS argVS: args) intent.putExtra(argVS.getKey(), argVS.getValue());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public boolean hasWebSocketConnection() {
        return hasWebSocketConnection;
    }

    public void setHasWebSocketConnection(boolean hasWebSocketConnection) {
        this.hasWebSocketConnection = hasWebSocketConnection;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(accessControl != null) {//listen cert state changes
            String accessControlStateKey = STATE_KEY + "_" + accessControl.getServerURL();
            if(accessControlStateKey.equals(key)) {
                this.state = PrefUtils.getAppCertState(this, accessControl.getServerURL());
            }
        }
        if(USER_KEY.equals(key)) {
            userVS = PrefUtils.getSessionUserVS(this);
        }
    }

}