package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.TransactionRequest;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.util.Calendar;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSService extends IntentService {

    public static final long FOUR_MINUTES = 60 * 4 * 1000;

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
        LOGD(TAG + ".onHandleIntent", "onHandleIntent");
        ResponseVS responseVS = null;
        switch(operation) {
            case TRANSACTIONVS:
                try {
                    responseVS = sendTransactionVS(transactionVS.getToUserVS().getIBAN(),
                            transactionVS.transactionFromUserVSJSON(fromUserIBAN));
                    broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                            responseVS, contextVS));
                } catch (Exception ex) { ex.printStackTrace(); }
                break;
            case SIGNED_TRANSACTION:
                try {
                    JSONObject transactionRequestJSON =
                            new JSONObject(arguments.getString(ContextVS.JSON_DATA_KEY));
                    TransactionRequest transactionRequest = TransactionRequest.parse(transactionRequestJSON);
                    processPayment(transactionRequest, operation);
                } catch (Exception ex) { ex.printStackTrace(); }
                break;
            case ANONYMOUS_SIGNED_TRANSACTION:
                break;
            default: LOGD(TAG + ".onHandleIntent", "unprocessed operation: " + operation.toString());
        }
    }

    private void processPayment(TransactionRequest transactionRequest, TypeVS operation) {
        LOGD(TAG + ".processPayment", "processPayment");
        UserVS userVS = PrefUtils.getSessionUserVS(this);
        ResponseVS responseVS = null;
        if(transactionRequest.getDate() != null && DateUtils.inRange(transactionRequest.getDate(),
                Calendar.getInstance().getTime(), FOUR_MINUTES)) {
            try {
                switch (transactionRequest.getPaymentMethod()) {
                    case SIGNED_TRANSACTION:
                        responseVS = sendTransactionVS(transactionRequest.getIBAN(),  transactionRequest.
                                getCooinServerTransaction(userVS.getIBAN()));
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionRequest.getPaymentConfirmURL());
                        }
                        break;
                    case ANONYMOUS_SIGNED_TRANSACTION:
                        break;
                    case COOIN_SEND:
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.getExceptionResponse(ex, this);
            }
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                    getString(R.string.payment_session_expired_msg));
        broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller, responseVS,
                contextVS));
    }

    private ResponseVS sendTransactionVS(String toUserIBAN, JSONObject transactionVSJSON) {
        LOGD(TAG + ".sendTransactionVS", "transactionVS: " + transactionVSJSON.toString());
        ResponseVS responseVS = null;
        try {
            CooinServer cooinServer = contextVS.getCooinServer();
            responseVS = contextVS.signMessage(toUserIBAN,
                    transactionVSJSON.toString(), getString(R.string.FROM_USERVS_msg_subject));
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                    ContentTypeVS.JSON_SIGNED, cooinServer.getTransactionVSServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        contextVS.showNotification(responseVS);
        contextVS.broadcastResponse(responseVS);
    }

}