package org.votingsystem.model;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.itextpdf.text.Context_iTextVS;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;

public class ContextVS {

    public enum State {WITH_CERTIFICATE, WITH_CSR, WITHOUT_CSR}

    public static final String TAG = "ContextVS";

    public static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static final int VOTE_TAG                                = 0;
    public static final int REPRESENTATIVE_VOTE_TAG                 = 1;
    public static final int ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG = 2;

    public static final String VOTING_SYSTEM_BASE_OID = "0.0.0.0.0.0.0.0.0.";
    public static final String REPRESENTATIVE_VOTE_OID = VOTING_SYSTEM_BASE_OID + REPRESENTATIVE_VOTE_TAG;
    public static final String ANONYMOUS_REPRESENTATIVE_DELEGATION_OID = VOTING_SYSTEM_BASE_OID +
            ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG;
    public static final String VOTE_OID = VOTING_SYSTEM_BASE_OID + VOTE_TAG;

    public static final String VOTING_SYSTEM_PRIVATE_PREFS = "VotingSystemSharedPrivatePreferences";

    public static final String SIGNED_FILE_NAME              = "signedFile";
    public static final String CSR_FILE_NAME                 = "csr";
    public static final String IMAGE_FILE_NAME               = "image";
    public static final String ACCESS_REQUEST_FILE_NAME      = "accessRequest";
    public static final String REPRESENTATIVE_DATA_FILE_NAME = "representativeData";
    public static final String DEFAULT_SIGNED_FILE_NAME      = "smimeMessage.p7m";
    public static final String PROVIDER                      = "BC";
    public static final String SERVER_URL_EXTRA_PROP_NAME    = "serverURL";

    //Intent keys
    public static final String FRAGMENT_KEY = "FRAGMENT_KEY";
    public static final String PIN_KEY = "PIN";
    public static final String URL_KEY = "URL";
    public static final String FORM_DATA_KEY = "FORM_DATA";
    public static final String NIF_KEY = "NIF";
    public static final String EMAIL_KEY = "EMAIL_KEY";
    public static final String SURNAME_KEY = "SURNAME_KEY";
    public static final String PHONE_KEY = "PHONE_KEY";
    public static final String DEVICE_ID_KEY = "DEVICE_ID_KEY";
    public static final String NAME_KEY = "NAME_KEY";
    public static final String URI_KEY = "URI_DATA";
    public static final String ACCESS_CONTROL_URL_KEY = "ACCESS_CONTROL_URL";
    public static final String CALLER_KEY = "CALLER_KEY";
    public static final String MESSAGE_KEY = "MESSAGE_KEY";
    public static final String MESSAGE_BYTES_KEY = "MESSAGE_BYTES_KEY";
    public static final String PASSWORD_CONFIRM_KEY = "PASSWORD_CONFIRM_KEY";
    public static final String CERT_VALIDATION_KEY = "CERT_VALIDATION_KEY";
    public static final String HTML_MESSAGE_KEY = "HTML_MESSAGE_KEY";
    public static final String TYPEVS_KEY = "TYPEVS_KEY";
    public static final String MESSAGE_SUBJECT_KEY = "MESSAGE_SUBJECT_KEY";
    public static final String RESPONSE_STATUS_KEY = "RESPONSE_STATUS";
    public static final String OFFSET_KEY = "OFFSET";
    public static final String CAPTION_KEY = "CAPTION";
    public static final String ERROR_PANEL_KEY = "ERROR_PANEL";
    public static final String ICON_KEY = "ICON_KEY";
    public static final String LOADING_KEY = "LOADING_KEY";
    public static final String NUM_TOTAL_KEY = "NUM_TOTAL";
    public static final String LIST_STATE_KEY = "LIST_STATE";
    public static final String ITEM_ID_KEY = "ITEM_ID";
    public static final String CONTENT_TYPE_KEY = "CONTENT_TYPE_KEY";
    public static final String CURSOR_POSITION_KEY = "CURSOR_POSITION";
    public static final String EVENT_STATE_KEY = "EVENT_STATE";
    public static final String CHILD_POSITION_KEY  = "CHILD_POSITION";
    public static final String EVENTVS_KEY  = "EVENTVS";
    public static final String VOTE_KEY  = "VOTE_KEY";
    public static final String RECEIPT_KEY  = "RECEIPT_KEY";
    public static final String STATE_KEY                   = "STATE";
    public static final String CSR_REQUEST_ID_KEY          = "csrRequestId";
    public static final String APPLICATION_ID_KEY          = "idAplicacion";

