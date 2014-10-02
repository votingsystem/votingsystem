package org.votingsystem.android;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.activity.BrowserVSActivity;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.contentprovider.VicketContentProvider;
import org.votingsystem.android.service.BootStrapService;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.votingsystem.android.util.LogUtils.LOGD;
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
    private String webSocketSessionId = null;
    private String webSocketUserId = null;
    private static final Map<String, ActorVS> serverMap = new HashMap<String, ActorVS>();
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private VicketServer vicketServer;
    private UserVS userVS;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();

    @Override public void onCreate() {
        //System.setProperty("android.os.Build.ID", android.os.Build.ID);
        try {
            /*java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            java.security.KeyStore.PrivateKeyEntry keyEntry = (java.security.KeyStore.PrivateKeyEntry)
                    keyStore.getEntry("USER_CERT_ALIAS", null);
            Enumeration aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                LOGD(TAG, "Subject DN: " + cert.getSubjectX500Principal().toString());
                LOGD(TAG, "Issuer DN: " + cert.getIssuerDN().getName());
            }*/
            VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            PrefUtils.init(this);
            Properties props = new Properties();
            props.load(getAssets().open("VotingSystem.properties"));
            String vicketServerURL = props.getProperty(ContextVS.VICKET_SERVER_URL);
            String accessControlURL = props.getProperty(ContextVS.ACCESS_CONTROL_URL_KEY);
            LOGD(TAG + ".onCreate()", "accessControlURL: " + accessControlURL + " - vicketServerURL: " +
                    vicketServerURL);
            if(accessControl == null || vicketServer == null) {
                Intent startIntent = new Intent(this, BootStrapService.class);
                startIntent.putExtra(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL);
                startIntent.putExtra(ContextVS.VICKET_SERVER_URL, vicketServerURL);
                startService(startIntent);
            }
            PrefUtils.registerOnSharedPreferenceChangeListener(this, this);
            state = PrefUtils.getAppCertState(this, accessControlURL);
            userVS = PrefUtils.getSessionUserVS(this);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
	}

    public void finish() {
        stopService(new Intent(getApplicationContext(), WebSocketService.class));
        UIUtils.killApp(true);
    }

    public void setServer(ActorVS actorVS) {
        if(serverMap.get(actorVS.getServerURL()) == null) {
            serverMap.put(actorVS.getServerURL(), actorVS);
        } else LOGD(TAG + ".setServer(...)", "ActorVS with URL '" + actorVS.getServerURL() +
                "' already in context");
    }

    public ActorVS getServer(String serverURL) {
        return serverMap.get(serverURL);
    }

    @Override public void onTerminate() {
        LOGD(TAG + ".onTerminate(...)", "");
        super.onTerminate();
        PrefUtils.unregisterOnSharedPreferenceChangeListener(this, this);
    }

    public String getHostID() {
        return android.os.Build.ID;
    }

    public State getState() {
        return state;
    }

    public X509Certificate getCert(String serverURL) {
        LOGD(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void putCert(String serverURL, X509Certificate cert) {
        LOGD(TAG + ".putCert(...)", " serverURL: " + serverURL);
        certsMap.put(serverURL, cert);
    }

    public X509Certificate getTimeStampCert() {
        if(accessControl == null) return null;
        return getAccessControl().getTimeStampCert();
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControlVS(AccessControlVS accessControl) {
        LOGD(TAG + ".setAccessControlVS() ", "serverURL: " + accessControl.getServerURL());
        this.accessControl = accessControl;
        state = PrefUtils.getAppCertState(this, this.accessControl.getServerURL());
    }

    public List<Vicket> getVicketList(String currencyCode) {
        String selection = VicketContentProvider.WEEK_LAPSE_COL + " =? AND " +
                VicketContentProvider.STATE_COL + " =? AND " +
                VicketContentProvider.CURRENCY_COL + "= ? ";
        String weekLapseId = getCurrentWeekLapseId();
        Cursor cursor = getContentResolver().query(VicketContentProvider.CONTENT_URI,null, selection,
                new String[]{weekLapseId, Vicket.State.OK.toString(), currencyCode}, null);
        LOGD(TAG + ".getCurrencyData(...)", "VicketContentProvider - cursor.getCount(): " + cursor.getCount());
        List<Vicket> vicketList = new ArrayList<Vicket>();
        while(cursor.moveToNext()) {
            Vicket vicket = (Vicket) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(VicketContentProvider.SERIALIZED_OBJECT_COL)));
            Long vicketId = cursor.getLong(cursor.getColumnIndex(VicketContentProvider.ID_COL));
            vicket.setLocalId(vicketId);
            vicketList.add(vicket);
        }
        selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? AND " +
                TransactionVSContentProvider.CURRENCY_COL + "= ? ";
        cursor =  getContentResolver().query(TransactionVSContentProvider.CONTENT_URI,null,
                selection, new String[]{weekLapseId, currencyCode}, null);
        List<TransactionVS> transactionList = new ArrayList<TransactionVS>();
        while(cursor.moveToNext()) {
            TransactionVS transactionVS = (TransactionVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(TransactionVSContentProvider.SERIALIZED_OBJECT_COL)));
            transactionList.add(transactionVS);
        }
        return vicketList;
    }

    public String getCurrentWeekLapseId() {
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        return DateUtils.getDirPath(currentLapseCalendar.getTime());
    }

    public KeyStore.PrivateKeyEntry getUserPrivateKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                USER_CERT_ALIAS, null);
        return keyEntry;
    }

    public ResponseVS decryptMessageVS(JSONObject documentToDecrypt) throws Exception {
        ResponseVS responseVS = null;
        JSONArray encryptedDataArray = documentToDecrypt.getJSONArray("encryptedDataList");
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        X509Certificate cryptoTokenCert = (X509Certificate) keyEntry.getCertificateChain()[0];
        PrivateKey privateKey = keyEntry.getPrivateKey();
        String encryptedData = null;
        for(int i = 0 ; i < encryptedDataArray.length(); i++){
            JSONObject arrayItem = encryptedDataArray.getJSONObject(i);
            Long serialNumber = Long.valueOf((String) arrayItem.get("serialNumber"));
            if(serialNumber == cryptoTokenCert.getSerialNumber().longValue()) {
                LOGD(TAG + ".decryptMessageVS(...) ", "Cert matched - serialNumber: " + serialNumber);
                encryptedData = (String) arrayItem.get("encryptedData");
            }
        }
        if(encryptedData != null) {
            responseVS = Encryptor.decryptCMS(encryptedData.getBytes(), privateKey);
            responseVS.setContentType(ContentTypeVS.JSON);
            Map editDataMap = new HashMap();
            editDataMap.put("operation", TypeVS.MESSAGEVS_EDIT.toString());
            editDataMap.put("locale", Locale.getDefault().getLanguage().toLowerCase());
            editDataMap.put("state", "CONSUMED");
            editDataMap.put("messageId", documentToDecrypt.get("id"));
            JSONObject messageToWebSocket = new JSONObject(editDataMap);
            responseVS.setData(messageToWebSocket);
        }
        else {
            Log.e(TAG + ".decryptMessageVS(...)", "Unable to decrypt from this device");
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, getString(R.string.decrypt_cert_notfound_msg));
        }
        return responseVS;
    }

    public ResponseVS signMessage(String toUser, String textToSign, String subject) {
        ResponseVS responseVS = null;
        try {
            String userVS = getUserVS().getNif();
            LOGD(TAG + ".signMessage(...) ", "subject: " + subject);
            KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyEntry.getPrivateKey(),
                    (X509Certificate) keyEntry.getCertificateChain()[0],
                    SIGNATURE_ALGORITHM, ANDROID_PROVIDER);
            SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(userVS, toUser,
                    textToSign, subject);
            //we can't timestamp here because of android.os.NetworkOnMainThreadException
            responseVS = new ResponseVS(ResponseVS.SC_OK, smimeMessage);
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),message);
        } finally {
            return responseVS;
        }
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    public void setVicketServer(VicketServer vicketServer) {
        serverMap.put(vicketServer.getServerURL(), vicketServer);
        this.vicketServer = vicketServer;
    }

    public VicketServer getVicketServer() {
        return vicketServer;
    }

    public void showNotification(ResponseVS responseVS){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(this, MessageActivity.class);http://javapapers.com/core-java/java-email/
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                VICKET_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(responseVS.getCaption()).setContentText(Html.fromHtml(
                        responseVS.getNotificationMessage())).setSmallIcon(responseVS.getIconId())
                .setContentIntent(pendingIntent);
        Notification note = builder.build();
        // hide the notification after its selected
        note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.REPRESENTATIVE_SERVICE_NOTIFICATION_ID, note);
    }

    public void sendBroadcast(ResponseVS responseVS) {
        LOGD(TAG + ".sendBroadcast(...) ", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        if(responseVS.getTypeVS() != null) {
            switch(responseVS.getTypeVS()) {
                case INIT_VALIDATED_SESSION:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        try {
                            setWebSocketSessionId(responseVS.getMessageJSON().getString("sessionId"));
                            setWebSocketUserId(responseVS.getMessageJSON().getString("userId"));
                            if(responseVS.getMessageJSON().has("messageVSList")) {
                                JSONArray messageVSList = responseVS.getMessageJSON().getJSONArray("messageVSList");
                                if(messageVSList.length() > 0) {
                                    String jsCommand = "javascript:updateMessageVSList('" + responseVS.getMessageJSON().toString() + "')";
                                    intent = new Intent(this, BrowserVSActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra(ContextVS.JS_COMMAND_KEY, jsCommand);
                                    intent.putExtra(ContextVS.URL_KEY, getVicketServer().getMessageVSInboxURL());
                                    startActivity(intent);
                                }
                            }
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;
                case WEB_SOCKET_CLOSE:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        setWebSocketSessionId(null);
                        setWebSocketUserId(null);
                    }
                    break;
            }
        } else  LOGD(TAG + ".sendBroadcast(...) ", "broadcast response with null type!!!");
    }

    public String getWebSocketSessionId() {
        return webSocketSessionId;
    }

    public void setWebSocketSessionId(String webSocketSessionId) {
        this.webSocketSessionId = webSocketSessionId;
    }

    public String getWebSocketUserId() {
        return webSocketUserId;
    }

    public void setWebSocketUserId(String webSocketUserId) {
        this.webSocketUserId = webSocketUserId;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, "onSharedPreferenceChanged - key: " + key);
        if(accessControl != null) {
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