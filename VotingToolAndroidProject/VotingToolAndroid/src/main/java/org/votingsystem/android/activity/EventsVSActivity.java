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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventVSGridFragment;
import org.votingsystem.android.fragment.PublishEventVSFragment;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventsVSActivity extends ActivityBase
        implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String TAG = EventsVSActivity.class.getSimpleName();

    WeakReference<EventVSGridFragment> weakRefToFragment;
    private boolean mSpinnerConfigured = false;
    private AppContextVS contextVS = null;
    private Menu mainMenu;
    private String broadCastId = EventsVSActivity.class.getSimpleName();


    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vs);
        getLPreviewUtils().trySetActionBar();
        contextVS = (AppContextVS) getApplicationContext();
        EventVSGridFragment fragment = new EventVSGridFragment();
        weakRefToFragment = new WeakReference<EventVSGridFragment>(fragment);
        Bundle args = getIntent().getExtras();
        if(args == null) args = new Bundle();
        args.putSerializable(ContextVS.TYPEVS_KEY, NavigatorDrawerOptionsAdapter.GroupPosition.VOTING);
        args.putSerializable(ContextVS.EVENT_STATE_KEY, EventVS.State.ACTIVE);
        args.putSerializable(ContextVS.CHILD_POSITION_KEY, NavigatorDrawerOptionsAdapter.ChildPosition.OPEN);
        fragment.setArguments(getIntent().getExtras());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment,
                ((Object) fragment).getClass().getSimpleName()).commit();

        View spinnerContainer = LayoutInflater.from(getActionBar().getThemedContext())
                .inflate(R.layout.actionbar_spinner, null);
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
                if(position == 0) requestDataRefresh(EventVS.State.ACTIVE,
                        NavigatorDrawerOptionsAdapter.GroupPosition.VOTING);
                else if(position == 1) requestDataRefresh(EventVS.State.PENDING,
                        NavigatorDrawerOptionsAdapter.GroupPosition.VOTING);
                else if(position == 2) requestDataRefresh(EventVS.State.CANCELLED,
                        NavigatorDrawerOptionsAdapter.GroupPosition.VOTING);
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getActionBar().setCustomView(spinnerContainer, lp);
        mSpinnerConfigured = true;
        updateActionBarNavigation();

        getActionBar().setLogo(UIUtils.getLogoIcon(this, R.drawable.poll_32));
        getActionBar().setSubtitle(getString(R.string.polls_lbl));
        PrefUtils.registerPreferenceChangeListener(this, this);
        if(!PrefUtils.isDataBootstrapDone(this)) {
            refreshingStateChanged(true);
        }
    }

    private void updateActionBarNavigation() {
        boolean show = mSpinnerConfigured && !isNavDrawerOpen();
        ActionBar ab = getActionBar();
        if (show) {
            ab.setDisplayShowCustomEnabled(true);
            ab.setDisplayShowTitleEnabled(false);
            ab.setDisplayUseLogoEnabled(false);
        } else if (getLPreviewUtils().shouldChangeActionBarForDrawer()) {
            ab.setDisplayShowCustomEnabled(false);
            ab.setDisplayShowTitleEnabled(false);
            ab.setDisplayUseLogoEnabled(true);
        }
    }

    public void setTitle(String title, String subTitle, Integer iconId) {
        getActionBar().setTitle(title);
        if(subTitle != null) getActionBar().setSubtitle(subTitle);
        if(iconId != null) getActionBar().setLogo(iconId);
    }

    @Override protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Representatives mode.
        return NAVDRAWER_ITEM_POLLS;
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
        EventVSGridFragment fragment = weakRefToFragment.get();
        fragment.fetchItems(fragment.getOffset());
    }

    public void requestDataRefresh(EventVS.State eventState, NavigatorDrawerOptionsAdapter.GroupPosition groupPosition) {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing - eventState: " +
                eventState.toString() + " - groupPosition: " + groupPosition.toString());
        EventVSGridFragment fragment = weakRefToFragment.get();
        fragment.fetchItems(eventState, groupPosition);
    }


    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        this.mainMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_eventsvs, menu);
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
        LOGD(TAG + ".onOptionsItemSelected",
                " - Title: " + item.getTitle() + " - ItemId: " + item.getItemId());
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.search_item:
                onSearchRequested();
                return true;
            case R.id.publish_document:
                intent = new Intent(EventsVSActivity.this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, PublishEventVSFragment.class.getName());
                intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VOTING_PUBLISHING);
                startActivity(intent);
                //showPublishDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    };

    private void showPublishDialog(){
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.publish_document_lbl).
                setIcon(R.drawable.view_detailed_32).setItems(R.array.publish_options, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(EventsVSActivity.this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, PublishEventVSFragment.class.getName());
                switch (which) {
                    case 0:
                        intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VOTING_PUBLISHING);
                        break;
                    case 1:
                        intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.MANIFEST_PUBLISHING);
                        break;
                    case 2:
                        intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.CLAIM_PUBLISHING);
                        break;
                }
                startActivity(intent);
            }
        }).show();
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }


    private void toggleWebSocketServiceConnection() {
        LOGD(TAG + ".toggleWebSocketServiceConnection", "toggleWebSocketServiceConnection");
        Intent startIntent = new Intent(contextVS, WebSocketService.class);
        TypeVS typeVS;
        if(contextVS.getWebSocketSession() != null) typeVS = TypeVS.WEB_SOCKET_CLOSE;
        else typeVS = TypeVS.WEB_SOCKET_INIT;
        startIntent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        startService(startIntent);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterPreferenceChangeListener(this, this);
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
                view = getLayoutInflater().inflate(R.layout.eventvs_spinner_item_dropdown,
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
                                ? R.layout.eventvs_spinner_item_actionbar
                                : R.layout.eventvs_spinner_item,
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, "onSharedPreferenceChanged - key: " + key);
        if(ContextVS.BOOTSTRAP_DONE.equals(key)) {
            refreshingStateChanged(false);
            requestDataRefresh();
        }
    }
}