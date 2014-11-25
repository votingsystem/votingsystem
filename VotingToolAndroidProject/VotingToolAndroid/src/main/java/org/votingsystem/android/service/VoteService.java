package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.VoteSender;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ArgVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.util.ArrayList;
import java.util.List;

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
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        List<ArgVS> argVSList = new ArrayList<ArgVS>();
        VoteVS vote = (VoteVS) intent.getSerializableExtra(ContextVS.VOTE_KEY);
        ResponseVS responseVS = null;
        String eventSubject = vote.getEventVS().getSubject();
        if(eventSubject.length() > 50) eventSubject = eventSubject.substring(0, 50) + "...";
        try {
            LOGD(TAG + ".onHandleIntent", "operation: " + operation +
                    " - event: " + vote.getEventVS().getId());
            if(contextVS.getControlCenter() == null) {
                ControlCenterVS controlCenter = (ControlCenterVS) contextVS.getActorVS(vote.getEventVS().
                        getControlCenter().getServerURL());
                contextVS.setControlCenter(controlCenter);
            }
            switch(operation) {
                case VOTING_PUBLISHING:
                    String textToSign = arguments.getString(ContextVS.MESSAGE_KEY);
                    responseVS = contextVS.signMessage(contextVS.getAccessControl().getNameNormalized(),
                            textToSign, getString(R.string.publish_election_msg_subject));
                    responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                            ContentTypeVS.JSON_SIGNED, contextVS.getAccessControl().
                            getPublishServiceURL());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) responseVS.
                            setNotificationMessage(getString(R.string.election_published_ok_msg));
                    break;
                case VOTEVS:
                    VoteSender voteSender = new VoteSender(vote, contextVS);
                    responseVS = voteSender.call();
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        VoteVS voteReceipt = (VoteVS)responseVS.getData();
                        responseVS.setCaption(getString(R.string.vote_ok_caption)).
                                setNotificationMessage(getString(
                                R.string.vote_ok_msg, eventSubject,
                                vote.getOptionSelected().getContent()));
                    } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                        responseVS.setUrl(responseVS.getMessage());
                        responseVS.setCaption(getString(R.string.access_request_repeated_caption)).
                                setNotificationMessage(getString( R.string.access_request_repeated_msg,
                                eventSubject));
                    } else {
                        responseVS.setCaption(getString(R.string.vote_error_caption)).
                                setNotificationMessage(
                                Html.fromHtml(responseVS.getMessage()).toString());
                    }
                    break;
                case CANCEL_VOTE:
                    JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
                    responseVS = contextVS.signMessage(contextVS.getAccessControl().getNameNormalized(),
                            cancelDataJSON.toString(), getString(R.string.cancel_vote_msg_subject));
                    responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                            ContentTypeVS.JSON_SIGNED,
                            contextVS.getAccessControl().getCancelVoteServiceURL());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        SMIMEMessage cancelReceipt = responseVS.getSMIME();
                        vote.setCancelVoteReceipt(cancelReceipt);
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
                        responseVS.setCaption(getString(R.string.cancel_vote_ok_caption)).
                                setNotificationMessage(getString(R.string.cancel_vote_result_msg,
                                eventSubject));
                    } else {
                        responseVS.setCaption(getString(R.string.cancel_vote_error_caption)).
                                setNotificationMessage(responseVS.getMessage());
                    }
                    break;
            }
            argVSList.add(new ArgVS(ContextVS.VOTE_KEY, vote));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            responseVS.setTypeVS(operation).setServiceCaller(serviceCaller);
            contextVS.broadcastResponse(responseVS, argVSList.toArray(new ArgVS[argVSList.size()]));
        }
    }

    private void showNotification(ResponseVS responseVS){
        Integer notificationIcon = null;
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) notificationIcon = R.drawable.accept_16;
        else notificationIcon = R.drawable.cancel_16;
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(responseVS.getCaption()).setContentText(
                responseVS.getNotificationMessage());
        if(notificationIcon != null) builder.setSmallIcon(notificationIcon);
        /*Intent clickIntent = new Intent(Intent.ACTION_MAIN);
        clickIntent.setClassName(this, EventsVSActivity.class.getName());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                VOTE_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(title).setContentText(message).setSmallIcon(resultIcon)
                .setContentIntent(pendingIntent);
        builder.setContentIntent(pendingIntent);*/
        Notification note =  builder.build();
        //note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.VOTE_SERVICE_NOTIFICATION_ID, note);
    }

}