    //Pages size
    //public static final Integer REPRESENTATIVE_PAGE_SIZE = 100;
    public static final Integer REPRESENTATIVE_PAGE_SIZE = 20;
    public static final Integer EVENTVS_PAGE_SIZE = 20;


    //loader IDs
    public static final int RECEIPT_LOADER_ID = 1;

    //Notifications IDs
    public static final int RSS_SERVICE_NOTIFICATION_ID           = 1;
    public static final int SIGN_AND_SEND_SERVICE_NOTIFICATION_ID = 2;
    public static final int VOTE_SERVICE_NOTIFICATION_ID          = 3;

    public static final int NUM_MIN_OPTIONS = 2;

    public static final int KEY_SIZE = 1024;
    public static final int SYMETRIC_ENCRYPTION_KEY_LENGTH = 256;
    public static final int SYMETRIC_ENCRYPTION_ITERATION_COUNT = 100;

    public static final int MAX_REPRESENTATIVE_IMAGE_FILE_SIZE = 512 * 1024;

    public static final int EVENTS_PAGE_SIZE = 30;
    public static final int MAX_SUBJECT_SIZE = 60;
    public static final int SELECTED_OPTION_MAX_LENGTH       = 60;
    //TODO por el bug en froyo de -> JcaDigestCalculatorProviderBuilder
    public static final String VOTING_DATA_DIGEST = "SHA256";
    public static final String SIG_NAME = "RSA";
    /** Random Number Generator algorithm. */
    private static final String ALGORITHM_RNG = "SHA1PRNG";
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    //public static final String VOTE_SIGN_MECHANISM = "SHA512withRSA";
    public static final String VOTE_SIGN_MECHANISM = "SHA256WithRSA";
    public static final String USER_CERT_ALIAS = "CertificadoUsuario";
    public static final String KEY_STORE_FILE = "keyStoreVS.p12";

    public static final String TIMESTAMP_USU_HASH = "2.16.840.1.101.3.4.2.1";//TSPAlgorithms.SHA256
    public static final String TIMESTAMP_VOTE_HASH = "2.16.840.1.101.3.4.2.1";//TSPAlgorithms.SHA256

    public static final String VOTING_HEADER_LABEL  = "votingSystemMessageType";

    private State state = State.WITHOUT_CSR;

    private AccessControlVS accessControl;
    private ControlCenterVS controlCenter;
    private UserVS userVS;
    private Map<String, X509Certificate> certsMap = new HashMap<String, X509Certificate>();
    private OperationVS operationVS = null;
    private static ContextVS INSTANCE;
    private Context context = null;

    private static PropertyResourceBundle resourceBundle;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {

        }};

    private ContextVS(Context context) {
        System.setProperty("android.os.Build.ID", android.os.Build.ID);
        this.context = context;
        try {
            Context_iTextVS.init(context);
            VotingSystemKeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            InputStream inputStream = context.getAssets().open("messages_es.properties");
            resourceBundle = new PropertyResourceBundle(inputStream);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public static ContextVS getInstance(Context context) {
        if(INSTANCE == null) {
            Log.d(TAG + ".getInstance(...)", "getInstance -  android.os.Build.ID: " +
                    android.os.Build.ID);
            INSTANCE = new ContextVS(context);
        }
        return INSTANCE;
    }

    public static String getMessage(String key, Object... arguments) {
        try {
            String pattern = resourceBundle.getString(key);
            if(arguments != null && arguments.length > 0)
                return MessageFormat.format(pattern, arguments);
            else return pattern;
        } catch(Exception ex) {
            ex.printStackTrace();
            Log.d(TAG + "getMessage(...)", "### Value not found for key: " + key);
            return "---" + key + "---";
        }
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

    public void setState(State state) {
        Log.d(TAG + ".setState(...)", STATE_KEY + "_" + accessControl.getServerURL() +
                " - state: " + state.toString());
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControl.getServerURL() , state.toString());
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

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControlVS(AccessControlVS accessControl) {
        Log.d(TAG + ".setAccessControlURL() ", " - setAccessControlURL: " +
                accessControl.getServerURL());
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String stateStr = settings.getString(
                STATE_KEY + "_" + accessControl.getServerURL(), State.WITHOUT_CSR.toString());
        state = State.valueOf(stateStr);
        this.accessControl = accessControl;
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

}