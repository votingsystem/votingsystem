package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.model.VoteVS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


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
            responseVS = contextVS.updateVicketServer();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                VicketServer vicketServer = (VicketServer) responseVS.getData();
                SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                        contextVS.getUserVS().getNif(), transactionVS.getToUserVS().getIBAN(),
                        vicketServer.getTransactionVSServiceURL(), signatureContent,
                        ContentTypeVS.JSON, messageSubject, null, contextVS);
                responseVS = smimeSignedSender.call();

                webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:"deposit", absolute:true)}"
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally {
            responseVS.setServiceCaller(serviceCaller);
            responseVS.setTypeVS(operation);
            broadCastResponse(responseVS);
        }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        responseVS.setServiceCaller(serviceCaller);
        contextVS.showNotification(responseVS);
        contextVS.sendBroadcast(responseVS);
    }

}