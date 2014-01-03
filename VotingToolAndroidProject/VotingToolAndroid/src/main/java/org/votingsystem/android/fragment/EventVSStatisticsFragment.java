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

package org.votingsystem.android.fragment;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;

import java.util.concurrent.atomic.AtomicBoolean;


public class EventVSStatisticsFragment extends Fragment {
	
	public static final String TAG = "EventVSStatisticsFragment";

    private View rootView;
    private EventVS eventVS;
    private ContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String htmlContent;
    private String baseURL;

    public static EventVSStatisticsFragment newInstance(Long eventId) {
        EventVSStatisticsFragment fragment = new EventVSStatisticsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.ITEM_ID_KEY, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        Long eventId =  getArguments().getLong(ContextVS.ITEM_ID_KEY);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                EventVSContentProvider.getEventURI(eventId), null, null, null, null);
        cursor.moveToFirst();
        String eventJSONData = cursor.getString(cursor.getColumnIndex(
                EventVSContentProvider.JSON_DATA_COL));
        Log.d(TAG + ".onCreateView(...)", "eventJSONData: " + eventJSONData);
        try {
            eventVS = EventVS.parse(new JSONObject(eventJSONData));
            eventVS.setAccessControlVS(contextVS.getAccessControl());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.eventvs_statistics_fragment, container, false);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        //Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        Log.d(TAG +  ".onActivityCreated(...)", "");
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) showProgress(true, true);
            htmlContent = savedInstanceState.getString(ContextVS.MESSAGE_KEY);
            baseURL = savedInstanceState.getString(ContextVS.URL_KEY);
            if(htmlContent != null && baseURL != null) loadHTMLContent(baseURL, htmlContent);
        }
        if(htmlContent == null || baseURL == null) {
            GetDataTask getDataTask = new GetDataTask(null);
            getDataTask.execute(eventVS.getURLStatistics());
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
    };

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        outState.putString(ContextVS.MESSAGE_KEY, htmlContent);
        outState.putString(ContextVS.URL_KEY, baseURL);
        //Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
        Log.d(TAG +  ".onSaveInstanceState(...)", "");
    }

    private void loadUrl(String serverURL) {
    	Log.d(TAG + ".serverURL(...)", " - serverURL: " + serverURL);
        WebView webview = (WebView) rootView.findViewById(R.id.webview);
        WebSettings webSettings = webview.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webview.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webview.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                showProgress(false, true);
            }
        });
        webview.loadUrl(serverURL);
    }

    private void loadHTMLContent(String baseURL, String htmlContent) {
        WebView webview = (WebView) rootView.findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadDataWithBaseURL(baseURL, htmlContent, "text/html", "UTF-8", "");
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    public class GetDataTask extends AsyncTask<String, Void, ResponseVS> {

        private ContentTypeVS contentType = null;

        public GetDataTask(ContentTypeVS contentType) { }

        @Override protected void onPreExecute() {
            showProgress(true, true);
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            baseURL = urls[0];
            Log.d(TAG + "GetDataTask.doInBackground", "baseURL: " + baseURL);
            return  HttpHelper.getData(urls[0], ContentTypeVS.HTML);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + "GetDataTask.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                htmlContent = responseVS.getMessage();
                loadHTMLContent(baseURL, htmlContent);
            } else if(ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                showMessage(responseVS.getStatusCode(), getString(R.string.operation_error_msg),
                        responseVS.getMessage());
            }
            showProgress(false, true);
        }
    }
}