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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventPublishingFragment;
import org.votingsystem.android.ui.EventNavigationPagerAdapter;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.ui.PagerAdapterVS;
import org.votingsystem.android.ui.RepresentativeNavigationPagerAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ScreenUtils;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class NavigationDrawer extends ActionBarActivity {

    public static final String TAG = "NavigationDrawer";

    public static final String GROUP_POSITION_KEY = "groupPosition";
    public static final String CHILD_POSITION_KEY = "childPosition";

    private NavigatorDrawerOptionsAdapter listAdapter;
    private ExpandableListView expListView;
    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private ActionBarDrawerToggle mDrawerToggle;
    private ContextVS contextVS = null;

    private RepresentativeNavigationPagerAdapter representativePagerAdapter;
    private EventNavigationPagerAdapter pagerAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new EventNavigationPagerAdapter(getSupportFragmentManager(), mViewPager);
        representativePagerAdapter = new RepresentativeNavigationPagerAdapter(
                getSupportFragmentManager(), mViewPager);

        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                Log.d(TAG +  ".mViewPager.onPageSelected(...)", "position: " + position);
                ((PagerAdapterVS)mViewPager.getAdapter()).updateChildPosition(position);
                updateActionBarTitle();
            }
        });
        contextVS = ContextVS.getInstance(getBaseContext());
        expListView = (ExpandableListView) findViewById(R.id.left_drawer);
        listAdapter = new NavigatorDrawerOptionsAdapter(this);
        expListView.setAdapter(listAdapter);

        expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                //Log.d(TAG +  ".GroupExpandListener", " - onGroupClick - groupPosition: " +
                //        listDataHeader.get(groupPosition));
                return false;
            }
        });

        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override public void onGroupExpand(int groupPosition) { }
        });

        expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override public void onGroupCollapse(int groupPosition) { }
        });

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                selectItem(groupPosition, childPosition);
                return true;
            }
        });

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                updateActionBarTitle();
                ActivityCompat.invalidateOptionsMenu(NavigationDrawer.this);
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setLogo(R.drawable.mail_mark_unread_22);
                getSupportActionBar().setTitle(getString(R.string.voting_system_lbl));
                getSupportActionBar().setSubtitle(null);
                ActivityCompat.invalidateOptionsMenu(NavigationDrawer.this);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
            pagerAdapter.setSearchQuery(searchQuery);
        }
        int  groupPosition = getIntent().getIntExtra(
                NavigatorDrawerOptionsAdapter.GROUP_POSITION_KEY, -1);
        if(groupPosition >= 0) pagerAdapter.selectItem(groupPosition,
                NavigatorDrawerOptionsAdapter.ChildPosition.OPEN.getPosition());
        int selectedGroupPosition = pagerAdapter.getSelectedGroupPosition();
        int selectedChildPosition = pagerAdapter.getSelectedChildPosition();
        if(savedInstanceState != null) {
            selectedGroupPosition = savedInstanceState.getInt(GROUP_POSITION_KEY);
            selectedChildPosition = savedInstanceState.getInt(CHILD_POSITION_KEY);
        }
        selectItem(selectedGroupPosition,selectedChildPosition);
    }

    private void updateActionBarTitle() {
        getSupportActionBar().setTitle(((PagerAdapterVS)mViewPager.getAdapter()).
                getSelectedGroupDescription(this));
        getSupportActionBar().setSubtitle(((PagerAdapterVS)mViewPager.getAdapter()).
                getSelectedChildDescription(this));
        getSupportActionBar().setLogo(((PagerAdapterVS)mViewPager.getAdapter()).getLogo(this));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(GROUP_POSITION_KEY,
                ((PagerAdapterVS)mViewPager.getAdapter()).getSelectedGroupPosition());
        outState.putInt(CHILD_POSITION_KEY,
                ((PagerAdapterVS)mViewPager.getAdapter()).getSelectedChildPosition());
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState:" + outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG + ".onRestoreInstanceState(...) ", "onRestoreInstanceState:" + savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void selectItem(int groupPosition, int childPosition) {
        Log.d(TAG + ".selectItem(...)", "groupPosition: " + groupPosition + " - childPosition: " +
                childPosition);
        mDrawerLayout.closeDrawer(expListView);
        NavigatorDrawerOptionsAdapter.GroupPosition selectedSubsystem =
                NavigatorDrawerOptionsAdapter.GroupPosition.valueOf(groupPosition);

        switch(selectedSubsystem) {
            case VOTING:
            case MANIFESTS:
            case CLAIMS:
                if(mViewPager.getAdapter() instanceof  RepresentativeNavigationPagerAdapter) {
                    mViewPager.setAdapter(pagerAdapter);
                }
                break;
            case REPRESENTATIVES:
                if(mViewPager.getAdapter() instanceof EventNavigationPagerAdapter) {
                    mViewPager.setAdapter(representativePagerAdapter);
                }
                break;
        }
        ((PagerAdapterVS)mViewPager.getAdapter()).selectItem(groupPosition, childPosition);
        updateActionBarTitle();
        mViewPager.setCurrentItem(childPosition, true);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.navigation_drawer, menu);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ||
        //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { }
        double diagonalInches = ScreenUtils.getDiagonalInches(getWindowManager().getDefaultDisplay());
        if(diagonalInches < 4) {
            //2 -> index of publish documents menu item on main.xml
            menu.getItem(2).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...)",
                " - Title: " + item.getTitle() + " - ItemId: " + item.getItemId());
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.search_item:
                onSearchRequested();
                return true;
            case R.id.get_cert:
                switch(contextVS.getState()) {
                    case WITHOUT_CSR:
                        startActivity(new Intent(this, MainActivity.class));
                        return true;
                    case WITH_CSR:
                        startActivity(new Intent(this, UserCertResponseActivity.class));
                        return true;
                    case WITH_CERTIFICATE:
                        AlertDialog.Builder builder= new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.
                                request_certificate_menu));
                        builder.setMessage(Html.fromHtml(
                                getString(R.string.request_cert_again_msg)));
                        builder.setPositiveButton(getString(
                                R.string.ok_button), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(NavigationDrawer.this, UserCertRequestActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
                        builder.setNegativeButton(getString(
                                R.string.cancel_button), null);
                        builder.show();
                }
                return true;
            case R.id.receipt_list:
                startActivity(new Intent(this, VoteReceiptListActivity.class));
                return true;
            case R.id.publish_document:
                showPublishDialog();
                return true;
            case R.id.close_app:
                finish();
                System.exit(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    };

    private void showPublishDialog(){
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.publish_document_lbl).setIcon(R.drawable.view_detailed_32)
                .setItems(R.array.publish_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(NavigationDrawer.this, EventPublishingActivity.class);
                        switch (which) {
                            case 0:
                                intent.putExtra(EventPublishingFragment.FORM_TYPE_KEY,
                                        TypeVS.VOTING_PUBLISHING.toString());
                                break;
                            case 1:
                                intent.putExtra(EventPublishingFragment.FORM_TYPE_KEY,
                                        TypeVS.MANIFEST_PUBLISHING.toString());
                                break;
                            case 2:
                                intent.putExtra(EventPublishingFragment.FORM_TYPE_KEY,
                                        TypeVS.CLAIM_PUBLISHING.toString());
                                break;
                        }
                        startActivity(intent);
                    }
                })
                .create();
        dialog.show();
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }


}