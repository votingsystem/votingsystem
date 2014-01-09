package org.votingsystem.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.votingsystem.android.R;
import org.votingsystem.android.ui.JavaScriptInterface;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EditorFragment extends Fragment {
	
	public static final String TAG = "EditorFragment";
	
	private WebView webView;
	private JavaScriptInterface javaScriptInterface;
    private FrameLayout mainLayout;
    private View rootView;
    private String editorDataStr = "";
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.editor_fragment, container, false);
        webView = (WebView) rootView.findViewById(R.id.webview);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) editorDataStr =
                savedInstanceState.getString(ContextVS.FORM_DATA_KEY);
        loadEditor();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.FORM_DATA_KEY, editorDataStr);
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart(...) ", "onStart");
        super.onStart();
    }

    private void loadEditor() {
        javaScriptInterface = new JavaScriptInterface(this);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        //webSettings.setBuiltInZoomControls(true);
        //webSettings.setSupportZoom(true);

        //webSettings.setUseWideViewPort(true);
        //webSettings.setLoadWithOverviewMode(true);

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
                String functionStr = "javascript:setEditorContent('" + editorDataStr + "')";
                webView.loadUrl(functionStr);
                rootView.invalidate();
            }
        });
        String editorFileName = "editor_" + Locale.getDefault().getLanguage().toLowerCase() + ".html";
        try {
            if(!Arrays.asList(getResources().getAssets().list("")).contains(editorFileName)) {
                Log.d(TAG + ".loadHTMLContent(...)", "missing editorFileName: " + editorFileName);
                editorFileName = "editor_es.html";
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        webView.loadUrl("file:///android_asset/" + editorFileName);
    }

    //This is for JavaScriptInterface.java operation processing
    public void processOperation(OperationVS operationVS) {
        try {
            if (ResponseVS.SC_ERROR == operationVS.getStatusCode()) {
                Log.d(TAG + ".processOperation(...) ", "statusCode: " +operationVS.getStatusCode());
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        operationVS.getMessage());
            } else if (ResponseVS.SC_PAUSED == operationVS.getStatusCode()) {
                editorDataStr = operationVS.getMessage();
            } else if(ResponseVS.SC_OK == operationVS.getStatusCode()) {
                editorDataStr = operationVS.getMessage();
                countDownLatch.countDown();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isEditorDataEmpty() {
        return (editorDataStr == null || TextUtils.isEmpty(editorDataStr));
    }

    private String getEditorDataStr() {
        return editorDataStr;
    }

    public String getEditorData() {
        //if(editorDataStr == null || TextUtils.isEmpty(editorDataStr))
        //    throw new Exception ("Editor data null");
        //webView.loadUrl("javascript:submitForm()");
        //countDownLatch.await();
        return getEditorDataStr();
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }
    
    public void onBackPressed() {
        Log.d(TAG + ".onBackPressed(...)", "onBackPressed");
        if(webView.canGoBack()) webView.goBack();
    }


}