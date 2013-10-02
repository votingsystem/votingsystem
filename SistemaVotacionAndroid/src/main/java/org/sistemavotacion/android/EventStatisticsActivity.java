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

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class EventStatisticsActivity extends ActionBarActivity {
	
	public static final String TAG = "EventStatisticsActivity";

	public static final String EVENT_URL_KEY      = "eventURL";
    public static final String ACTIVITY_TITLE_KEY = "activityTitle";
	
	private static WebView svWebView;

	private JavaScriptInterface javaScriptInterface;
    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_statistics_activity);
        String eventStatisticsURL = getIntent().getStringExtra(EVENT_URL_KEY);
        String activityTitle = getIntent().getStringExtra(ACTIVITY_TITLE_KEY);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(activityTitle);
        loadUrl(eventStatisticsURL);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    };


    private void loadUrl(String serverURL) {
    	Log.d(TAG + ".serverURL(...)", " - serverURL: " + serverURL);
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

}