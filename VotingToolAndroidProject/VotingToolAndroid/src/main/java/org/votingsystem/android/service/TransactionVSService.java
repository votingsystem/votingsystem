package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VicketServer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        TransactionVS transactionVS = (TransactionVS) intent.getSerializableExtra(ContextVS.TRANSACTION_KEY);
        Log.d(TAG + ".onHandleIntent(...) ", " - transactionVS: " + transactionVS.toString()  );
        ResponseVS responseVS = null;
        String caption = null;
        String message = null;
        try {
            Map mapToSend = new HashMap();
            mapToSend.put("operation", TypeVS.TRANSACTIONVS_FROM_USERVS.toString());
            mapToSend.put("subject", transactionVS.getSubject());
            mapToSend.put("toUser", transactionVS.getToUserVS().getName());
            mapToSend.put("toUserIBAN", Arrays.asList(transactionVS.getToUserVS().getIBAN()));
            mapToSend.put("tags", Arrays.asList(transactionVS.getTagVS().getName()));
            mapToSend.put("amount", transactionVS.getAmount().toString());
            mapToSend.put("currency", transactionVS.getCurrencyCode());
            mapToSend.put("UUID", UUID.randomUUID().toString());
            responseVS = contextVS.getHTTPVicketServer();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                VicketServer vicketServer = (VicketServer) responseVS.getData();
                SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                        contextVS.getUserVS().getNif(), transactionVS.getToUserVS().getIBAN(),
                        vicketServer.getTransactionVSServiceURL(), new JSONObject(mapToSend).toString(),
                        ContentTypeVS.JSON_SIGNED, getString(R.string.transactionvs_from_uservs_msg_subject), null, contextVS);
                responseVS = smimeSignedSender.call();

            }
        } catch(Exception ex) {
            ex.printStackTrace();
            message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                    message);
        } finally {
            broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller, responseVS, contextVS));
        }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        contextVS.showNotification(responseVS);
        contextVS.sendBroadcast(responseVS);
    }

}