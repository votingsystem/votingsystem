package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TicketServer;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketService extends IntentService {

    public static final String TAG = "TicketService";

    public TicketService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String pin = arguments.getString(ContextVS.PIN_KEY);
        switch(operationType) {
            case TICKET_USER_INFO:
                ResponseVS responseVS = updateUserInfo(pin);
                responseVS.setTypeVS(operationType);
                responseVS.setServiceCaller(serviceCaller);
                sendMessage(responseVS);
                break;
        }
    }

    private ResponseVS updateUserInfo(String pin) {
        ResponseVS responseVS = null;
        Map mapToSend = new HashMap();
        mapToSend.put("NIF", contextVS.getUserVS().getNif());
        mapToSend.put("operation", TypeVS.TICKET_USER_INFO.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        String msgSubject = getString(R.string.ticket_user_info_request_msg_subject);
        try {
            JSONObject userInfoRequestJSON = new JSONObject(mapToSend);
            TicketServer ticketServer = contextVS.getTicketServer();
            if(ticketServer == null) {
                responseVS = initTicketServer();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    ticketServer = contextVS.getTicketServer();
                } else return responseVS;
            }
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(contextVS.getUserVS().getNif(),
                    ticketServer.getNameNormalized(), ticketServer.getUserInfoServiceURL(),
                    userInfoRequestJSON.toString(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                    msgSubject, pin.toCharArray(), ticketServer.getCertificate(), contextVS);
            responseVS = smimeSignedSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                TicketAccount ticketAccount = TicketAccount.parse(
                        new JSONObject(responseVS.getMessage()));
                byte[] ticketUserInfoBytes = ObjectUtils.serializeObject(ticketAccount);
                FileOutputStream outputStream;
                try {
                    outputStream = openFileOutput(ContextVS.TICKET_USER_INFO_DATA_FILE_NAME,
                            Context.MODE_PRIVATE);
                    outputStream.write(ticketUserInfoBytes);
                    outputStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseVS = ResponseVS.getExceptionResponse(
                            ex.getMessage(), getString(R.string.exception_lbl));
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(
                    ex.getMessage(), getString(R.string.exception_lbl));
        } finally {
            return responseVS;
        }
    }


    private ResponseVS initTicketServer() {
        ResponseVS responseVS = null;
        TicketServer ticketServer = null;
        try {
            responseVS = HttpHelper.getData(ActorVS.getServerInfoURL(contextVS.getTicketServerURL()),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ticketServer = (TicketServer) ActorVS.parse(new JSONObject(responseVS.getMessage()));
                contextVS.setTicketServer(ticketServer);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(
                    ex.getMessage(), getString(R.string.exception_lbl));
        } finally {
            responseVS.setData(ticketServer);
            return responseVS;
        }
    }

    private void showNotification(ResponseVS responseVS){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                TICKET_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(responseVS.getCaption()).setContentText(
                responseVS.getNotificationMessage()).setSmallIcon(responseVS.getIconId())
                .setContentIntent(pendingIntent);
        Notification note = builder.build();
        // hide the notification after its selected
        note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.REPRESENTATIVE_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(ResponseVS responseVS) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        intent.putExtra(ContextVS.TYPEVS_KEY, responseVS.getTypeVS());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}