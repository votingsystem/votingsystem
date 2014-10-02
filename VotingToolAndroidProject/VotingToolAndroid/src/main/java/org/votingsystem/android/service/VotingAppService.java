package org.votingsystem.android.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.OperationVS;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.URI_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VotingAppService extends Service implements Runnable {

    public static final String TAG = VotingAppService.class.getSimpleName();

    private AppContextVS contextVS;
    private Handler handler;
    private static final int UPDATE_FREQUENCY_IN_MINUTES = 60;

    @Override public void onCreate(){
        contextVS = (AppContextVS) getApplicationContext();
        handler = new Handler();
        Log.i(TAG + ".onCreate(...) ", "VotingAppService created");
    }

    @Override public void onDestroy(){ }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        LOGD(TAG + ".onStartCommand(...) ", "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(intent != null) {
            Bundle arguments = intent.getExtras();
            Uri uriData = (Uri) arguments.getParcelable(URI_KEY);
            OperationVS operationVS = null;
            final OperationVS operationFinal = null;
            Runnable runnable = new Runnable() {
                @Override public void run() {
                   // backgroundProcess(operationFinal);
                }
            };
            /*Services run in the main thread of their hosting process. This means that, if
            * it's going to do any CPU intensive (such as networking) operations, it should
            * spawn its own thread in which to do that work.*/
            Thread thr = new Thread(null, runnable, "voting_app_service_thread");
            thr.start();
            //We want this service to continue running until it is explicitly stopped, so return sticky.
        } else LOGD(TAG + ".onStartCommand(...) ", "onStartCommand - intent null");
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent){
        return mBinder;
    }

    private final IBinder mBinder = new Binder()  {
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

    private void checkForPendingOperations() {
        Log.d(TAG + ".checkForPendingOperations()", "");
        GregorianCalendar nextCheckTime = new GregorianCalendar();
        Calendar lastCheckedTime = PrefUtils.getLastPendingOperationCheckedTime(contextVS);
        nextCheckTime = (GregorianCalendar) lastCheckedTime.clone();
        nextCheckTime.add(GregorianCalendar.MINUTE, UPDATE_FREQUENCY_IN_MINUTES);
        Log.d(TAG + ".checkForPendingOperations() ", "last checked time:" +
                lastCheckedTime.toString() + " - Next checked time: " + nextCheckTime.toString());

        if(lastCheckedTime.before(nextCheckTime)) runPendingOperations();
        else handler.postDelayed(this, 1000 * 60 * UPDATE_FREQUENCY_IN_MINUTES);
    }

    private void runPendingOperations() {
        Log.d(TAG + ".checkForPendingOperations(...) ", "");
    }

    @Override public void run() {
        checkForPendingOperations();
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

}