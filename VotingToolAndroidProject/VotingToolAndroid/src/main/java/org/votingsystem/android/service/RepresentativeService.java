package org.votingsystem.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.android.contentprovider.RssContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSResponse;
import org.votingsystem.util.HttpHelper;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeService extends Service {

    public static final String TAG = "RepresentativeService";

    private AtomicBoolean httpDataInitialized = new AtomicBoolean(false);
    private static ContextVS contextVS = null;

    @Override public void onCreate(){
        contextVS = contextVS.getInstance(this);
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle(contextVS.getMessage("loadingRepresentativeDataMsg"))
                .setSmallIcon(R.drawable.manifest_22).build();
        notificationManager.notify(ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID, note);
        Log.i(TAG + ".onCreate(...) ", "RepresentativeService created");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG + ".onStartCommand(...) ", "Service initialized: " + httpDataInitialized.get());
        super.onStartCommand(intent, flags, startId);
        final Bundle arguments = intent.getExtras();
        if(arguments != null && arguments.containsKey(ContextVS.URL_KEY)) {
            if(!httpDataInitialized.get()) {
                /*Services run in the main thread of their hosting process. This means that, if
                * it's going to do any CPU intensive (such as networking) operations, it should
                * spawn its own thread in which to do that work.*/
                Runnable runnable = new Runnable() {
                    @Override public void run() {
                        loadURLData(arguments.getString(ContextVS.URL_KEY));
                    }
                };
                Thread thr = new Thread(null, runnable, "representative_service_thread");
                thr.start();
            }
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    private void loadURLData(String url) {
        Log.d(TAG + ".loadURLData(...) ", "url: " + url);
        ResponseVS responseVS = HttpHelper.getData(url, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            try {
                UserVSResponse response = UserVSResponse.parse(responseVS.getMessage());
                for(UserVS representative : response.getUsers()) {
                    ContentValues values = new ContentValues(5);
                    values.put(RepresentativeContentProvider.ID_COL, representative.getId());
                    values.put(RepresentativeContentProvider.URL_COL, representative.getURL());
                    values.put(RepresentativeContentProvider.FULL_NAME_COL, representative.getFullName());
                    values.put(RepresentativeContentProvider.NIF_COL, representative.getNif());
                    values.put(RepresentativeContentProvider.NUM_REPRESENTATIONS_COL, representative.getNumRepresentations());
                    Uri uri = getContentResolver().insert(
                            RepresentativeContentProvider.CONTENT_URI, values);
                    Log.d(TAG + ".loadURLData(...)", "inserted representative: " + uri.toString());
                }
                Intent intent = new Intent(ContextVS.HTTP_DATA_INITIALIZED_ACTION_ID);
                intent.putExtra(ContextVS.HTTP_RESPONSE_KEY, responseVS.getStatusCode());
                LocalBroadcastManager.getInstance(RepresentativeService.this).sendBroadcast(intent);
                httpDataInitialized.set(true);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
