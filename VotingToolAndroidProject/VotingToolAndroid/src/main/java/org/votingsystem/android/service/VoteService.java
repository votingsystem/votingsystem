package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.VoteSender;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.votingsystem.android.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteService extends IntentService {

    public static final String TAG = VoteService.class.getSimpleName();

    public VoteService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        VoteVS vote = (VoteVS) intent.getSerializableExtra(ContextVS.VOTE_KEY);
        ResponseVS responseVS = null;
        String caption = null;
        String message = null;
        try {
            contextVS = (AppContextVS) getApplicationContext();
            String receiverName = arguments.getString(ContextVS.RECEIVER_KEY);
            LOGD(TAG + ".onHandleIntent", "operation: " + operation + " - receiverName: " +
                    receiverName + " - event: " + vote.getEventVS().getId());
            if(receiverName == null) receiverName = contextVS.getAccessControl().getNameNormalized();
            ControlCenterVS controlCenter = vote.getEventVS().getControlCenter();
            X509Certificate controlCenterCert = contextVS.getCert(controlCenter.getServerURL());
            if(controlCenterCert == null) {
                responseVS = HttpHelper.getData(controlCenter.getCertChainURL(), null);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    caption = getString(R.string.operation_error_msg);
                    message = getString(R.string.get_cert_error_msg, controlCenter.getName());
                } else {
                    Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(
                            responseVS.getMessageBytes());
                    controlCenterCert = certChain.iterator().next();
                    contextVS.putCert(controlCenter.getServerURL(), controlCenterCert);
                }
            }
            if(controlCenterCert != null) {
                controlCenter.setCertificate(controlCenterCert);
                contextVS.setControlCenter(controlCenter);
                switch(operation) {
                    case VOTEVS:
                        responseVS = processVote(vote);
                        break;
                    case CANCEL_VOTE:
                        JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
                        responseVS = processCancellation(receiverName, cancelDataJSON.toString());
                        break;
                }
                showNotification(responseVS, vote.getEventVS().getSubject(), operation, vote);
                switch(operation) {
                    case VOTEVS:
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            VoteVS voteReceipt = (VoteVS)responseVS.getData();
                            message = getString(R.string.vote_ok_msg, vote.getEventVS().getSubject(),
                                    vote.getOptionSelected().getContent());
                        } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                            caption = getString(R.string.access_request_repeated_caption);
                            message = getString( R.string.access_request_repeated_msg,
                                    vote.getEventVS().getSubject(), responseVS.getMessage());
                        } else {
                            caption = getString(R.string.error_lbl);
                            message = Html.fromHtml(responseVS.getMessage()).toString();
                        }
                        break;
                    case CANCEL_VOTE:
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            SMIMEMessage cancelReceipt = responseVS.getSMIME();
                            vote.setCancelVoteReceipt(cancelReceipt);
                            message = getString(R.string.cancel_vote_result_msg,
                                    vote.getEventVS().getSubject());
                            if(vote.getLocalId() > 0) {//Update local receipt database
                                ContentValues values = new ContentValues();
                                vote.setTypeVS(TypeVS.VOTEVS_CANCELLED);
                                values.put(ReceiptContentProvider.URL_COL, cancelReceipt.getMessageID());
                                values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                                        ObjectUtils.serializeObject(vote));
                                values.put(ReceiptContentProvider.TYPE_COL, vote.getTypeVS().toString());
                                values.put(ReceiptContentProvider.TIMESTAMP_UPDATED_COL,
                                        System.currentTimeMillis());
                                getContentResolver().update(ReceiptContentProvider.getReceiptURI(
                                        vote.getLocalId()), values, null, null);
                            }
                        } else {
                            caption = getString(R.string.error_lbl);
                            message = responseVS.getMessage();
                        }
                        break;
                }
            }
            responseVS.setCaption(caption).setNotificationMessage(message).setData(vote);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            responseVS.setTypeVS(operation).setServiceCaller(serviceCaller);
            sendMessage(responseVS);
        }
    }

    private ResponseVS processVote(VoteVS vote) {
        ResponseVS responseVS = null;
        try {
            VoteSender voteSender = new VoteSender(vote, (AppContextVS)getApplicationContext());
            responseVS = voteSender.call();
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, contextVS);
        }
        return responseVS;
    }


    private ResponseVS processCancellation(String toUser, String signatureContent) {
        ResponseVS responseVS = null;
        String subject = getString(R.string.cancel_vote_msg_subject);
        String serviceURL = contextVS.getAccessControl().getCancelVoteServiceURL();
        try {
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                    contextVS.getUserVS().getNif(), toUser, serviceURL, signatureContent,
                    ContentTypeVS.JSON_SIGNED, subject,
                    contextVS.getAccessControl().getCertificate(),
                    (AppContextVS)getApplicationContext());
            responseVS = smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally { return responseVS; }
    }

    private void showNotification(ResponseVS responseVS, String eventSubject, TypeVS operation,
            VoteVS vote){
        String title = null;
        String message = null;
        if(eventSubject != null) {
            if(eventSubject.length() > 50) message = eventSubject.substring(0, 50);
            else message = eventSubject;
        }
        Integer notificationIcon = null;
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            notificationIcon = R.drawable.accept_16;
            if(TypeVS.VOTEVS == operation) {
                title = getString(R.string.vote_ok_notification_msg);
            } else  if(TypeVS.CANCEL_VOTE == operation) {
                title = getString(R.string.cancel_vote_ok_notification_msg);
            }
        } else {
            title = getString(R.string.signature_error_notification_msg);
            notificationIcon = R.drawable.cancel_16;
            if(TypeVS.VOTEVS == operation) {
                title = getString(R.string.vote_error_notification_msg);
            } else  if(TypeVS.CANCEL_VOTE == operation) {
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
        //clickIntent.setClassName(this, EventsVSActivity.class.getName());
        //PendingIntent pIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);
        //builder.setContentIntent(pIntent);
        Notification note =  builder.build();
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.VOTE_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(ResponseVS responseVS) {
        LOGD(TAG + ".sendMessage", "statusCode: " + responseVS.getStatusCode() +
                " - serviceCaller: " + responseVS.getServiceCaller() + " - operation: " +
                responseVS.getTypeVS() + " - caption: " + responseVS.getCaption() +
                " - message: " + responseVS.getNotificationMessage());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        if(responseVS.getData() != null) intent.putExtra(ContextVS.VOTE_KEY,
                (Serializable) responseVS.getData());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}