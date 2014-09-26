package org.votingsystem.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSActivity extends ActivityVS {
	
	public static final String TAG = BrowserVSActivity.class.getSimpleName();

    private String viewerURL = null;
    private TypeVS operationType;
    private AppContextVS contextVS = null;
    private String broadCastId = BrowserVSActivity.class.getSimpleName();
    private WebView webView;
    private OperationVS operationVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                "intent.getExtras(): " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            if(TypeVS.MESSAGEVS_DECRYPT == typeVS) {
                decryptMessageVS(operationVS);
            } else launchSignAndSendService();
        }
        else {
            if(responseVS != null && TypeVS.MESSAGEVS_GET == responseVS.getTypeVS()) {
                String jsCommand = "javascript:updateMessageVSList('" +
                        responseVS.getMessageJSON().toString() + "')";
                runOnUiThread(new Runnable() { @Override public void run() { showProgress(false, true); } });
                webView.loadUrl(jsCommand);
            } else if(responseVS.getOperation() != null) {
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    sendMessageToBrowserApp(responseVS.getMessageJSON(),
                            responseVS.getOperation().getCallerCallback());
                } else {
                    sendMessageToBrowserApp(responseVS.getStatusCode(),
                            responseVS.getNotificationMessage(), responseVS.getOperation().getCallerCallback());
                }
            } else showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                    responseVS.getNotificationMessage());
        }
        }
    };

    private void launchSignAndSendService() {
        Log.d(TAG + ".launchUserCertRequestService() ", "launchSignAndSendService");
        try {
            Intent startIntent = new Intent(this, SignAndSendService.class);
            startIntent.putExtra(ContextVS.OPERATIONVS_KEY, operationVS);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            showProgress(true, true);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        broadCastId = BrowserVSActivity.class.getSimpleName();
    	super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        viewerURL = getIntent().getStringExtra(ContextVS.URL_KEY);
        setContentView(R.layout.browservs);
        initActivityVS((FrameLayout)findViewById(R.id.mainLayout), findViewById(R.id.progressContainer));
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setTitle(getString(R.string.browswevs_lbl));
        getSupportActionBar().hide();
        if(savedInstanceState != null) {
            operationType = (TypeVS) savedInstanceState.getSerializable(ContextVS.TYPEVS_KEY);
        }
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        loadUrl(viewerURL, getIntent().getStringExtra(ContextVS.JS_COMMAND_KEY));
    }

    private void loadUrl(String viewerURL, final String jsCommand) {
        Log.d(TAG + ".viewerURL(...)", " - viewerURL: " + viewerURL);
        webView = (WebView) findViewById(R.id.browservs_content);
        WebSettings webSettings = webView.getSettings();
        showProgress(true, true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webView.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "clientTool");
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if(jsCommand != null) webView.loadUrl(jsCommand);
                showProgress(false, true);
            }
        });
        webView.loadUrl(viewerURL);
    }

    @JavascriptInterface public void setJSONMessageToSignatureClient (String appMessage) {
        Log.d(TAG + ".setJSONMessageToSignatureClient(...) ", "appMessage: " + appMessage);
        try {
            operationVS = OperationVS.parse(appMessage);
            switch(operationVS.getTypeVS()) {
                case MESSAGEVS_GET:
                    sendMessageToWebSocketService(operationVS.getTypeVS(), operationVS.getDocument().toString());
                    break;
                case MESSAGEVS_DECRYPT:
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                            getString(R.string.enter_pin_to_decrypt_msg), false, TypeVS.MESSAGEVS_DECRYPT);
                    break;
                default:
                    processSignatureOperation(operationVS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void decryptMessageVS(OperationVS operationVS)  {
        Log.d(TAG + ".decryptMessageVS(...) ", "decryptMessageVS");
        ResponseVS responseVS = null;
        try {
            responseVS = contextVS.decryptMessageVS(operationVS.getDocumentToDecrypt());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                sendMessageToWebSocketService(TypeVS.WEB_SOCKET_MESSAGE,
                        ((JSONObject)responseVS.getData()).toString());
                sendMessageToBrowserApp(responseVS.getMessageJSON(), operationVS.getCallerCallback());
            } else Log.e(TAG + ".decryptMessageVS(...) ", "ERROR decrypting message");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendMessageToWebSocketService(TypeVS messageTypeVS, String message) {
        Log.d(TAG + ".sendMessageToWebSocketService(...)", "messageTypeVS: " + messageTypeVS.toString());
        Intent startIntent = new Intent(contextVS, WebSocketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, messageTypeVS);
        startIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        runOnUiThread(new Runnable() { @Override public void run() { showProgress(true, true); } });
        startService(startIntent);
    }

    @Override public void onBackPressed() {
        String webUrl = webView.getUrl();
        Log.d(TAG + ".onBackPressed(...) ", "webUrl: " + webUrl);
        if (webView.isFocused() && webView.canGoBack() && webUrl.contains("mode=details")) {
            webView.goBack();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    private void processSignatureOperation(OperationVS operationVS) {
        Log.d(TAG + ".processSignatureOperation(...) ", "processSignatureOperation");
        PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId,
                getString(R.string.enter_pin_signature_device_msg), false, null);
    }

    private void sendResult(int result, String message) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        setResult(result, resultIntent);
        finish();
    }

    public void sendMessageToBrowserApp(int statusCode, String message, String callbackFunction) {
        Log.d(TAG + ".sendMessageToBrowserApp(...) ", "statusCode: " + statusCode + " - message: " +
                message + " - callbackFunction: " + callbackFunction);
        Map resultMap = new HashMap();
        resultMap.put("statusCode", statusCode);
        resultMap.put("message", message);
        JSONObject messageJSON = new JSONObject(resultMap);
        String jsCommand = "javascript:" + callbackFunction + "(" + messageJSON.toString() + ")";
        webView.loadUrl(jsCommand);
        showProgress(false, true);
    }


    public void sendMessageToBrowserApp(JSONObject messageJSON, String callbackFunction) {
        Log.d(TAG + ".sendMessageToBrowserApp(...) ", "statusCode: " + messageJSON.toString() +
                " - callbackFunction: " + callbackFunction);
        String jsCommand = "javascript:" + callbackFunction + "(" + messageJSON.toString() + ")";
        webView.loadUrl(jsCommand);
        showProgress(false, true);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        outState.putString(ContextVS.URL_KEY, viewerURL);
        outState.putSerializable(ContextVS.TYPEVS_KEY, operationType);
    }

}