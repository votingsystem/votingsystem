package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.callable.PDFSignedSender;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.HttpHelper;

import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignAndSendService extends IntentService {

    public static final String TAG = "SignAndSendService";

    public SignAndSendService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        TypeVS operationType = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        ResponseVS responseVS = null;
        int resultIcon = R.drawable.cancel_22;
        try {
            contextVS = (AppContextVS) getApplicationContext();
            Long eventId = arguments.getLong(ContextVS.ITEM_ID_KEY);
            String pin = arguments.getString(ContextVS.PIN_KEY);
            String signatureContent = arguments.getString(ContextVS.MESSAGE_KEY);
            String messageSubject = arguments.getString(ContextVS.MESSAGE_SUBJECT_KEY);
            String serviceURL = arguments.getString(ContextVS.URL_KEY);
            String toUser = arguments.getString(ContextVS.RECEIVER_KEY);
            if(toUser == null) toUser = contextVS.getAccessControl().
                    getNameNormalized();
            ContentTypeVS contentType = (ContentTypeVS)intent.getSerializableExtra(
                    ContextVS.CONTENT_TYPE_KEY);
            String caption = null;
            byte[] pdfBytes = null;
            String notificationMessage = null;
            Log.d(TAG + ".onHandleIntent(...) ", "operationType: " + operationType +
                    " - contentType: " + contentType);
            switch(operationType) {
                case MANIFEST_PUBLISHING:
                    //Get the PDF to sign
                    responseVS = HttpHelper.sendData(signatureContent.getBytes(), null,
                            serviceURL, "eventId");
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        String manifestId = ((List<String>)responseVS.getData()).iterator().next();
                        pdfBytes = responseVS.getMessageBytes();
                        serviceURL = serviceURL + "/" + manifestId;
                        PDFSignedSender pdfSignedSender = new PDFSignedSender(pdfBytes, serviceURL,
                                pin.toCharArray(), null, null,
                                (AppContextVS)getApplicationContext());
                        responseVS = pdfSignedSender.call();
                    }
                    break;
                case MANIFEST_EVENT:
                    //Get the PDF to sign
                    responseVS = HttpHelper.getData(contextVS.getAccessControl().
                            getEventVSManifestURL(eventId), ContentTypeVS.PDF);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        pdfBytes = responseVS.getMessageBytes();
                        PDFSignedSender pdfSignedSender = new PDFSignedSender(pdfBytes,
                                contextVS.getAccessControl().getEventVSManifestCollectorURL(eventId),
                                pin.toCharArray(), null, null,
                                (AppContextVS)getApplicationContext());
                        responseVS = pdfSignedSender.call();
                    }
                    break;
                case VOTING_PUBLISHING:
                case CLAIM_PUBLISHING:
                case CONTROL_CENTER_ASSOCIATION:
                case SMIME_CLAIM_SIGNATURE:
                case CLAIM_EVENT:
                case REPRESENTATIVE_REVOKE:
                case REPRESENTATIVE_SELECTION:
                case ANONYMOUS_REPRESENTATIVE_SELECTION:
                    SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                            contextVS.getUserVS().getNif(), toUser, serviceURL, signatureContent,
                            contentType, messageSubject, pin.toCharArray(),
                            contextVS.getAccessControl().getCertificate(),
                            (AppContextVS)getApplicationContext());
                    responseVS = smimeSignedSender.call();
                    break;
                default:
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, getString(
                            R.string.operation_unknown_error_msg, operationType.toString()));
                    break;
            }
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                switch(operationType) {
                    case CLAIM_PUBLISHING:
                        notificationMessage = getString(R.string.claim_published_ok_msg);
                        break;
                    case VOTING_PUBLISHING:
                        notificationMessage = getString(R.string.election_published_ok_msg);
                        break;
                    case REPRESENTATIVE_REVOKE:
                        notificationMessage = getString(R.string.representative_revoke_ok_msg);
                        break;
                    default:
                        notificationMessage = getString(R.string.signature_ok_notification_msg);
                }
                caption = getString(R.string.operation_ok_msg);
                resultIcon = R.drawable.signature_ok_32;
            } else {
                caption = getString(R.string.signature_error_notification_msg);
                notificationMessage = responseVS.getMessage();
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    try {
                        JSONObject responseJSON = new JSONObject(responseVS.getNotificationMessage());
                        notificationMessage = responseJSON.getString("message");
                        responseVS.setData(responseJSON.getString("URL"));
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            responseVS.setCaption(caption);
            responseVS.setNotificationMessage(notificationMessage);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex.getMessage(),
                    getString(R.string.exception_lbl));
        } finally {
            responseVS.setIconId(resultIcon);
            responseVS.setTypeVS(operationType);
            responseVS.setServiceCaller(serviceCaller);
            sendMessage(responseVS);
        }
    }

    private void showNotification(ResponseVS responseVS){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(responseVS.getCaption()).setContentText(
                        responseVS.getNotificationMessage()).setSmallIcon(responseVS.getIconId())
                .setContentIntent(pendingIntent);
        Notification note = builder.build();
        // hide the notification after its selected
        note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(ResponseVS responseVS) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + responseVS.getStatusCode() +
                " - serviceCaller: " + responseVS.getServiceCaller() +
                " - caption: " + responseVS.getCaption()  + " - message: " +
                responseVS.getNotificationMessage());
        showNotification(responseVS);
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}