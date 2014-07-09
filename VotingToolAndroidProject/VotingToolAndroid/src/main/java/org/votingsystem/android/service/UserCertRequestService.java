package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import static org.votingsystem.model.ContextVS.CALLER_KEY;
import static org.votingsystem.model.ContextVS.CSR_REQUEST_ID_KEY;
import static org.votingsystem.model.ContextVS.DEVICE_ID_KEY;
import static org.votingsystem.model.ContextVS.EMAIL_KEY;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.NAME_KEY;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.PHONE_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.SURNAME_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserCertRequestService extends IntentService {

    public static final String TAG = UserCertRequestService.class.getSimpleName();

    public UserCertRequestService() { super(TAG); }


    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        Log.d(TAG + ".onHandleIntent(...) ", "arguments: " + arguments);
        AppContextVS contextVS = (AppContextVS) getApplicationContext();
        String serviceCaller = arguments.getString(CALLER_KEY);
        ResponseVS responseVS = null;
        try {
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
            SharedPreferences settings = getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
            editor.putString(ContextVS.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
            editor.commit();

            responseVS = HttpHelper.sendData(csrBytes, null,
                    contextVS.getAccessControl().getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Long requestId = Long.valueOf(responseVS.getMessage());
                editor.putLong(CSR_REQUEST_ID_KEY, requestId);
                contextVS.setPin(pin);
                editor.commit();
                contextVS.setState(State.WITH_CSR, null);
            }
            String caption = null;
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                caption = getString(R.string.operation_ok_msg);
            } else caption = getString(R.string.operation_error_msg);
            responseVS.setCaption(caption);
        } catch (Exception ex){
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                    message);
        } finally {
            responseVS.setServiceCaller(serviceCaller);
            contextVS.sendBroadcast(responseVS);
        }
    }

}
