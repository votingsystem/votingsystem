package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CooinServer;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BootStrapService extends IntentService {

    public static final String TAG = BootStrapService.class.getSimpleName();

    private AppContextVS contextVS;
    private String serviceCaller;
    private Handler mHandler;

    public BootStrapService() {
        super(TAG);
        mHandler = new Handler();
    }

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        final String accessControlURL = arguments.getString(ContextVS.ACCESS_CONTROL_URL_KEY);
        final String cooinServerURL = arguments.getString(ContextVS.COOIN_SERVER_URL);
        LOGD(TAG + ".onHandleIntent", "accessControlURL: " + accessControlURL +
                " - cooinServerURL: " + cooinServerURL);
        ResponseVS responseVS = null;
        if(contextVS.getAccessControl() == null) {
            responseVS = HttpHelper.getData(AccessControlVS.getServerInfoURL(accessControlURL),
                    ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    AccessControlVS accessControl = AccessControlVS.parse(responseVS.getMessage());
                    contextVS.setAccessControlVS(accessControl);
                } catch(Exception ex) {ex.printStackTrace();}
            } else {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(contextVS, contextVS.getString(R.string.server_connection_error_msg,
                                accessControlURL), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        if(contextVS.getCooinServer() == null) {
            responseVS = HttpHelper.getData(ActorVS.getServerInfoURL(cooinServerURL),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    CooinServer cooinServer = (CooinServer) ActorVS.parse(new JSONObject(responseVS.getMessage()));
                    contextVS.setCooinServer(cooinServer);
                } catch(Exception ex) {ex.printStackTrace();}
            } else {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(contextVS, contextVS.getString(R.string.server_connection_error_msg,
                                cooinServerURL), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        if(!PrefUtils.isDataBootstrapDone(this)) {
            PrefUtils.markDataBootstrapDone(this);
            /*if(contextVS.getCooinServer() == null && contextVS.getAccessControl() == null) {
                intent = new Intent(getBaseContext(), IntentFilterActivity.class);
                responseVS.setCaption(getString(R.string.connection_error_msg));
                if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode())
                    responseVS.setNotificationMessage(getString(R.string.conn_timeout_msg));
                intent.putExtra(RESPONSEVS_KEY, responseVS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }*/
        }
        if(responseVS == null) responseVS = new ResponseVS();
        responseVS.setServiceCaller(serviceCaller);
        contextVS.broadcastResponse(responseVS);
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}