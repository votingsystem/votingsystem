package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.callable.PDFPublisher;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.JavaScriptInterface;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventVSPublishingFragment extends Fragment {
	
	public static final String TAG = "EventVSPublishingFragment";

	public static final String EDITOR_SESSION_KEY = "editorSessionKey";
    public static final String FORM_TYPE_KEY = "formTypeKey";
	
	private WebView svWebView;
	private TypeVS formType;
	private JavaScriptInterface javaScriptInterface;
	private OperationVS pendingOperationVS;
    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean progressVisible;
    private PublishTask publishTask;
    private View rootView;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            if(pin != null) {
                if(publishTask != null) publishTask.cancel(true);
                publishTask = new PublishTask(pin);
                publishTask.execute();
            }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.eventvs_publishing_fragment, container, false);
        mainLayout = (FrameLayout) rootView.findViewById( R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        mainLayout.getForeground().setAlpha(0);
        progressVisible = false;
        loadForm();
        // if set to true savedInstanceState will be allways null
        //setRetainInstance(true);
        setHasOptionsMenu(true);
        Log.d(TAG + ".onCreateView(...) ", "savedInstanceState: " + savedInstanceState);
        return rootView;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart(...) ", "onStart");
        super.onStart();
    }

    @Override public void onActivityCreated (Bundle savedInstanceState) {
        Log.d(TAG + ".onActivityCreated(...) ", "onActivityCreated - savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
    }

    private void loadForm() {
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        String operationStr = getArguments().getString(OperationVS.OPERATION_KEY);
        if(!TextUtils.isEmpty(operationStr)) { //called from browser
            try {
                this.pendingOperationVS = OperationVS.parse(operationStr);
                Log.d(TAG + ".onCreateView(...) ", "restoring: " + operationStr);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        String formTypeStr = getArguments().getString(FORM_TYPE_KEY);
        String screenTitle = null;
        String serverURL = null;
        if(formTypeStr != null) {
            formType = TypeVS.valueOf(formTypeStr);
            serverURL = contextVS.getAccessControl().getPublishServiceURL(formType);
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
        Log.d(TAG + ".onCreate(...) ", "onCreate: " + formType + " - serverURL: " + serverURL);
        getActivity().setTitle(screenTitle);
        progressMessage.setText(R.string.loading_data_msg);
        showProgress(true, true);
        javaScriptInterface = new JavaScriptInterface(getActivity());
        svWebView = (WebView) rootView.findViewById(R.id.webview);
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


	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
	    		Intent intent = new Intent(getActivity().getApplicationContext(),
                        NavigationDrawer.class);
	    		startActivity(intent); 
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", "onDestroy");
        if(publishTask != null) publishTask.cancel(true);
    }

    private void showPinScreen(String message) {
        CertPinDialogFragment pinDialog = CertPinDialogFragment.newInstance(
                message, false, this.getClass().getName());
        pinDialog.show(getFragmentManager(), CertPinDialogFragment.TAG);
    }

    //This is for JavaScriptInterface.java operation processing
    public void processOperation(OperationVS operationVS) {
        Log.d(TAG + ".processOperation(...) ", "processOperation: " + operationVS.getTypeVS() +
                " - operation status: " + operationVS.getStatusCode());
        try {
            if(ResponseVS.SC_PROCESSING == operationVS.getStatusCode()) {
                this.pendingOperationVS = operationVS;
                if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
                    Log.d(TAG + ".processOperation(...)", "Cert Not Found - ");
                    showCertNotFoundDialog();
                } else showPinScreen(null);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void onBackPressed() {
        Log.d(TAG + ".onBackPressed(...)",  "onBackPressed");
        if(svWebView.canGoBack()) svWebView.goBack();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

	private void showCertNotFoundDialog() {
		Log.d(TAG + ".showCertNotFoundDialog(...)", "showCertNotFoundDialog");
		CertNotFoundDialog certDialog = new CertNotFoundDialog();
		FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
	    Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(
                ContextVS.CERT_NOT_FOUND_DIALOG_ID);
	    if (prev != null) ft.remove(prev);
	    ft.addToBackStack(null);
	    certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
	}
	
	public void sendMessageToWebApp(OperationVS operationVS) {
		if(operationVS == null) {
			Log.d(TAG + ".sendMessageToWebApp(...) ", "operationVS null");
			return;
		}
		try {
            String operationStr = operationVS.getJSON().toString();
            Log.d(TAG + ".sendMessageToWebApp(...) ", "operationStr: " + operationStr);
            String jsOperation = "javascript:sendMessageToWebApp('" + operationStr + "')";
            svWebView.loadUrl(jsOperation);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", "caption: "+ caption + "  - message: " + message);
		AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
		builder.setTitle(caption).setMessage(Html.fromHtml((message == null? "" : message))).show();
	}

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", "onStop");
        if(publishTask != null) publishTask.cancel(true);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
        Log.d(TAG + ".onResume() ", "onResume");
    }

    public void showProgress(boolean shown, boolean animate) {
        if (progressVisible == shown) return;
        progressVisible = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
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
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
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

        public PublishTask(String pin) { this.pin = pin; }

        protected ResponseVS doInBackground(URL... urls) {
            Log.d(TAG + ".PublishTask.doInBackground(...)",
                    "doInBackground - operation: " + pendingOperationVS.getTypeVS());
            try {
                ResponseVS responseVS = null;
                byte[] keyStoreBytes = null;
                FileInputStream fis = getActivity().openFileInput(KEY_STORE_FILE);
                keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, pin.toCharArray());
                PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, pin.toCharArray());
                //X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(USER_CERT_ALIAS);
                switch(pendingOperationVS.getTypeVS()) {
                    case MANIFEST_PUBLISHING:
                        PDFPublisher publisher = new PDFPublisher(pendingOperationVS.getUrlEnvioDocumento(),
                                pendingOperationVS.getContentFirma().toString(),
                                keyStoreBytes, pin.toCharArray(), null, null,
                                getActivity().getApplicationContext());
                        responseVS = publisher.call();
                        break;
                    case VOTING_PUBLISHING:
                    case CLAIM_PUBLISHING:
                    case CONTROL_CENTER_ASSOCIATION:
                        pendingOperationVS.getContentFirma().put("UUID", UUID.randomUUID().toString());
                        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                                pendingOperationVS.getUrlEnvioDocumento(),
                                pendingOperationVS.getContentFirma().toString(),
                                ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                                pendingOperationVS.getSignedMessageSubject(),
                                keyStoreBytes, pin.toCharArray(),
                                contextVS.getAccessControl().getCertificate(),
                                getActivity().getApplicationContext());
                        responseVS = smimeSignedSender.call();
                        break;
                    default:
                        Log.d(TAG + ".doInBackground(...) ", "unknown operation: " +
                                pendingOperationVS.getTypeVS().toString());
                }
                return responseVS;
            } catch(Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
            }
        }

        protected void onPreExecute() {
            Log.d(TAG + ".PublishTask.onPreExecute(...)", "onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            progressMessage.setText(R.string.publishing_document_msg);
            showProgress(true, true);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(ResponseVS response) {
            Log.d(TAG + ".PublishTask.onPostExecute(...)", "onPostExecute - status:" +
                    response.getStatusCode());
            showProgress(false, true);
            String resultMsg = null;
            String resultCaption = null;
            GroupPosition selectedSubsystem = null;
            if(ResponseVS.SC_OK == response.getStatusCode()) {
                resultCaption = getString(R.string.operation_ok_msg);
                switch(pendingOperationVS.getTypeVS()) {
                    case MANIFEST_PUBLISHING:
                        resultMsg = getString(R.string.publish_manifest_OK_prefix_msg);
                        selectedSubsystem = GroupPosition.MANIFESTS;
                        break;
                    case CLAIM_PUBLISHING:
                        resultMsg = getString(R.string.publish_claim_OK_prefix_msg);
                        selectedSubsystem = GroupPosition.CLAIMS;
                        break;
                    case VOTING_PUBLISHING:
                        resultMsg = getString(R.string.publish_voting_OK_prefix_msg);
                        selectedSubsystem = GroupPosition.VOTING;
                        break;
                }
                final GroupPosition groupPosition = selectedSubsystem;
                resultMsg = resultMsg + " " + getString(R.string.publish_document_OK_sufix_msg);
                new AlertDialog.Builder(getActivity()).setTitle(resultCaption).
                        setMessage(resultMsg).setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(getActivity().getApplicationContext(),
                                NavigationDrawer.class);
                        intent.putExtra(NavigatorDrawerOptionsAdapter.GROUP_POSITION_KEY,
                                groupPosition.getPosition());
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