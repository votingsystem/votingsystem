package org.votingsystem.android;

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
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.votingsystem.android.callable.PDFPublisher;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ContextVSImpl;
import org.votingsystem.model.SubSystemVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventPublishingActivity extends ActionBarActivity implements CertPinDialogListener {
	
	public static final String TAG = "EventPublishingActivity";

	public static final String EDITOR_SESSION_KEY = "editorSessionKey";
    public static final String FORM_TYPE_KEY = "formTypeKey";
	
	private WebView svWebView;
	private TypeVS formType;
	private JavaScriptInterface javaScriptInterface;
	private OperationVS pendingOperationVS;
    private ContextVSImpl contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;
    private PublishTask publishTask;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_publishing_activity);
        contextVS = ContextVSImpl.getInstance(getBaseContext());
        String operation = getIntent().getStringExtra(OperationVS.OPERATION_KEY);
        if(operation!= null) { //called from browser
        	try {
                this.pendingOperationVS = OperationVS.parse(operation);
                if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
                    Log.d(TAG + ".processOperation(...)", " - Cert Not Found - ");
                    showCertNotFoundDialog();
                } else showPinScreen(null);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
        }
        String formTypeStr = getIntent().getStringExtra(FORM_TYPE_KEY);
        Log.d(TAG + ".onCreate(...) ", " - formType: " + formTypeStr);
        String screenTitle = null;
        String serverURL = null;
        if(formTypeStr != null) {
            serverURL = contextVS.getAccessControlVS().getPublishServiceURL(formType);
            formType = TypeVS.valueOf(formTypeStr);
        	switch(formType) {
	        	case CLAIM_PUBLISHING:
	        		screenTitle = getString(R.string.publish_claim_caption);
	        		break;
	        	case MANIFEST_PUBLISHING:
	        		screenTitle = getString(R.string.publish_manifest_caption);
	        		break;
	        	case VOTING_PUBLISHING:
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

    //This is for JavaScriptInterface.java operation processing
    public void processOperation(OperationVS operationVS) {
        Log.d(TAG + ".processOperation(...) ",
                " --- processOperation: " + operationVS.getTypeVS());
        this.pendingOperationVS = operationVS;
        if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
            Log.d(TAG + ".processOperation(...)", " - Cert Not Found - ");
            showCertNotFoundDialog();
        } else showPinScreen(null);
    }
	
    private void loadUrl(String serverURL) {
    	Log.d(TAG + ".serverURL(...)", " --- serverURL: " + serverURL);
    	javaScriptInterface = new JavaScriptInterface(this);
        svWebView = (WebView) findViewById(R.id.webview);
        svWebView.setWebChromeClient(new WebChromeClient());
        svWebView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = svWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        String userAgent = webSettings.getUserAgentString();
        //To prevent block if ckeditor detects the 'Mobile' in user agent
        webSettings.setUserAgentString(userAgent.replaceAll("Mobile", ""));
        svWebView.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        svWebView.addJavascriptInterface(javaScriptInterface, "androidClient");
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
    
    public void setSignClientMessage(final String mensaje){
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	Log.d(TAG + ".setSignClientMessage", "mensaje: " + mensaje);
    	    	svWebView.loadUrl("javascript:setSignClientMessage('" + mensaje + "')");
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
		Log.d(TAG + ".onRestoreInstanceState(...) ", " --- onRestoreInstanceState");
	}
	
	private void showCertNotFoundDialog() {
		Log.d(TAG + ".showCertNotFoundDialog(...)", " - showCertNotFoundDialog - ");
		CertNotFoundDialog certDialog = new CertNotFoundDialog();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(ContextVSImpl.CERT_NOT_FOUND_DIALOG_ID);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    certDialog.show(ft, ContextVSImpl.CERT_NOT_FOUND_DIALOG_ID);
	}
	
	public void sendMessageToWebApp(OperationVS operationVS) {
		if(operationVS == null) {
			Log.d(TAG + ".sendMessageToWebApp(...) ", " --- operationVS null");
			return;
		}
		try {
            String operationStr = operationVS.getJSONStr();
            Log.d(TAG + ".sendMessageToWebApp(...) ", " --- operationStr: " + operationStr);
            String jsOperation = "javascript:sendMessageToWebApp('" + operationStr + "')";
            svWebView.loadUrl(jsOperation);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", " - caption: " 
				+ caption + "  - message: " + message);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(Html.fromHtml(
                (message == null? "" : message))).show();
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

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", " - onStop");
        isDestroyed = true;
        if(publishTask != null) publishTask.cancel(true);
    }

    @Override public void onResume() {
        super.onResume();
        Log.d(TAG + ".onResume() ", " - onResume");
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

    private class PublishTask extends AsyncTask<URL, Integer, ResponseVS> {

        private String pin = null;

        public PublishTask(String pin) {
            this.pin = pin;
        }

        protected ResponseVS doInBackground(URL... urls) {
            Log.d(TAG + ".PublishTask.doInBackground(...)",
                    " - doInBackground - operation: " + pendingOperationVS.getTypeVS());
            try {
                ResponseVS responseVS = null;
                byte[] keyStoreBytes = null;
                FileInputStream fis = openFileInput(KEY_STORE_FILE);
                keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, pin.toCharArray());
                PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, pin.toCharArray());
                //X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(USER_CERT_ALIAS);
                switch(pendingOperationVS.getTypeVS()) {
                    case MANIFEST_PUBLISHING:
                        PDFPublisher publisher = new PDFPublisher(pendingOperationVS.getUrlEnvioDocumento(),
                                pendingOperationVS.getContentFirma().toString(),
                                keyStoreBytes, pin.toCharArray(), null, null, getBaseContext());
                        responseVS = publisher.call();
                        break;
                    case VOTING_PUBLISHING:
                    case CLAIM_PUBLISHING:
                    case CONTROL_CENTER_ASSOCIATION:
                        boolean isEncryptedResponse = false;
                        pendingOperationVS.getContentFirma().put("UUID", UUID.randomUUID().toString());
                        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                                pendingOperationVS.getUrlEnvioDocumento(),
                                pendingOperationVS.getContentFirma().toString(),
                                pendingOperationVS.getSignedMessageSubject(), isEncryptedResponse,
                                keyStoreBytes, pin.toCharArray(),
                                contextVS.getAccessControlVS().getCertificate(), getBaseContext());
                        responseVS = smimeSignedSender.call();
                        break;
                    default:
                        Log.d(TAG + ".doInBackground(...) ", " --- unknown operation: " +
                                pendingOperationVS.getTypeVS().toString());
                }
                return responseVS;
            } catch(Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
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

        protected void onPostExecute(ResponseVS response) {
            Log.d(TAG + ".PublishTask.onPostExecute(...)", " - onPostExecute - status:" +
                    response.getStatusCode());
            showProgress(false, true);
            String resultMsg = null;
            String resultCaption = null;
            SubSystemVS selectedSubsystem = null;
            if(ResponseVS.SC_OK == response.getStatusCode()) {
                resultCaption = getString(R.string.operacion_ok_msg);
                switch(pendingOperationVS.getTypeVS()) {
                    case MANIFEST_PUBLISHING:
                        resultMsg = getString(R.string.publish_manifest_OK_prefix_msg);
                        selectedSubsystem = SubSystemVS.MANIFESTS;
                        break;
                    case CLAIM_PUBLISHING:
                        resultMsg = getString(R.string.publish_claim_OK_prefix_msg);
                        selectedSubsystem = SubSystemVS.CLAIMS;
                        break;
                    case VOTING_PUBLISHING:
                        resultMsg = getString(R.string.publish_voting_OK_prefix_msg);
                        selectedSubsystem = SubSystemVS.VOTING;
                        break;
                }
                final SubSystemVS subSystemVS = selectedSubsystem;
                resultMsg = resultMsg + " " + getString(R.string.publish_document_OK_sufix_msg);
                new AlertDialog.Builder(EventPublishingActivity.this).setTitle(resultCaption).
                        setMessage(resultMsg).setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        contextVS.setSelectedSubsystem(subSystemVS);
                        Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
                        startActivity(intent);
                    }
                }).show();
            } else {
                resultCaption = getString(R.string.publish_document_ERROR_msg);
                resultMsg = response.getMessage();
                showMessage(resultCaption, resultMsg);
            }
        }
    }

}