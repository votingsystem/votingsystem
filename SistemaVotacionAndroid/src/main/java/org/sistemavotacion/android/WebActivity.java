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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.sistemavotacion.android.service.PublishService;
import org.sistemavotacion.android.service.ServiceListener;
import org.sistemavotacion.android.ui.CertNotFoundDialog;
import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.SubSystem;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.sistemavotacion.android.AppData.KEY_STORE_FILE;

public class WebActivity extends ActionBarActivity
	implements WebSessionListener, CertPinDialogListener, ServiceListener {
	
	public static final String TAG = "WebActivity";
	
	public static final String SCREEN_EXTRA_KEY   = "screenKey";
	public static final String OPERATION_KEY      = "operationKey";
	public static final String EDITOR_SESSION_KEY = "editorSessionKey";
	
	public enum Screen {PUBLISH_VOTING, PUBLISH_MANIFEST, PUBLISH_CLAIM}
	
	private static WebView svWebView;
	private String serverURL = null;

	private Screen screen;
	private JavaScriptInterface javaScriptInterface;
	private boolean isPageLoaded = false;
    private ProgressDialog progressDialog = null;
    private static List<String> pendingOperations = new ArrayList<String>();
	private PublishService publishService = null;
	private Operation pendingOperation;
    private AppData appData;
	
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

	private void unbindPublishService() {
		Log.d(TAG + ".unbindSignService()", "--- unbindSignService");
		if(publishService != null)	unbindService(publishServiceConnection);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_activity);
        appData = AppData.getInstance(getBaseContext());
        String operation = getIntent().getStringExtra(OPERATION_KEY);
        if(operation!= null) {
        	try {
				processOperation(Operation.parse(operation));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
        }
        String screenValue = getIntent().getStringExtra(SCREEN_EXTRA_KEY);
        String screenTitle = null;
        if(screenValue != null) {
        	screen = Screen.valueOf(screenValue);
        	switch(screen) {
	        	case PUBLISH_CLAIM:
	        		serverURL = ServerPaths.getURLPublish(
                            appData.getAccessControlURL(), Screen.PUBLISH_CLAIM);
	        		screenTitle = getString(R.string.publish_claim_caption);
	        		break;
	        	case PUBLISH_MANIFEST:
	        		serverURL = ServerPaths.getURLPublish(
                            appData.getAccessControlURL(), Screen.PUBLISH_MANIFEST);
	        		screenTitle = getString(R.string.publish_manifest_caption);
	        		break;
	        	case PUBLISH_VOTING:
	        		serverURL = ServerPaths.getURLPublish(
                            appData.getAccessControlURL(), Screen.PUBLISH_VOTING);
	        		screenTitle = getString(R.string.publish_voting_caption);
	        		break;
        	}  
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(screenTitle);
        loadUrl(serverURL);
    }
    
	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
	    		Intent intent = new Intent(this, NavigationDrawer.class);   
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
    	unbindPublishService();
    };
    
	private void showMsg(String caption, String msg) {
    	AlertDialog.Builder builder= new AlertDialog.Builder(WebActivity.this);
		builder.setTitle(caption).setMessage(msg)
			.setPositiveButton(R.string.solicitar_label, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	        		Intent intent = new Intent(
	        				WebActivity.this, MainActivity.class);;
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
    	Log.d(TAG + ".serverURL(...)", " - serverURL: " + serverURL);
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
            Log.d(TAG, ".onKeyDown(...) - back button pressed");
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
		this.pendingOperation = operation;
		if (!AppData.Estado.CON_CERTIFICADO.equals(appData.getEstado())) {
    		Log.d(TAG + ".processOperation(...)", " - Cert Not Found - ");
    		showCertNotFoundDialog();
    	} else showPinScreen(null);
	}
	
	private void showCertNotFoundDialog() {
		Log.d(TAG + ".showCertNotFoundDialog(...)", " - showCertNotFoundDialog - ");
		CertNotFoundDialog certDialog = new CertNotFoundDialog();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(AppData.CERT_NOT_FOUND_DIALOG_ID);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    certDialog.show(ft, AppData.CERT_NOT_FOUND_DIALOG_ID);
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
		builder.setTitle(caption).setMessage(Html.fromHtml(message)).show();
	}
	
    private void showProgressDialog(String dialogMessage) {
        if (progressDialog == null) 
        	progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(dialogMessage);
    	progressDialog.setIndeterminate(true);
    	progressDialog.setCancelable(false);
        progressDialog.show();
    }

	@Override
	public void setPin(String pin) {
		Log.d(TAG + ".setPin(...) ", " - setPin ");
		if(pin != null) {
			byte[] keyStoreBytes = null;
	        try {
	        	FileInputStream fis = openFileInput(KEY_STORE_FILE);
				keyStoreBytes = FileUtils.getBytesFromInputStream(fis);

	        } catch(Exception ex) {
				ex.printStackTrace();
				showMessage(getString(R.string.error_lbl), 
						getString(R.string.pin_error_msg));
	        }
	        if(publishService != null) {
	        	showProgressDialog(getString(R.string.publishing_document_msg));
	        	publishService.publishDocument(null, pendingOperation, 
	        			keyStoreBytes, pin.toCharArray(), this);

	        } else {
	        	Log.d(TAG + ".publishService(...) ", " - publishService null ");
	        } 
		} 
	}
	
	@Override public void proccessResponse(Integer requestId, Respuesta response) {
		Log.d(TAG + ".proccessResponse(...) ", " - requestId: " + requestId + 
				" - statusCode: " + response.getCodigoEstado());
		String resultMsg = null;
		String resultCaption = null;
		SubSystem selectedSubsystem = null;
		if(Respuesta.SC_OK == response.getCodigoEstado()) {
			resultCaption = getString(R.string.operacion_ok_msg);
			switch(pendingOperation.getTipo()) {
				case PUBLICACION_MANIFIESTO_PDF:
					resultMsg = getString(R.string.publish_manifest_OK_prefix_msg);
					selectedSubsystem = SubSystem.MANIFESTS;
					break;
				case PUBLICACION_RECLAMACION_SMIME:
					resultMsg = getString(R.string.publish_claim_OK_prefix_msg);
					selectedSubsystem = SubSystem.CLAIMS;
					break;
				case PUBLICACION_VOTACION_SMIME:
					resultMsg = getString(R.string.publish_voting_OK_prefix_msg);
					selectedSubsystem = SubSystem.VOTING;
					break;
			}
			final SubSystem subSystem = selectedSubsystem;
			resultMsg = resultMsg + " " + getString(R.string.publish_document_OK_sufix_msg);	
	    	new AlertDialog.Builder(this).setTitle(resultCaption).setMessage(resultMsg)
			.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					appData.setSelectedSubsystem(subSystem);
			    	Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
			    	startActivity(intent);
				}
			}).show();
		} else {
			resultCaption = getString(R.string.publish_document_ERROR_msg);
			resultMsg = response.getMensaje();
			showMessage(resultCaption, resultMsg);
		}
		
	}

	
	

}