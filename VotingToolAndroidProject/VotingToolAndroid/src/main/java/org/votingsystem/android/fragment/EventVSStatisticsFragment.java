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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;
import org.votingsystem.android.activity.EventVSPagerActivity;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;


public class EventVSStatisticsFragment extends Fragment {
	
	public static final String TAG = "EventVSStatisticsFragment";

    private int eventIndex;
    private View rootView;
    private EventVS eventVS =  null;
    private ContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean progressVisible;

    public static EventVSStatisticsFragment newInstance(Long eventId) {
        EventVSStatisticsFragment fragment = new EventVSStatisticsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.ITEM_ID_KEY, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", "onCreate");
        Long eventId =  getArguments().getLong(ContextVS.ITEM_ID_KEY);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                EventVSContentProvider.getEventURI(eventId), null, null, null, null);
        cursor.moveToFirst();
        String eventJSONData = cursor.getString(cursor.getColumnIndex(
                EventVSContentProvider.JSON_DATA_COL));
        Log.d(TAG + ".bindView(...)", "cursor.getPosition(): "  + cursor.getPosition() +
                " - eventJSONData: " + eventJSONData);
        try {
            eventVS = EventVS.parse(new JSONObject(eventJSONData));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        rootView = inflater.inflate(R.layout.eventvs_statistics_fragment, container, false);
        String eventStatisticsURL = eventVS.getURLStatistics();
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha( 0);
        progressVisible = false;
        setHasOptionsMenu(true);
        showProgress(true, true);
        loadUrl(eventStatisticsURL);
        return rootView;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
    };

    public void showProgress(boolean shown, boolean animate) {
        if (progressVisible == shown || getActivity() == null) {
            return;
        }
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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
    }

    private void loadUrl(String serverURL) {
    	Log.d(TAG + ".serverURL(...)", " - serverURL: " + serverURL);
        WebView svWebView = (WebView) rootView.findViewById(R.id.webview);
        WebSettings webSettings = svWebView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
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