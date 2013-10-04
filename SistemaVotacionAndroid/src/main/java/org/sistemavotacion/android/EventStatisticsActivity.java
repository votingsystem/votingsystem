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
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.util.ServerPaths;


public class EventStatisticsActivity extends ActionBarActivity {
	
	public static final String TAG = "EventStatisticsActivity";

	public static final String EVENT_URL_KEY      = "eventURL";

    private Evento evento =  null;
	private static WebView svWebView;
    private AppData appData;

    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_statistics_activity);
        String eventStatisticsURL = getIntent().getStringExtra(EVENT_URL_KEY);
        appData = AppData.getInstance(getBaseContext());
        if(eventStatisticsURL == null) {
            evento = appData.getEvent();
            if(evento == null) return;
            eventStatisticsURL = ServerPaths.getURLStatistics(evento);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(evento.getAsunto());
        getSupportActionBar().setLogo(android.R.drawable.ic_dialog_info);
        mainLayout = (FrameLayout) findViewById( R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha( 0);
        isProgressShown = false;
        isDestroyed = false;
        Log.d(TAG + ".onCreate(...) ", " - opening: " + eventStatisticsURL);
        showProgress(true, true);
        loadUrl(eventStatisticsURL);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    };

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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
    }

    private void loadUrl(String serverURL) {
    	Log.d(TAG + ".serverURL(...)", " - serverURL: " + serverURL);
        svWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = svWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        //webSettings.setLoadWithOverviewMode(true);
        svWebView.setClickable(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        svWebView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                showProgress(false, true);
            }
        });
        svWebView.loadUrl(serverURL);
    }

}