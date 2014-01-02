package org.votingsystem.android.service;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.AppWithoutCertActivity;
import org.votingsystem.android.activity.EventVSPublishingActivity;
import org.votingsystem.android.activity.MainActivity;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.activity.UserCertRequestActivity;
import org.votingsystem.android.activity.UserCertResponseActivity;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;
import java.util.UUID;
import static org.votingsystem.model.ContextVS.APPLICATION_ID_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VotingAppService extends Service {

    public static final String TAG = "VotingAppService";

    private ContextVS contextVS;

    @Override public void onCreate(){
        contextVS = ContextVS.getInstance(getBaseContext());
        Log.i(TAG + ".onCreate(...) ", "VotingAppService created");
    }

    private void processOperation (String accessControlURL, String operationStr) {
        ResponseVS responseVS = HttpHelper.getData(AccessControlVS.
                getServerInfoURL(accessControlURL), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            intent.putExtra(ContextVS.CAPTION_KEY, getString(R.string.connection_error_msg));
            intent.putExtra(ContextVS.MESSAGE_KEY, responseVS.getMessage());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            try {
                AccessControlVS accessControl = AccessControlVS.parse(
                        responseVS.getMessage());
                contextVS.setAccessControlVS(accessControl);
                if(operationStr != null  &&
                        ContextVS.State.WITH_CERTIFICATE == contextVS.getState()){
                    OperationVS operationVS = OperationVS.parse(operationStr);
                    Log.d(TAG + ".onStartCommand(...)", "operationVS: " + operationVS.getTypeVS());
                    if(operationVS.getEventVS() != null) {
                        //We don't pass all eventvs data on uri because content can be very large
                        responseVS = HttpHelper.getData(operationVS.getEventVS().getURL(), null);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            EventVS selectedEvent = EventVS.parse(responseVS.getMessage());
                            selectedEvent.setOptionSelected(operationVS.
                                    getEventVS().getOptionSelected());
                            operationVS.setEventVS(selectedEvent);
                            processOperation(operationVS);
                        }
                    } else {
                        if(operationStr != null) {
                            Intent operIntent = new Intent(VotingAppService.this,
                                    EventVSPublishingActivity.class);
                            operIntent.putExtra(OperationVS.OPERATION_KEY, operationStr);
                            startActivity(operIntent);
                        }
                    }
                } else {
                    Intent intent = null;
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    switch (contextVS.getState()) {
                        case WITHOUT_CSR:
                            String applicationID = settings.getString(APPLICATION_ID_KEY, null);
                            if (applicationID == null || applicationID.isEmpty()) {
                                Log.d(TAG + ".setActivityState() ", "applicationID: " + applicationID);
                                applicationID = UUID.randomUUID().toString();
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(APPLICATION_ID_KEY, applicationID);
                                editor.commit();
                            }
                            intent = new Intent(getBaseContext(), AppWithoutCertActivity.class);
                            break;
                        case WITH_CSR:
                            intent = new Intent(getBaseContext(), UserCertResponseActivity.class);
                            break;
                        case WITH_CERTIFICATE:
                            intent = new Intent(getBaseContext(), NavigationDrawer.class);
                            break;
                    }
                    if(intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void processOperation(OperationVS operationVS) {
        Log.d(TAG + ".processOperation(...)", "====_ TODO _==== operationVS: " +
                operationVS.getTypeVS() + " - state: " + contextVS.getState());
        switch(operationVS.getTypeVS()) {
            case SEND_SMIME_VOTE:
                break;
            case MANIFEST_SIGN:
            case SMIME_CLAIM_SIGNATURE:
                break;
            default:
                Log.e(TAG + ".processOperation(...)", "unknown operationVS");;
        }
    }

    @Override public void onDestroy(){ }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG + ".onStartCommand(...) ", "");
        super.onStartCommand(intent, flags, startId);
        Bundle arguments = intent.getExtras();
        if(arguments != null) {
            final String accessControlURL = arguments.getString(ContextVS.ACCESS_CONTROL_URL_KEY);
            final String operationStr = arguments.getString(ContextVS.URI_DATA_KEY);
            Runnable runnable = new Runnable() {
                @Override public void run() {
                    processOperation(accessControlURL, operationStr);
                }
            };
            /*Services run in the main thread of their hosting process. This means that, if
            * it's going to do any CPU intensive (such as networking) operations, it should
            * spawn its own thread in which to do that work.*/
            Thread thr = new Thread(null, runnable, "voting_app_service_thread");
            thr.start();
        }
        //We want this service to continue running until it is explicitly stopped, so return sticky.
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

}
