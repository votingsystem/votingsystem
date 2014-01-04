package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.VoteSender;
import org.votingsystem.android.contentprovider.VoteReceiptDBHelper;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VoteService extends IntentService {

    public static final String TAG = "VoteService";

    public enum Operation {CANCEL_VOTE, SAVE_VOTE, VOTE};

    public VoteService() { super(TAG); }

    private ContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        Operation operation = (Operation)arguments.getSerializable(ContextVS.OPERATION_KEY);
        try {
            contextVS = ContextVS.getInstance(getApplicationContext());
            Long eventId = arguments.getLong(ContextVS.ITEM_ID_KEY);
            String pin = arguments.getString(ContextVS.PIN_KEY);
            String signatureContent = arguments.getString(ContextVS.MESSAGE_KEY);
            EventVS eventVS = (EventVS) intent.getSerializableExtra(ContextVS.EVENTVS_KEY);
            Log.d(TAG + ".onHandleIntent(...) ", "operation: " + operation + " - event: " +
                    eventVS.getId());
            ActorVS controlCenter = eventVS.getControlCenter();
            X509Certificate controlCenterCert = contextVS.getCert(controlCenter.getServerURL());
            ResponseVS responseVS = null;
            if(controlCenterCert == null) {
                responseVS = HttpHelper.getData(controlCenter.getCertChainURL(), null);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    sendMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                            getString(R.string.get_cert_error_msg, controlCenter.getName()),
                            serviceCaller, operation);
                    return;
                } else {
                    Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(
                            responseVS.getMessageBytes());
                    controlCenterCert = certChain.iterator().next();
                    contextVS.putCert(controlCenter.getServerURL(), controlCenterCert);
                }
            }
            eventVS.getControlCenter().setCertificate(controlCenterCert);

            String caption = null;
            String message = null;
            switch(operation) {
                case VOTE:
                    responseVS = processVote(pin, eventVS);
                    break;
                case CANCEL_VOTE:
                    responseVS = processCancellation(pin, signatureContent);
                    break;
            }
            showNotification(responseVS, eventVS.getSubject(), operation);
            switch(operation) {
                case VOTE:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        VoteVS voteReceipt = (VoteVS)responseVS.getData();
                        contextVS.putVoteReceipt(eventVS.getId(), voteReceipt);
                    } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                        caption = getString(R.string.access_request_repeated_caption);
                        message = getString( R.string.access_request_repeated_msg,
                                eventVS.getSubject(), responseVS.getMessage());
                    } else {
                        caption = getString(R.string.error_lbl);
                        message = Html.fromHtml(responseVS.getMessage()).toString();
                    }
                    break;
                case CANCEL_VOTE:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        SMIMEMessageWrapper cancelReceipt = responseVS.getSmimeMessage();
                        VoteVS receipt = contextVS.getVoteReceipt(eventId);
                        receipt.setCancelVoteReceipt(cancelReceipt);
                        message = getString(R.string.cancel_vote_result_msg,
                                receipt.getVote().getSubject());
                        if(receipt.getId() > 0) {
                            VoteReceiptDBHelper db = new VoteReceiptDBHelper(
                                    getApplicationContext());
                            try {
                                db.insertVoteReceipt(receipt);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else {
                        caption = getString(R.string.error_lbl);
                        message = responseVS.getMessage();
                    }
                    break;
            }
            sendMessage(responseVS.getStatusCode(), caption, message, serviceCaller, operation);
        } catch(Exception ex) {
            ex.printStackTrace();
            sendMessage(ResponseVS.SC_ERROR,getString(R.string.alert_exception_caption),
                    ex.getMessage(), serviceCaller, operation);
        }
    }

    private ResponseVS processVote(String pin, EventVS eventVS) {
        ResponseVS responseVS = null;
        try {
            FileInputStream fis = openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            VoteSender voteSender = new VoteSender(eventVS, keyStoreBytes, pin.toCharArray(),
                    getApplicationContext());
            responseVS = voteSender.call();
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }


    private ResponseVS processCancellation(String pin, String signatureContent) {
        ResponseVS responseVS = null;
        String subject = getString(R.string.cancel_vote_msg_subject);
        String serviceURL = contextVS.getAccessControl().getCancelVoteServiceURL();
        try {
            FileInputStream fis = openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                    signatureContent, ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                    subject, keyStoreBytes, pin.toCharArray(),
                    contextVS.getAccessControl().getCertificate(), getApplicationContext());
            responseVS = smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }


    private void showNotification(ResponseVS responseVS, String eventSubject, Operation operation){
        String title = null;
        String message = null;
        if(eventSubject != null) {
            if(eventSubject.length() > 50) message = eventSubject.substring(0, 50);
            else message = eventSubject;
        }
        Integer notificationIcon = null;
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            notificationIcon = R.drawable.accept_16;
            if(Operation.VOTE == operation) {
                title = getString(R.string.vote_ok_notification_msg);
            } else  if(Operation.VOTE == operation) {
                title = getString(R.string.cancel_vote_ok_notification_msg);
            }
        } else {
            title = getString(R.string.signature_error_notification_msg);
            notificationIcon = R.drawable.cancel_16;
            if(Operation.VOTE == operation) {
                title = getString(R.string.vote_error_notification_msg);
            } else  if(Operation.VOTE == operation) {
                title = getString(R.string.cancel_vote_error_notification_msg);
            }
        }
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        if(title != null) builder.setContentTitle(title);
        if(message != null) builder.setContentText(message);
        if(notificationIcon != null) builder.setSmallIcon(notificationIcon);
        //Intent clickIntent = new Intent(Intent.ACTION_MAIN);
        //clickIntent.setClassName(this, NavigationDrawer.class.getName());
        //PendingIntent pIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);
        //builder.setContentIntent(pIntent);
        Notification note =  builder.build();
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.VOTE_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(Integer statusCode, String caption, String message,
             String serviceCaller, Operation operation) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - serviceCaller: " +
                serviceCaller + " - operation: " + operation + " - caption: " + caption  +
                " - message: " + message);
        Intent intent = new Intent(serviceCaller);
        if(statusCode != null)
            intent.putExtra(ContextVS.RESPONSE_STATUS_KEY, statusCode.intValue());
        if(caption != null) intent.putExtra(ContextVS.CAPTION_KEY, caption);
        if(message != null) intent.putExtra(ContextVS.MESSAGE_KEY, message);
        intent.putExtra(ContextVS.OPERATION_KEY, operation);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}