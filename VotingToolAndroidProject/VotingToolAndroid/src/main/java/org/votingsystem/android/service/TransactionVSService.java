package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSService extends IntentService {

    public static final String TAG = TransactionVSService.class.getSimpleName();

    public TransactionVSService() { super(TAG); }

    private AppContextVS contextVS;
    private String serviceCaller;

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String fromUserIBAN = arguments.getString(ContextVS.IBAN_KEY);
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        TransactionVS transactionVS = (TransactionVS) intent.getSerializableExtra(ContextVS.TRANSACTION_KEY);
        LOGD(TAG + ".onHandleIntent", "transactionVS: " + transactionVS.toString());
        ResponseVS responseVS = null;
        String caption = null;
        String message = null;
        try {
            JSONObject transactionVSJSON = transactionVS.transactionFromUserVSJSON(fromUserIBAN);
            VicketServer vicketServer = contextVS.getVicketServer();
            responseVS = contextVS.signMessage(transactionVS.getToUserVS().getIBAN(),
                    transactionVSJSON.toString(), getString(R.string.FROM_USERVS_msg_subject));
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                    ContentTypeVS.JSON_SIGNED, vicketServer.getTransactionVSServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller, responseVS, contextVS));
        }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        contextVS.showNotification(responseVS);
        contextVS.broadcastResponse(responseVS);
    }

}