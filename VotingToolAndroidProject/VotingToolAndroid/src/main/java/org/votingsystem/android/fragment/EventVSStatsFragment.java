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
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;


public class EventVSStatsFragment extends Fragment {
	
	public static final String TAG = EventVSStatsFragment.class.getSimpleName();

    private View rootView;
    private EventVS eventVS;
    private AppContextVS contextVS;
    private String htmlContent;
    private String baseURL;

    public static EventVSStatsFragment newInstance(Long eventId) {
        EventVSStatsFragment fragment = new EventVSStatsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.ITEM_ID_KEY, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        Long eventId =  getArguments().getLong(ContextVS.ITEM_ID_KEY);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        Cursor cursor = getActivity().getContentResolver().query(
                EventVSContentProvider.getEventURI(eventId), null, null, null, null);
        cursor.moveToFirst();
        String eventJSONData = cursor.getString(cursor.getColumnIndex(
                EventVSContentProvider.JSON_DATA_COL));
        LOGD(TAG + ".onCreateView", "eventJSONData: " + eventJSONData);
        try {
            eventVS = EventVS.parse(new JSONObject(eventJSONData));
            eventVS.setAccessControlVS(contextVS.getAccessControl());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.eventvs_stats, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        //LOGD(TAG +  ".onActivityCreated", "savedInstanceState: " + savedInstanceState);
        LOGD(TAG +  ".onActivityCreated", "");
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            htmlContent = savedInstanceState.getString(ContextVS.MESSAGE_KEY);
            baseURL = savedInstanceState.getString(ContextVS.URL_KEY);
            if(htmlContent != null && baseURL != null) loadHTMLContent(baseURL, htmlContent);
        }
        if(htmlContent == null || baseURL == null) {
            GetDataTask getDataTask = new GetDataTask(null);
            getDataTask.execute(eventVS.getURLstats());
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
    };

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.MESSAGE_KEY, htmlContent);
        outState.putString(ContextVS.URL_KEY, baseURL);
        //LOGD(TAG +  ".onSaveInstanceState", "outState: " + outState);
        LOGD(TAG +  ".onSaveInstanceState", "");
    }

    private void loadUrl(String serverURL) {
    	LOGD(TAG + ".loadUrl", " - serverURL: " + serverURL);
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
                setProgressDialogVisible(false);
            }
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
                handler.proceed();//for SSL self-signed certs
            }
        });
        webview.loadUrl(serverURL);
    }

    private void loadHTMLContent(String baseURL, String htmlContent) {
        WebView webview = (WebView) rootView.findViewById(R.id.webview);
        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
                handler.proceed();//for SSL self-signed certs
            }
        });
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadDataWithBaseURL(baseURL, htmlContent, "text/html", "UTF-8", "");
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        }  else ProgressDialogFragment.hide(getFragmentManager());
    }

    public class GetDataTask extends AsyncTask<String, Void, ResponseVS> {

        private ContentTypeVS contentType = null;

        public GetDataTask(ContentTypeVS contentType) { }

        @Override protected void onPreExecute() {
            setProgressDialogVisible(true);
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            baseURL = urls[0];
            LOGD(TAG + "GetDataTask.doInBackground", "baseURL: " + baseURL);
            return  HttpHelper.getData(urls[0], ContentTypeVS.HTML);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            LOGD(TAG + "GetDataTask.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            setProgressDialogVisible(false);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                htmlContent = responseVS.getMessage();
                loadHTMLContent(baseURL, htmlContent);
            } else if(ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                MessageDialogFragment.showDialog(responseVS, getFragmentManager());
            }
        }
    }
}