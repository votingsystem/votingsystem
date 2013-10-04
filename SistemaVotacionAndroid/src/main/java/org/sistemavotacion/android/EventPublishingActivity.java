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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.sistemavotacion.android.ui.CertNotFoundDialog;
import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.callable.SignedPDFSender;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.SubSystem;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.sistemavotacion.android.AppData.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.AppData.KEY_STORE_FILE;

public class EventPublishingActivity extends ActionBarActivity
	implements WebSessionListener, CertPinDialogListener {
	
	public static final String TAG = "EventPublishingActivity";

	public static final String EDITOR_SESSION_KEY = "editorSessionKey";
    public static final String FORM_TYPE_KEY = "formTypeKey";
	
	private static WebView svWebView;
	private String serverURL = null;

	private Operation.Tipo formType;
	private JavaScriptInterface javaScriptInterface;
	private boolean isPageLoaded = false;
    private static List<String> pendingOperations = new ArrayList<String>();
	private Operation pendingOperation;
    private AppData appData;

    private TextView progressMessage;

    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;
    private PublishTask publishTask;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_publishing_activity);
        appData = AppData.getInstance(getBaseContext());
        String operation = getIntent().getStringExtra(Operation.OPERATION_KEY);
        if(operation!= null) { //called from browser
        	try {
				processOperation(Operation.parse(operation));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
        }
        String formTypeStr = getIntent().getStringExtra(FORM_TYPE_KEY);
        String screenTitle = null;
        if(formTypeStr != null) {
            formType = Operation.Tipo.valueOf(formTypeStr);
        	switch(formType) {
	        	case PUBLICACION_RECLAMACION_SMIME:
	        		serverURL = ServerPaths.getURLPublish(
                            appData.getAccessControlURL(),
                            Operation.Tipo.PUBLICACION_RECLAMACION_SMIME);
	        		screenTitle = getString(R.string.publish_claim_caption);
	        		break;
	        	case PUBLICACION_MANIFIESTO_PDF:
	        		serverURL = ServerPaths.getURLPublish(
                            appData.getAccessControlURL(),
                            Operation.Tipo.PUBLICACION_MANIFIESTO_PDF);
	        		screenTitle = getString(R.string.publish_manifest_caption);
	        		break;
	        	case PUBLICACION_VOTACION_SMIME:
	        		serverURL = ServerPaths.getURLPublish(
                            appData.getAccessControlURL(),
                            Operation.Tipo.PUBLICACION_VOTACION_SMIME);
	        		screenTitle = getString(R.string.publish_voting_caption);
	        		break;
        	}  
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(screenTitle);
        mainLayout = (FrameLayout) findViewById( R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        progressMessage = (TextView)findViewById(R.id.progressMessage);
        mainLayout.getForeground().setAlpha( 0);
        isProgressShown = false;
        isDestroyed = false;
        progressMessage.setText(R.string.loading_data_msg);
        showProgress(true, true);
        Log.d(TAG + ".onCreate(...) ", " - formType: " + formType + " - serverURL: " + serverURL);
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
        isDestroyed = true;
        if(publishTask != null) publishTask.cancel(true);
    }

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", " - onStop");
        isDestroyed = true;
        if(publishTask != null) publishTask.cancel(true);
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
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        svWebView.setClickable(true);
        svWebView.addJavascriptInterface(javaScriptInterface, "androidClient");
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        svWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                showProgress(false, true);
            }
        });
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
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(Html.fromHtml(message)).show();
	}

	@Override
	public void setPin(String pin) {
		Log.d(TAG + ".setPin(...) ", " - setPin ");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
        if(pin == null) {
            Log.d(TAG + ".setPin()", "--- setPin - pin null");
            return;
        }
        if(publishTask != null) publishTask.cancel(true);
        publishTask = new PublishTask(pin);
        publishTask.execute();
	}

    public void showProgress(boolean shown, boolean animate) {
        if (isProgressShown == shown) {
            return;
        }
        isProgressShown = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha( 0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {return false;}
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) { return true; }
            });
        }

    }

    private class PublishTask extends AsyncTask<URL, Integer, Respuesta> {

        private String pin = null;

        public PublishTask(String pin) {
            this.pin = pin;
        }

        protected Respuesta doInBackground(URL... urls) {
            Log.d(TAG + ".PublishTask.doInBackground(...)",
                    " - doInBackground - operation: " + pendingOperation.getTipo());
            try {
                Respuesta respuesta = null;
                byte[] keyStoreBytes = null;
                FileInputStream fis = openFileInput(KEY_STORE_FILE);
                keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, pin.toCharArray());
                PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, pin.toCharArray());
                //X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
                switch(pendingOperation.getTipo()) {
                    case PUBLICACION_MANIFIESTO_PDF:
                        SignedPDFSender signedPDFSender = new SignedPDFSender(
                                pendingOperation.getUrlDocumento(),
                                pendingOperation.getUrlEnvioDocumento(),
                                keyStoreBytes, pin.toCharArray(), null, null, getBaseContext());
                        respuesta = signedPDFSender.call();
                        break;
                    case PUBLICACION_VOTACION_SMIME:
                    case PUBLICACION_RECLAMACION_SMIME:
                    case ASOCIAR_CENTRO_CONTROL_SMIME:
                        boolean isEncryptedResponse = false;
                        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                                pendingOperation.getUrlEnvioDocumento(),
                                pendingOperation.getContenidoFirma().toString(),
                                pendingOperation.getAsuntoMensajeFirmado(), isEncryptedResponse,
                                keyStoreBytes, pin.toCharArray(),
                                appData.getControlAcceso().getCertificado(), getBaseContext());
                        respuesta = smimeSignedSender.call();
                        break;
                    default:
                        Log.d(TAG + ".processOperation(...) ", " --- unknown operation: " +
                                pendingOperation.getTipo().toString());
                }
                return respuesta;
            } catch(Exception ex) {
                ex.printStackTrace();
                return new Respuesta(Respuesta.SC_ERROR, ex.getLocalizedMessage());
            }
        }

        protected void onPreExecute() {
            Log.d(TAG + ".PublishTask.onPreExecute(...)", " --- onPreExecute");
            getWindow().getDecorView().findViewById(
                    android.R.id.content).invalidate();
            progressMessage.setText(R.string.publishing_document_msg);
            showProgress(true, true);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(Respuesta response) {
            Log.d(TAG + ".PublishTask.onPostExecute(...)", " - onPostExecute - status:" +
                    response.getCodigoEstado());
            showProgress(false, true);
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
                new AlertDialog.Builder(EventPublishingActivity.this).setTitle(resultCaption).
                        setMessage(resultMsg).setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
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

}