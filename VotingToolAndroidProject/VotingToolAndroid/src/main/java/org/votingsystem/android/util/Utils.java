package org.votingsystem.android.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.votingsystem.android.R;
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

    public static ResponseVS getBroadcastResponse(TypeVS operation, String serviceCaller,
              ResponseVS responseVS, Context context) {
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            if(responseVS.getIconId() == null) responseVS.setIconId(R.drawable.fa_check_32);
            if(responseVS.getCaption() == null) responseVS.setCaption(context.getString(R.string.ok_lbl));

        } else {
            if(responseVS.getIconId() == null) responseVS.setIconId(R.drawable.fa_times_32);
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
        if (intent == null) {
            return arguments;
        }
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

}
