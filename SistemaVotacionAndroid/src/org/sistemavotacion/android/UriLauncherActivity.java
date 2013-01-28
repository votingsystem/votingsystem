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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.sistemavotacion.json.DeJSONAObjeto;
import org.sistemavotacion.modelo.Consulta;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.util.ServerPaths;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;


public class UriLauncherActivity extends Activity {
	
	public static final String TAG = "UriLauncherActivity";
	
	private static WebView svWebView;
	private String serverURL = null;
	private Operation operation = null;
    public static UriLauncherActivity INSTANCIA; 
    
    DataListener<String> eventDataListener = new DataListener<String>() {

		@Override public void updateData(int codigoEstado, String data) {
			Log.d(TAG + ".eventDataListener.updateData() ", "data: " + data);
			try {
				Consulta consulta =  DeJSONAObjeto.obtenerConsultaEventos(data);
				if(consulta.getEventos() != null && consulta.getEventos().size() > 0) {
					operation.setEvento(consulta.getEventos().iterator().next());	
				}
				processOperation(operation);
			} catch (Exception e) {
				Log.e(TAG + ".eventDataListener.updateData() ", e.getMessage(), e);
				showMsg(getApplicationContext().getString(
						R.string.error_lbl), e.getMessage());
			}
			
		}

		@Override public void setException(final String exceptionMsg) {
			Log.d(TAG + ".eventDataListener.manejarExcepcion(...) ", 
					" -- exceptionMsg: " + exceptionMsg);	
			showMsg(getApplicationContext().getString(
					R.string.error_lbl), exceptionMsg);
		}
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.uri_launcher);
        final Uri data = getIntent().getData();
        
        serverURL = data.getQueryParameter("serverURL");
        String eventoId = data.getQueryParameter("eventoId");
        String browserToken = data.getQueryParameter("browserToken");
        String encodedMsg = data.getQueryParameter("msg");
        String msg = decodeString(encodedMsg);
        Aplicacion.CONTROL_ACCESO_URL = data.getHost();
        Log.d(TAG + ".onCreate() - ", " - host: " + data.getHost() + 
        		" - path: " + data.getPath() + 
        		" - userInfo: " + data.getUserInfo() + 
        		" - serverURL: " + serverURL + " - eventoId: " + eventoId +
        		" - browserToken: " + browserToken + 
        		" - msg: " + msg);
        if(msg != null) {
    		try {
    			operation = Operation.parse(msg);
    		} catch (Exception ex) {
    			Log.e(TAG + ".onCreate(...)", ex.getMessage(), ex);
    		}
        } else operation = new Operation();
        operation.setTipo(browserToken);
        if(operation.getEvento() != null) {
        	new GetDataTask(eventDataListener).execute(operation.getEvento().getURL());
        }
    }
    
    private String decodeString(String string) {
    	if(string == null) return null;
    	String result = null;
        try {
        	result = URLDecoder.decode(string, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Log.e(TAG + ".decodeString()", ex.getMessage(), ex);
		}
    	return result;
    }
    
    private void processOperation(Operation operation) {
    	Log.d(TAG + ".processOperation(...)", "- processOperation(...)");
    	AppData.INSTANCE.setOperation(operation);
		Intent intent = new Intent(this, Aplicacion.class);;
        startActivity(intent);
    }
    
	private void showMsg(String caption, String msg) {
    	AlertDialog.Builder builder= new AlertDialog.Builder(UriLauncherActivity.this);
		builder.setTitle(caption).setMessage(msg)
			.setPositiveButton(R.string.solicitar_label, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	        		Intent intent = new Intent(
	        				UriLauncherActivity.this, Aplicacion.class);;
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