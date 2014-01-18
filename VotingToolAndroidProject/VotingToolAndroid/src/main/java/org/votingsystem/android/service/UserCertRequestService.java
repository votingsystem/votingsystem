package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.HttpHelper;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.votingsystem.model.ContextVS.CALLER_KEY;
import static org.votingsystem.model.ContextVS.CAPTION_KEY;
import static org.votingsystem.model.ContextVS.CSR_REQUEST_ID_KEY;
import static org.votingsystem.model.ContextVS.DEVICE_ID_KEY;
import static org.votingsystem.model.ContextVS.EMAIL_KEY;
import static org.votingsystem.model.ContextVS.KEYSTORE_TYPE;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.MESSAGE_KEY;
import static org.votingsystem.model.ContextVS.NAME_KEY;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.PHONE_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.RESPONSE_STATUS_KEY;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.SURNAME_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class UserCertRequestService extends IntentService {

    public static final String TAG = "UserCertRequestService";

    public UserCertRequestService() { super(TAG); }

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        Log.d(TAG + ".onHandleIntent(...) ", "arguments: " + arguments);
        AppContextVS appContextVS = (AppContextVS) getApplicationContext();
        try {
            String serviceCaller = arguments.getString(CALLER_KEY);
            String nif = arguments.getString(NIF_KEY);
            String email = arguments.getString(EMAIL_KEY);
            String phone = arguments.getString(PHONE_KEY);
            String deviceId = arguments.getString(DEVICE_ID_KEY);
            String givenName = arguments.getString(NAME_KEY);
            String surname = arguments.getString(SURNAME_KEY);
            String pin = arguments.getString(PIN_KEY);
            CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                    KEY_SIZE, SIG_NAME, SIGNATURE_ALGORITHM, PROVIDER, nif, email, phone, deviceId,
                    givenName, surname);
            byte[] csrBytes = certificationRequest.getCsrPEM();
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(null, null);
            X509Certificate[] dummyCerts = CertUtil.generateCertificate(
                    certificationRequest.getKeyPair(), new Date(System.currentTimeMillis()),
                    new Date(System.currentTimeMillis()), "CN=Dummy" + USER_CERT_ALIAS);
            keyStore.setKeyEntry(USER_CERT_ALIAS, certificationRequest.getPrivateKey(),
                    pin.toCharArray(), dummyCerts);
            byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, pin.toCharArray());
            FileOutputStream fos = openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
            fos.write(keyStoreBytes);
            fos.close();
            ResponseVS responseVS = HttpHelper.sendData(csrBytes, null,
                    appContextVS.getAccessControl().getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SharedPreferences settings = getApplicationContext().getSharedPreferences(
                        VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                Long requestId = Long.valueOf(responseVS.getMessage());
                editor.putLong(CSR_REQUEST_ID_KEY, requestId);
                editor.commit();
                appContextVS.setState(State.WITH_CSR, null);
            }
            String caption = null;
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                caption = getString(R.string.operation_ok_msg);
            } else caption = getString(R.string.operation_error_msg);
            sendMessage(responseVS.getStatusCode(), caption, responseVS.getMessage(),serviceCaller);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendMessage(Integer statusCode, String caption, String message,
                             String serviceCaller) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - caption: " +
                caption  + " - message: " + message);
        Intent intent = new Intent(serviceCaller);
        if(statusCode != null)
            intent.putExtra(RESPONSE_STATUS_KEY, statusCode.intValue());
        if(caption != null) intent.putExtra(CAPTION_KEY, caption);
        if(message != null) intent.putExtra(MESSAGE_KEY, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
