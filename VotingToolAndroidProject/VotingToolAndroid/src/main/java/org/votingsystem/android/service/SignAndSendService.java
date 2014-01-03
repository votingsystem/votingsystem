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

import org.votingsystem.android.R;
import org.votingsystem.android.callable.PDFPublisher;
import org.votingsystem.android.callable.PDFSignedSender;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignAndSendService extends IntentService {

    public static final String TAG = "SignAndSendService";

    public SignAndSendService() { super(TAG); }

    private ContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        try {
            contextVS = ContextVS.getInstance(getApplicationContext());
            Long eventId = arguments.getLong(ContextVS.ITEM_ID_KEY);
            String pin = arguments.getString(ContextVS.PIN_KEY);
            String signatureContent = arguments.getString(ContextVS.MESSAGE_KEY);
            String messageSubject = arguments.getString(ContextVS.MESSAGE_SUBJECT_KEY);
            String serviceURL = arguments.getString(ContextVS.URL_KEY);
            ContentTypeVS contentType = (ContentTypeVS)intent.getSerializableExtra(
                    ContextVS.CONTENT_TYPE_KEY);
            TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.EVENT_TYPE_KEY);
            byte[] keyStoreBytes = null;
            FileInputStream fis = openFileInput(KEY_STORE_FILE);
            keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            ResponseVS responseVS = null;
            String caption = null;
            String message = null;
            String notificationMessage = null;
            Log.d(TAG + ".onHandleIntent(...) ", "typeVS: " + typeVS + " - contentType: " +
                    contentType);
            switch(typeVS) {
                case MANIFEST_PUBLISHING:
                    PDFPublisher publisher = new PDFPublisher(serviceURL, signatureContent,
                            keyStoreBytes, pin.toCharArray(), null, null, getApplicationContext());
                    responseVS = publisher.call();
                    break;
                case MANIFEST_EVENT:
                    PDFSignedSender pdfSignedSender = new PDFSignedSender(
                            contextVS.getAccessControl().getEventVSManifestURL(eventId),
                            contextVS.getAccessControl().getEventVSManifestCollectorURL(eventId),
                            keyStoreBytes, pin.toCharArray(), null, null, getApplicationContext());
                    responseVS = pdfSignedSender.call();
                    break;
                case VOTING_PUBLISHING:
                case CLAIM_PUBLISHING:
                case CONTROL_CENTER_ASSOCIATION:
                case SMIME_CLAIM_SIGNATURE:
                case CLAIM_EVENT:
                    SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                            signatureContent, contentType, messageSubject, keyStoreBytes,
                            pin.toCharArray(), contextVS.getAccessControl().getCertificate(),
                            getApplicationContext());
                    responseVS = smimeSignedSender.call();
                    break;
                default:
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, getString(
                            R.string.operation_unknown_error_msg, typeVS.toString()));
                    break;
            }
            showNotification(responseVS, typeVS);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                caption = getString(R.string.operation_ok_msg);
                message = getString(R.string.operation_ok_msg);
            } else {
                caption = getString(R.string.operation_error_msg);
                message = responseVS.getMessage();
            }
            sendMessage(responseVS.getStatusCode(), caption, message,serviceCaller);
        } catch(Exception ex) {
            ex.printStackTrace();
            sendMessage(ResponseVS.SC_ERROR,getString(R.string.alert_exception_caption),
                    ex.getMessage(), serviceCaller);
        }
    }

    private void showNotification(ResponseVS responseVS, TypeVS typeVS){
        String title = null;
        String message = null;
        if(ResponseVS.SC_OK == responseVS.getStatusCode())
            title = getString(R.string.signature_ok_notification_msg);
        else title = getString(R.string.signature_error_notification_msg);
        message = typeVS.toString();
        // Display an icon to show that the service is running.
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(Intent.ACTION_MAIN);
        //clickIntent.setClassName(this, NavigationDrawer.class.getName());
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle(title).setContentText(message)
                .setContentIntent(pIntent).setSmallIcon(R.drawable.application_certificate_16)
                .build();
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(Integer statusCode, String caption, String message,
             String serviceCaller) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + statusCode + " - serviceCaller: " +
                serviceCaller +" - caption: " + caption  + " - message: " + message);
        Intent intent = new Intent(serviceCaller);
        if(statusCode != null)
            intent.putExtra(ContextVS.RESPONSE_STATUS_KEY, statusCode.intValue());
        if(caption != null) intent.putExtra(ContextVS.CAPTION_KEY, caption);
        if(message != null) intent.putExtra(ContextVS.MESSAGE_KEY, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}