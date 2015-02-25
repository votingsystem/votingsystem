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

package org.votingsystem.android.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventVSGridFragment;
import org.votingsystem.android.fragment.ProgressDialogFragment;
import org.votingsystem.android.service.EventVSService;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSMainActivity extends ActivityBase {

	public static final String TAG = EventVSMainActivity.class.getSimpleName();

    WeakReference<EventVSGridFragment> eventVSGridRef;
    private boolean mSpinnerConfigured = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        Bundle args = getIntent().getExtras();
        EventVSGridFragment fragment = new EventVSGridFragment();
        eventVSGridRef = new WeakReference<EventVSGridFragment>(fragment);
        if(args == null) args = new Bundle();
        args.putSerializable(ContextVS.EVENT_STATE_KEY, EventVS.State.ACTIVE);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object) fragment).getClass().getSimpleName()).commit();
        View spinnerContainer = LayoutInflater.from(this).inflate(
                R.layout.spinner_eventvs_actionbar, null);
        EventVSSpinnerAdapter mTopLevelSpinnerAdapter = new EventVSSpinnerAdapter(true);
        mTopLevelSpinnerAdapter.clear();
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.polls_lbl) + " " +
                getString(R.string.open_voting_lbl), false, 0);
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.polls_lbl) + " " +
                getString(R.string.pending_voting_lbl), false, 0);
        mTopLevelSpinnerAdapter.addItem("", getString(R.string.polls_lbl) + " " +
                getString(R.string.closed_voting_lbl), false, 0);
        Spinner spinner = (Spinner) spinnerContainer.findViewById(R.id.actionbar_spinner);
        spinner.setAdapter(mTopLevelSpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> spinner, View view, int position, long itemId) {
                LOGD(TAG + ".onItemSelected", "position:" + position);
                if(position == 0) requestDataRefresh(EventVS.State.ACTIVE);
                else if(position == 1) requestDataRefresh(EventVS.State.PENDING);
                else if(position == 2) requestDataRefresh(EventVS.State.TERMINATED);
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mSpinnerConfigured = true;
        updateActionBarNavigation();
        getSupportActionBar().setCustomView(spinnerContainer, lp);
        getSupportActionBar().setLogo(UIUtils.getLogoIcon(this, R.drawable.mail_mark_unread_32));
        getSupportActionBar().setSubtitle(getString(R.string.polls_lbl));
        if(args != null && args.getString(SearchManager.QUERY) != null) {
            String queryStr = getIntent().getExtras().getString(SearchManager.QUERY);
            Bundle bundled = getIntent().getBundleExtra(SearchManager.APP_DATA);
            Intent startIntent = new Intent(this, EventVSService.class);
            startIntent.putExtra(ContextVS.STATE_KEY, bundled.getSerializable(ContextVS.STATE_KEY));
            startIntent.putExtra(ContextVS.QUERY_KEY, queryStr);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VOTING_EVENT);
            startIntent.putExtra(ContextVS.OFFSET_KEY, 0L);
            startService(startIntent);
        }
    }

    private void updateActionBarNavigation() {
        boolean show = mSpinnerConfigured && !isNavDrawerOpen();
        if (show) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayUseLogoEnabled(false);
        } else {
            getSupportActionBar().setDisplayShowCustomEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }
    }

    public void setTitle(String title, String subTitle, Integer iconId) {
        getSupportActionBar().setTitle(title);
        if(subTitle != null) getSupportActionBar().setSubtitle(subTitle);
        if(iconId != null) getSupportActionBar().setLogo(iconId);
    }

    @Override protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Representatives mode.
        return NAVDRAWER_ITEM_POLLS;
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
        EventVSGridFragment fragment = eventVSGridRef.get();
        fragment.fetchItems(fragment.getOffset());
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }
    public void requestDataRefresh(EventVS.State eventState) {
        LOGD(TAG + ".requestDataRefresh", "eventState: " + eventState.toString());
        EventVSGridFragment fragment = eventVSGridRef.get();
        fragment.fetchItems(eventState);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.eventvs_grid, menu);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ||
        //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { }
        double diagonalInches = UIUtils.getDiagonalInches(getWindowManager().getDefaultDisplay());
        if(diagonalInches < 4) {
            //2 -> index of publish documents menu item on main.xml
            menu.getItem(2).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.search_item:
                onSearchRequested();
                return true;
            case R.id.publish_document:
                intent = new Intent(this, EventVSNewActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    };

    @Override public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        EventVSGridFragment fragment = eventVSGridRef.get();
        appData.putSerializable(ContextVS.STATE_KEY, fragment.getState());
        startSearch(null, false, appData, false);
        return true;
    }

    private class EventVSSpinnerItem {
        boolean isHeader;
        String tag, title;
        int color;
        boolean indented;

        EventVSSpinnerItem(boolean isHeader, String tag, String title, boolean indented, int color) {
            this.isHeader = isHeader;
            this.tag = tag;
            this.title = title;
            this.indented = indented;
            this.color = color;
        }
    }

    /** Adapter that provides views for our top-level Action Bar spinner. */
    private class EventVSSpinnerAdapter extends BaseAdapter {

        private boolean mTopLevel;

        private EventVSSpinnerAdapter(boolean topLevel) {
            this.mTopLevel = topLevel;
        }

        // pairs of (tag, title)
        private ArrayList<EventVSSpinnerItem> mItems = new ArrayList<EventVSSpinnerItem>();

        public void clear() {
            mItems.clear();
        }

        public void addItem(String tag, String title, boolean indented, int color) {
            mItems.add(new EventVSSpinnerItem(false, tag, title, indented, color));
        }

        public void addHeader(String title) {
            mItems.add(new EventVSSpinnerItem(true, "", title, false, 0));
        }

        @Override public int getCount() {
            return mItems.size();
        }

        @Override public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        private boolean isHeader(int position) {
            return position >= 0 && position < mItems.size()
                    && mItems.get(position).isHeader;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent) {
            if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
                view = getLayoutInflater().inflate(R.layout.spinner_eventvs_item_dropdown,
                        parent, false);
                view.setTag("DROPDOWN");
            }

            TextView headerTextView = (TextView) view.findViewById(R.id.header_text);
            View dividerView = view.findViewById(R.id.divider_view);
            TextView normalTextView = (TextView) view.findViewById(R.id.normal_text);

            if (isHeader(position)) {
                headerTextView.setText(getTitle(position));
                headerTextView.setVisibility(View.VISIBLE);
                normalTextView.setVisibility(View.GONE);
                dividerView.setVisibility(View.VISIBLE);
            } else {
                headerTextView.setVisibility(View.GONE);
                normalTextView.setVisibility(View.VISIBLE);
                dividerView.setVisibility(View.GONE);
                normalTextView.setText(getTitle(position));
            }
            return view;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null || !view.getTag().toString().equals("NON_DROPDOWN")) {
                view = getLayoutInflater().inflate(mTopLevel
                                ? R.layout.spinner_eventvs_item_actionbar
                                : R.layout.spinner_eventvs_item,
                        parent, false);
                view.setTag("NON_DROPDOWN");
            }
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(getTitle(position));
            return view;
        }

        private String getTitle(int position) {
            return position >= 0 && position < mItems.size() ? mItems.get(position).title : "";
        }

        private int getColor(int position) {
            return position >= 0 && position < mItems.size() ? mItems.get(position).color : R.color.bkg_vs;
        }

        private String getTag(int position) {
            return position >= 0 && position < mItems.size() ? mItems.get(position).tag : "";
        }

        @Override
        public boolean isEnabled(int position) {
            return !isHeader(position);
        }

        @Override public int getItemViewType(int position) {
            return 0;
        }

        @Override  public int getViewTypeCount() {
            return 1;
        }

        @Override public boolean areAllItemsEnabled() {
            return false;
        }
    }

}