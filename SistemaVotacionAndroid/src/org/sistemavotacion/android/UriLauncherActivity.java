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

import org.sistemavotacion.util.ServerPaths;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

public class UriLauncherActivity extends Activity {
	
	public static final String TAG = "UriLauncherActivity";
	
	private static WebView myWebView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uri_launcher);
		
        final Uri data = getIntent().getData();
        String appMessage = data.getQueryParameter("appMessage");
        Log.d(TAG + ".onCreate() - ", "data.getPath(): " + data.getPath());
        Log.d(TAG + ".onCreate() - ", "appMessage: " + appMessage);
        //URLDecoder.decode(appMessage, "UTF-8");

        
        JavaScriptInterface jsi = new JavaScriptInterface(this);
        
        String dataDecoded = null;
        
        myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        myWebView.setClickable(true);
        myWebView.addJavascriptInterface(jsi, "ClienteAndroid");
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        myWebView.loadUrl(ServerPaths.getUrlAndroidBrowserSession(Aplicacion.CONTROL_ACCESO_URL));
		
        setClienteFirmaMessage("Contacto establecido con cliente Android");
        
        Button aceptarButton = (Button) findViewById(R.id.aceptar_button);
        aceptarButton.setVisibility(View.VISIBLE);
        aceptarButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	setClienteFirmaMessage("Hola desde la aplicación Android del Sistema de Votación");
            }
        });
        /*try {
			dataDecoded = URLDecoder.decode(data.getPath(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.d(TAG + ".onCreate() - data.getQueryParameter(): ", data.getQueryParameter("appMessage"));
		Log.d(TAG + ".onCreate() - data.getUserInfo(): ", data.getUserInfo());
		
		
		Log.d(TAG + ".onCreate() - dataDecoded: ", dataDecoded);
		String jsonData = null;
		if(dataDecoded != null) {
			jsonData = dataDecoded.split("&clienteAndroidSistemaVotacion=")[1];
		}
		Log.d(TAG + ".onCreate() - jsonData: ", jsonData);
        //startActivity(RepositoryViewActivity.createIntent(repository));*/
    }
    
    public void setClienteFirmaMessage(final String mensaje){
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	Log.d(TAG + ".setClienteFirmaMessage", "mensaje: " + mensaje);
    	    	myWebView.loadUrl("javascript:alert('holita_con comillas')");
    	    	myWebView.loadUrl("javascript:setClienteFirmaMessage('" + mensaje + "')");
    	    }
    	});
    }
}
