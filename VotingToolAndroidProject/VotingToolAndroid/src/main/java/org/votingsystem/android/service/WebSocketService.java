package org.votingsystem.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class WebSocketService extends Service {

    public static final String TAG = "WebSocketService";

    private NotificationManager notificationManager;

    @Override public void onCreate(){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle(ContextVS.getMessage("signAndSendNotificationMsg"))
                .setSmallIcon(R.drawable.manifest_22)
                .build();
        notificationManager.notify(ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, note);
        Log.i(TAG + ".onCreate(...) ", "SignAndSendService created");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Bundle arguments = intent.getExtras();
        if(arguments != null) {
            if(arguments.containsKey(ContextVS.PIN_KEY)) {
                signAndSendDocument(arguments.getString(ContextVS.PIN_KEY));
            }
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void signAndSendDocument(String pin) {

    }

    // When the service is destroyed, get rid of our persistent icon.
    @Override public void onDestroy(){
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);
    }

    @Override public IBinder onBind(Intent intent) {
        Log.d(TAG + ".onBind(...)", "onBind");
        return mBinder;
    }

    private final IBinder mBinder = new Binder()  {
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
        return super.onTransact(code, data, reply, flags);
        }
    };
}
