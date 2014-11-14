package org.votingsystem.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.util.ResponseVS;

import java.util.Arrays;
import java.util.Locale;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EditorFragment extends Fragment {
	
	public static final String TAG = EditorFragment.class.getSimpleName();
	
	private WebView webView;
    private FrameLayout mainLayout;
    private TextView textview;
    private View rootView;
    private String editorDataStr = "";
    private boolean isEditable = true;
    private boolean isEditorLoaded = false;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.editor_fragment, container, false);
        webView = (WebView) rootView.findViewById(R.id.webview);
        textview = (TextView) rootView.findViewById(R.id.textview);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        LOGD(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            editorDataStr = savedInstanceState.getString(ContextVS.FORM_DATA_KEY);
            isEditable = savedInstanceState.getBoolean(ContextVS.EDITOR_VISIBLE_KEY, true);
        }
        loadEditor();
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.FORM_DATA_KEY, editorDataStr);
        outState.putBoolean(ContextVS.EDITOR_VISIBLE_KEY, isEditable);
        LOGD(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "onCreate: " + savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override public void onStart() {
        LOGD(TAG + ".onStart", "onStart");
        super.onStart();
    }

    private void loadEditor() {
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
        webView.addJavascriptInterface(this, "androidClientTool");
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                String functionStr = "javascript:setEditorContent('" + editorDataStr + "')";
                webView.loadUrl(functionStr);
                if(!isEditable) setEditable(isEditable);
                isEditorLoaded = true;
                rootView.invalidate();
            }
        });
        String editorFileName = "editor_" + Locale.getDefault().getLanguage().toLowerCase() + ".html";
        try {
            if(!Arrays.asList(getResources().getAssets().list("")).contains(editorFileName)) {
                LOGD(TAG + ".loadEditor(...)", "missing editorFileName: " + editorFileName);
                editorFileName = "editor_es.html";
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        webView.loadUrl("file:///android_asset/" + editorFileName);
    }

    @JavascriptInterface public void setJSONMessageToSignatureClient (String appMessage) {
        try {
            OperationVS operationVS = OperationVS.parse(appMessage);
            processOperation(operationVS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //This is for JavaScriptInterface.java operation processing
    public void processOperation(OperationVS operationVS) {
        try {
            if (ResponseVS.SC_ERROR == operationVS.getStatusCode()) {
                LOGD(TAG + ".processOperation", "statusCode: " +operationVS.getStatusCode());
                showMessage(ResponseVS.SC_ERROR, getActivity().getString(R.string.error_lbl),
                        operationVS.getMessage());
            } else if (ResponseVS.SC_PAUSED == operationVS.getStatusCode()) {
                if(isEditable) editorDataStr = operationVS.getMessage();
            } else if(ResponseVS.SC_OK == operationVS.getStatusCode()) {
                editorDataStr = operationVS.getMessage();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isEditorDataEmpty() {
        return (editorDataStr == null || TextUtils.isEmpty(editorDataStr));
    }

    public void setEditorData(String editorData) {
        if(isEditorLoaded)  webView.loadUrl("javascript:setEditorContent('" + editorData + "')");
        else editorDataStr = editorData;
    }

    public String getEditorData() {
        return editorDataStr;
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }
    
    public void onBackPressed() {
        LOGD(TAG + ".onBackPressed(...)", "onBackPressed");
        if(webView.canGoBack()) webView.goBack();
    }

    public void setEditable(boolean editable) {
        LOGD(TAG + ".setEditable(...)", " - editable: " + editable);
        String functionStr = null;
        if(editable) {
            functionStr = "javascript:createEditor('" + editorDataStr + "')";
            webView.loadUrl(functionStr);
        } else {
            functionStr = "javascript:removeEditor()";
            webView.loadUrl(functionStr);
        }
        this.isEditable = editable;
    }

}