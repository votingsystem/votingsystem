package org.votingsystem.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.zxing.integration.android.IntentIntegrator;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static void launchQRScanner(Activity activity) {
        IntentIntegrator integrator = null;
        if(activity != null) integrator = new IntentIntegrator(activity);
        integrator.addExtra("SCAN_WIDTH", 500);
        integrator.addExtra("SCAN_HEIGHT", 500);
        integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
        integrator.addExtra("PROMPT_MESSAGE", "Enfoque el código QR");
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES, activity);
    }

    public static ResponseVS getBroadcastResponse(TypeVS operation, String serviceCaller,
              ResponseVS responseVS, Context context) {
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            if(responseVS.getCaption() == null) responseVS.setCaption(context.getString(R.string.ok_lbl));

        } else {
            if(responseVS.getCaption() == null) responseVS.setCaption(context.getString(R.string.error_lbl));
        }
        responseVS.setTypeVS(operation).setServiceCaller(serviceCaller);
        return responseVS;
    }

    public static void printKeyStoreInfo() throws CertificateException, NoSuchAlgorithmException,
            IOException, KeyStoreException, UnrecoverableEntryException {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.KeyStore.PrivateKeyEntry keyEntry = (java.security.KeyStore.PrivateKeyEntry)
                keyStore.getEntry("USER_CERT_ALIAS", null);
        Enumeration aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            LOGD(TAG, "Subject DN: " + cert.getSubjectX500Principal().toString());
            LOGD(TAG, "Issuer DN: " + cert.getIssuerDN().getName());
        }
    }

    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null)return arguments;
        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }
        return arguments;
    }

    public static void toggleWebSocketServiceConnection(AppContextVS contextVS) {
        Intent startIntent = new Intent(contextVS, WebSocketService.class);
        TypeVS typeVS = TypeVS.WEB_SOCKET_INIT;
        if(contextVS.hasWebSocketConnection()) typeVS = TypeVS.WEB_SOCKET_CLOSE;
        LOGD(TAG + ".toggleWebSocketServiceConnection", "operation: " + typeVS.toString());
        startIntent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        contextVS.startService(startIntent);
    }

    public static void closeQuietly(final Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

}
