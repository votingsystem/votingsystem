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

import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sistemavotacion.android.service.PublishService;
import org.sistemavotacion.android.service.PublishServiceListener;
import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class WebActivity extends FragmentActivity implements WebSessionListener, 
	CertPinDialogListener, PublishServiceListener {
	
	public static final String TAG = "WebActivity";
	
	public static final String SCREEN_EXTRA_KEY   = "screenKey";
	public static final String EDITOR_SESSION_KEY = "editorSessionKey";
	
	public enum Screen {PUBLISH_VOTING, PUBLISH_MANIFEST, PUBLISH_CLAIM}
	
	private static WebView svWebView;
	private String serverURL = null;
	private String urlDocument = null;
	private String urlSignedDocument = null;

	private Screen screen;
	private JavaScriptInterface javaScriptInterface;
	private boolean isPageLoaded = false;
    private ProgressDialog progressDialog = null;
    private static List<String> pendingOperations = new ArrayList<String>();
	private PublishService publishService = null;
	
	ServiceConnection publishServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG + ".publishServiceConnection.onServiceDisconnected()", 
					" - publishServiceConnection.onServiceDisconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG + ".publishServiceConnection.onServiceConnected()", 
					" - publishServiceConnection.onServiceConnected");
			publishService = ((PublishService.PublishServiceBinder) service).getBinder();
		}
	};

	private void unbindSignService() {
		Log.d(TAG + ".unbindSignService()", "--- unbindSignService");
		if(publishService != null)	unbindService(publishServiceConnection);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_activity);     
        String screenValue = getIntent().getStringExtra(SCREEN_EXTRA_KEY);
        String screenTitle = null;
        if(screenValue != null) {
        	screen = Screen.valueOf(screenValue);
        	switch(screen) {
	        	case PUBLISH_CLAIM:
	        		serverURL = ServerPaths.getURLPublish(
	        				Aplicacion.CONTROL_ACCESO_URL, Screen.PUBLISH_CLAIM);
	        		screenTitle = getString(R.string.publish_claim_caption);
	        		break;
	        	case PUBLISH_MANIFEST:
	        		serverURL = ServerPaths.getURLPublish(
	        				Aplicacion.CONTROL_ACCESO_URL, Screen.PUBLISH_MANIFEST);
	        		screenTitle = getString(R.string.publish_manifest_caption);
	        		break;
	        	case PUBLISH_VOTING:
	        		serverURL = ServerPaths.getURLPublish(
	        				Aplicacion.CONTROL_ACCESO_URL, Screen.PUBLISH_VOTING);
	        		screenTitle = getString(R.string.publish_voting_caption);
	        		break;
        	}  
        }
		try {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setTitle(screenTitle);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".onCreate(...)", " --- android api 11 I doesn't have method 'setLogo'");
		}  
        loadUrl(serverURL);
    }
    
	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
	    		Intent intent = new Intent(this, FragmentTabsPager.class);   
	    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    		startActivity(intent); 
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    	unbindSignService();
    };
    
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
    
    private void showPinScreen(String message) {
    	CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    pinDialog.show(ft, CertPinDialog.TAG);
    }
	
    private void loadUrl(String serverURL) {
    	isPageLoaded = false;
    	javaScriptInterface = new JavaScriptInterface(this);
        svWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = svWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        svWebView.setClickable(true);
        svWebView.addJavascriptInterface(javaScriptInterface, "androidClient");
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        svWebView.loadUrl(serverURL);
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
    
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		String sessionDataKey = UUID.randomUUID().toString();
		svWebView.loadUrl("javascript:getEditorData('" + sessionDataKey + "')");
		savedInstanceState.putString(EDITOR_SESSION_KEY, sessionDataKey);
		Log.d(TAG + ".onSaveInstanceState(...) ", " --- sessionDataKey: " + sessionDataKey);
	}
    
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {		
		String sessionDataKey = savedInstanceState.getString(EDITOR_SESSION_KEY);
		javaScriptInterface.getEditorData(sessionDataKey, this);
		Log.d(TAG + ".onRestoreInstanceState(...) ", " --- sessionDataKey: " + sessionDataKey);
	}

	public void processOperation(Operation operation) {
		Log.d(TAG + ".processOperation(...) ", 
				" --- processOperation: " + operation.getTipo());
		Intent publishServiceIntent = new Intent(this, PublishService.class);
		startService(publishServiceIntent);
		bindService(publishServiceIntent, publishServiceConnection, BIND_AUTO_CREATE);
		switch(operation.getTipo()) {
			case PUBLICACION_MANIFIESTO_PDF:
				urlDocument = operation.getUrlDocumento();
				urlSignedDocument = operation.getUrlEnvioDocumento();
				showPinScreen(null);
				break;
			case PUBLICACION_VOTACION_SMIME:
				break;
				default:
					Log.d(TAG + ".processOperation(...) ", " --- unknown operation: " + operation.getTipo().toString());
		}
	}
	
	
	@Override
	public void updateEditorData(Operation editorData) {
		if(editorData == null || editorData.getEvento() == null) {
			Log.d(TAG + ".updateEditorData(...) ", " --- editorData null");
			return;
		} 
		String editorDataStr = null;
		try {
			editorDataStr = editorData.obtenerJSONStr();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		Log.d(TAG + ".updateEditorData(...) ", " --- editorDataStr: " + editorDataStr);
		String jsOperation = "javascript:updateEditorData('" + editorDataStr + "')";
		if(isPageLoaded) {
			Log.d(TAG + ".updateEditorData(...) ", " --- lanzando jsOperation: " + jsOperation);
			svWebView.loadUrl(jsOperation);
		} 
		else  {
			Log.d(TAG + ".updateEditorData(...) ", " --- poniendo en cola jsOperation: " + jsOperation);
			pendingOperations.add(jsOperation);
		} 
	}
	
	public void isPageLoaded(boolean isPageLoaded) {
		this.isPageLoaded = isPageLoaded;
		if(isPageLoaded) {
			for(final String javascriptOperation: pendingOperations) {
		    	runOnUiThread(new Runnable() {
		    	    public void run() {
		    	    	Log.d(TAG + ".isPageLoaded(...) ", " --- javascriptOperation: " + javascriptOperation);
		    	    	svWebView.loadUrl(javascriptOperation);
		    	    	pendingOperations.remove(javascriptOperation);
		    	    }
		    	});  
			}
		}
	}

	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", " - caption: " 
				+ caption + "  - showMessage: " + message);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(message).show();
	}
	
	@Override
	public void setPublishServiceMsg(int statusCode, String msg) {
    	Log.d(TAG + ".setPublishServiceMsg(...) ", " - statusCode: " + statusCode);
		
	}

	@Override
	public void proccessReceipt(SMIMEMessageWrapper receipt) {
		Log.d(TAG + ".proccessReceipt(...) ", " - proccessReceipt ");
		
	}

	@Override
	public void setPin(String pin) {
		Log.d(TAG + ".setPin(...) ", " - setPin ");
		if(pin != null) {
	        try {
				FileInputStream fis = openFileInput(KEY_STORE_FILE);
				byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
	    		if(publishService != null) publishService.publishPDF(urlDocument, urlSignedDocument, this, 
	    				keyStoreBytes, pin.toCharArray());
	    		else Log.d(TAG + ".publishService(...) ", " - publishService null ");
	        } catch(Exception ex) {
				ex.printStackTrace();
				showMessage(getString(R.string.error_lbl), 
						getString(R.string.pin_error_msg));
	        }

		} 
	}
	

}