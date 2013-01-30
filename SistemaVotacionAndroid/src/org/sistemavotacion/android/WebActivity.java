/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sistemavotacion.android;

import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.util.ServerPaths;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;


public class WebActivity extends Activity {
	
	public static final String TAG = "WebActivity";
	
	private static WebView svWebView;
	private String serverURL = null;
	private Operation operation = null;
    public static WebActivity INSTANCIA; 
    
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.uri_launcher);
    }
    

	private void showMsg(String caption, String msg) {
    	AlertDialog.Builder builder= new AlertDialog.Builder(WebActivity.this);
		builder.setTitle(caption).setMessage(msg)
			.setPositiveButton(R.string.solicitar_label, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	        		Intent intent = new Intent(
	        				WebActivity.this, Aplicacion.class);;
	                startActivity(intent);
	            }
				}).show();
	}
    
    private void initBrowser() {
        JavaScriptInterface jsi = new JavaScriptInterface(this);
        svWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = svWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        svWebView.setClickable(true);
        svWebView.addJavascriptInterface(jsi, "androidClient");
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        svWebView.loadUrl(ServerPaths.getUrlAndroidBrowserSession(serverURL));
        setClienteFirmaMessage("Android client loaded");
        Button aceptarButton = (Button) findViewById(R.id.aceptar_button);
        aceptarButton.setVisibility(View.VISIBLE);
        aceptarButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	setClienteFirmaMessage("Android client loaded");
            }
        });
    }
    
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.d(this.getClass().getName(), "back button pressed");
            if(svWebView.canGoBack()) {
            	svWebView.goBack();
            } else return super.onKeyDown(keyCode, event);
        }
        return true;
    }
    
    public void setClienteFirmaMessage(final String mensaje){
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	Log.d(TAG + ".setClienteFirmaMessage", "mensaje: " + mensaje);
    	    	svWebView.loadUrl("javascript:setClienteFirmaMessage('" + mensaje + "')");
    	    }
    	});
    }
}