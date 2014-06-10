package org.votingsystem.android;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import com.itextpdf.text.Context_iTextVS;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.activity.BrowserVSActivity;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.votingsystem.model.ContextVS.ALGORITHM_RNG;
import static org.votingsystem.model.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.STATE_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.USER_DATA_FILE_NAME;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class AppContextVS extends Application {

    public static final String TAG = AppContextVS.class.getSimpleName();

    private State state = State.WITHOUT_CSR;
    private String vicketServerURL;
    private String webSocketSessionId = null;
    private String webSocketUserId = null;
    private static final Map<String, ActorVS> serverMap = new HashMap<String, ActorVS>();
    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private VicketServer vicketServer;
    private UserVS userVS;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private OperationVS operationVS = null;

    public void setServer(ActorVS actorVS) {
        if(serverMap.get(actorVS.getServerURL()) == null) {
            serverMap.put(actorVS.getServerURL(), actorVS);
        } else Log.d(TAG + ".setServer(...)", "ActorVS with URL '" + actorVS.getServerURL() +
                "' already in context");
    }

    public ActorVS getServer(String serverURL) {
        return serverMap.get(serverURL);
    }


	@Override public void onCreate() {
        //System.setProperty("android.os.Build.ID", android.os.Build.ID);
        Log.d(TAG + ".onCreate()", "");
        try {
            /*java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            java.security.KeyStore.PrivateKeyEntry keyEntry = (java.security.KeyStore.PrivateKeyEntry)
                    keyStore.getEntry("USER_CERT_ALIAS", null);
            Enumeration aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                Log.d(TAG, "Subject DN: " + cert.getSubjectX500Principal().toString());
                Log.d(TAG, "Issuer DN: " + cert.getIssuerDN().getName());
            }*/
            Context_iTextVS.init(getApplicationContext());
            VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            Properties props = new Properties();
            props.load(getAssets().open("VotingSystem.properties"));
            vicketServerURL = props.getProperty(ContextVS.VICKET_SERVER_URL);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
	}

    public String getVicketServerURL() {
        return vicketServerURL;
    }

    public String getHostID() {
        return android.os.Build.ID;
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    public void setOperationVS(OperationVS operationVS) {
        if(operationVS == null) Log.d(TAG + ".setOperationVS(...)", "- removing pending operationVS");
        else Log.d(TAG + ".setOperationVS(...)", "- operationVS: " + operationVS.getTypeVS());
        this.operationVS = operationVS;
    }

    public void setState(State state, String nif) {
        Log.d(TAG + ".setState(...)", STATE_KEY + "_" + accessControl.getServerURL() +
                " - state: " + state.toString());
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControl.getServerURL() , state.toString());
        if(nif != null) editor.putString(NIF_KEY, nif);
        if(State.WITH_CERTIFICATE == state) loadUser();
        editor.commit();
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public X509Certificate getCert(String serverURL) {
        Log.d(TAG + ".getCert(...)", " - getCert - serverURL: " + serverURL);
        if(serverURL == null) return null;
        return certsMap.get(serverURL);
    }

    public void putCert(String serverURL, X509Certificate cert) {
        Log.d(TAG + ".putCert(...)", " serverURL: " + serverURL);
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
        Log.d(TAG + ".setAccessControlURL() ", " - setAccessControlURL: " +
                accessControl.getServerURL());
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String stateStr = settings.getString(
                STATE_KEY + "_" + accessControl.getServerURL(), State.WITHOUT_CSR.toString());
        state = State.valueOf(stateStr);
        this.accessControl = accessControl;
        loadUser();
    }

    public void loadUser() {
        try {
            File representativeDataFile = new File(getFilesDir(), USER_DATA_FILE_NAME);
            if(representativeDataFile.exists()) {
                byte[] serializedUserData = FileUtils.getBytesFromFile(
                        representativeDataFile);
                userVS = (UserVS) ObjectUtils.deSerializeObject(serializedUserData);
            } else Log.d(TAG + ".loadUser(...)", "user data not found");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getLapseWeekLbl(Calendar calendar) {
        Calendar thisWeekMonday = DateUtils.getMonday(calendar);
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return getString(R.string.week_lapse_lbl, DateUtils.getDate_Es(
                thisWeekMonday.getTime()), DateUtils.getDate_Es(calendar.getTime()));
    }

    public void updateVicketAccountLastChecked() {
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ContextVS.VICKET_ACCOUNT_LAST_CHECKED_KEY,
        Calendar.getInstance().getTimeInMillis());
        editor.commit();
    }

    public Date getVicketAccountLastChecked() {
        SharedPreferences pref = getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(ContextVS.VICKET_ACCOUNT_LAST_CHECKED_KEY, 0));

        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());

        if(lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public void setPin(String pin) throws NoSuchAlgorithmException {
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String hashPin = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
        editor.putString(ContextVS.PIN_KEY, hashPin);
        editor.commit();
    }

    public String getStoredPasswordHash() throws NoSuchAlgorithmException,
            VotingSystemKeyStoreException {
        SharedPreferences settings = getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.PIN_KEY, null);
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
                Log.d(TAG + ".decryptMessageVS(...) ", "Cert matched - serialNumber: " + serialNumber);
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
            Log.d(TAG + ".signMessage(...) ", "subject: " + subject);
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

    public VicketServer getVicketServer() {
        return vicketServer;
    }

    public void setVicketServer(VicketServer vicketServer) {
        serverMap.put(vicketServer.getServerURL(), vicketServer);
        this.vicketServer = vicketServer;
    }


    public void showNotification(ResponseVS responseVS){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(this, MessageActivity.class);
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
        Log.d(TAG + ".sendBroadcast(...) ", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        if(responseVS.getTypeVS() != null) {
            Intent intent = new Intent(responseVS.getServiceCaller());
            intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
        }
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
}