package org.votingsystem.android.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.WebSocketRequest;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DeviceUtils;
import org.votingsystem.util.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignerActivity extends FragmentActivity {
	
	public static final String TAG = SMIMESignerActivity.class.getSimpleName();

    private TypeVS operationType;
    private AppContextVS contextVS = null;
    private String broadCastId = SMIMESignerActivity.class.getSimpleName();
    private WebView webView;
    private WebSocketRequest request;
    private OperationVS operationVS;
    private ProgressDialog progressDialog = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver",
                "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        WebSocketRequest request = intent.getParcelableExtra(ContextVS.WEBSOCKET_REQUEST_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) launchService(null, operationVS);
        else {
            if(progressDialog != null) progressDialog.dismiss();
            if(request != null) {
                if(TypeVS.MESSAGEVS_FROM_DEVICE == request.getTypeVS()) {
                    if(ResponseVS.SC_OK == request.getStatusCode()) {
                        UIUtils.launchMessageActivity(SMIMESignerActivity.this,
                                request.getNotificationResponse(SMIMESignerActivity.this));
                        SMIMESignerActivity.this.finish();
                    } else showMessage(request.getStatusCode(),
                            getString(R.string.sign_document_lbl), request.getMessage());
                }
            }
        }
    }};

    private void launchService(ResponseVS responseVS, OperationVS operationVS) {
        Log.d(TAG + ".launchService() ", "launchService");
        Intent startIntent = new Intent(this, WebSocketService.class);
        startIntent.putExtra(ContextVS.OPERATIONVS_KEY, operationVS);
        startIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        if(operationVS != null) showProgressDialog(
                getString(R.string.wait_caption), getString(R.string.signing_document_lbl));
        startService(startIntent);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.smime_signer);
        contextVS = (AppContextVS) getApplicationContext();
        request =  getIntent().getParcelableExtra(ContextVS.WEBSOCKET_REQUEST_KEY);
        operationVS = request.getOperationVS();
        webView = (WebView) findViewById(R.id.smime_signed_content);
        try {
            JSONObject documentToSign = new JSONObject(request.getOperationVS().getTextToSign());
            webView.loadData(documentToSign.toString(3), "application/json", "UTF-8");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TextView textView = (TextView) findViewById(R.id.deviceName);
        textView.setText(getString(R.string.signature_request_from_device,
                request.getOperationVS().getDeviceFromName()));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getString(R.string.sign_request_lbl));
        if(savedInstanceState != null) {
            operationType = (TypeVS) savedInstanceState.getSerializable(ContextVS.TYPEVS_KEY);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.smime_signer, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.sign_document:
                PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                        getString(R.string.ping_to_sign_msg), false, TypeVS.MESSAGEVS_SIGN);
                return true;
            case android.R.id.home:
            case R.id.reject_sign_request:
                try {
                    ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
                    responseVS.setMessageJSON(request.getResponse(operationVS.getTypeVS(),
                            ResponseVS.SC_ERROR, getString(R.string.reject_websocket_request_msg,
                            DeviceUtils.getDeviceName()), this));
                    launchService(responseVS, null);
                    this.finish();
                } catch(Exception ex) {ex.printStackTrace();}
                return true;
            case R.id.ban_device:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendMessageToWebSocketService(TypeVS messageTypeVS, String message) {
        Log.d(TAG + ".sendMessageToWebSocketService(...)", "messageTypeVS: " + messageTypeVS.toString());
        Intent startIntent = new Intent(contextVS, WebSocketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, messageTypeVS);
        startIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        startService(startIntent);
    }

    public void showMessage(int statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    private void showProgressDialog(final String title, final String dialogMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(SMIMESignerActivity.this);
                    progressDialog.setCancelable(true);
                    progressDialog.setTitle(title);
                    progressDialog.setMessage(dialogMessage);
                    progressDialog.setIndeterminate(true);
                    /*progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
                    @Override public void onCancel(DialogInterface dialog){}});*/
                }
                progressDialog.show();
            }
        });
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.TYPEVS_KEY, operationType);
    }

}