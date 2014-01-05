package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.JavaScriptInterface;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventVSPublishingFragment extends Fragment {
	
	public static final String TAG = "EventVSPublishingFragment";
	
	private WebView webView;
	private TypeVS formType;
	private JavaScriptInterface javaScriptInterface;
	private OperationVS pendingOperationVS;
    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private View rootView;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            TypeVS operationType = (TypeVS) intent.getSerializableExtra(ContextVS.OPERATION_KEY);
            if(pin != null) launchSignAndSendService(pin);
            else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                showProgress(false, true);
                GroupPosition selectedSubsystem = null;
                if(ResponseVS.SC_OK == responseStatusCode) {
                    caption = getString(R.string.operation_ok_msg);
                    switch(operationType) {
                        case MANIFEST_PUBLISHING:
                            message = getString(R.string.publish_manifest_OK_prefix_msg);
                            selectedSubsystem = GroupPosition.MANIFESTS;
                            break;
                        case CLAIM_PUBLISHING:
                            message = getString(R.string.publish_claim_OK_prefix_msg);
                            selectedSubsystem = GroupPosition.CLAIMS;
                            break;
                        case VOTING_PUBLISHING:
                            message = getString(R.string.publish_voting_OK_prefix_msg);
                            selectedSubsystem = GroupPosition.VOTING;
                            break;
                    }
                    final GroupPosition groupPosition = selectedSubsystem;
                    message = message + " " + getString(R.string.publish_document_OK_sufix_msg);
                    new AlertDialog.Builder(getActivity()).setTitle(caption).
                            setMessage(message).setPositiveButton(R.string.ok_button,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(getActivity().getApplicationContext(),
                                    NavigationDrawer.class);
                            intent.putExtra(NavigatorDrawerOptionsAdapter.GROUP_POSITION_KEY,
                                    groupPosition.getPosition());
                            startActivity(intent);
                        }
                    }).show();
                } else {
                    caption = getString(R.string.publish_document_ERROR_msg);
                    showMessage(responseStatusCode, caption, Html.fromHtml(message).toString());
                }
            }
        }
    };

    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchSignAndSendService(...) ", "operation: " +
                pendingOperationVS.getTypeVS());
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    SignAndSendService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.OPERATION_KEY, pendingOperationVS.getTypeVS());
            startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
            startIntent.putExtra(ContextVS.URL_KEY, pendingOperationVS.getUrlEnvioDocumento());
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, pendingOperationVS.getSignedMessageSubject());
            startIntent.putExtra(ContextVS.MESSAGE_KEY,
                    pendingOperationVS.getContentFirma().toString());
            progressMessage.setText(R.string.publishing_document_msg);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.eventvs_publishing_fragment, container, false);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        mainLayout.getForeground().setAlpha(0);
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
        TypeVS formType = (TypeVS)getArguments().getSerializable(ContextVS.OPERATION_KEY);
        String screenTitle = null;
        String serverURL = contextVS.getAccessControl().getPublishServiceURL(formType);
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
        Log.d(TAG + ".onCreate(...) ", "formType: " + formType + " - serverURL: " + serverURL);
        getActivity().setTitle(screenTitle);
        progressMessage.setText(R.string.loading_data_msg);
        showProgress(true, true);
        javaScriptInterface = new JavaScriptInterface(getActivity());
        webView = (WebView) rootView.findViewById(R.id.webview);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        String userAgent = webSettings.getUserAgentString();
        //To prevent block if ckeditor detects the 'Mobile' in user agent
        webSettings.setUserAgentString(userAgent.replaceAll("Mobile", ""));
        webView.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(javaScriptInterface, "androidClient");
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                showProgress(false, true);
            }
        });
        webView.loadUrl(serverURL);
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

    @Override public void onDestroy() {
        Log.d(TAG + ".onDestroy()", "onDestroy");
        super.onDestroy();
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, false, this.getClass().getName());
        pinDialog.show(getFragmentManager(), PinDialogFragment.TAG);
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
        if(webView.canGoBack()) webView.goBack();
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
            webView.loadUrl(jsOperation);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_in));
            progressContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

}