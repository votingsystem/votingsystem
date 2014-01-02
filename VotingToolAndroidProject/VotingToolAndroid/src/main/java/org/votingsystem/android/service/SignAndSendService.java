package org.votingsystem.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Bundle;
import android.database.Cursor;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.model.ContextVS;

import java.io.BufferedReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.StringBuilder;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignAndSendService extends Service {

    public static final String TAG = "SignAndSendService";

    private static ContextVS contextVS = null;

    @Override public void onCreate(){
        contextVS = contextVS.getInstance(this);
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle("SignAndSendService created")
                .setSmallIcon(R.drawable.manifest_22).build();
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
