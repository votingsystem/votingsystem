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

package org.votingsystem.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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

import org.votingsystem.android.model.ContextVSAndroid;
import org.votingsystem.android.ui.VoteReceiptListActivity;
import org.votingsystem.android.model.OperationVSAndroid;
import org.votingsystem.android.util.EventState;
import org.votingsystem.util.ScreenUtils;
import org.votingsystem.android.util.SubSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class NavigationDrawer extends ActionBarActivity {

    public static final String TAG = "NavigationDrawer";

    private static final int VOTING_GROUP_POSITION   = 0;
    private static final int MANIFEST_GROUP_POSITION = 1;
    private static final int CLAIM_GROUP_POSITION    = 2;

    private static final int OPEN_CHILD_POSITION    = 0;
    private static final int PENDING_CHILD_POSITION = 1;
    private static final int CLOSED_CHILD_POSITION  = 2;

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;

    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private ActionBarDrawerToggle mDrawerToggle;

    private ContextVSAndroid contextVSAndroid = null;
    private String searchQuery = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate()", " - onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        SubsystemPagerAdapter subsystemPagerAdapter = new SubsystemPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(subsystemPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                Log.d(TAG +  ".mViewPager - ", " - position: " + position);
                switch(position) {
                    case OPEN_CHILD_POSITION:
                        contextVSAndroid.setNavigationDrawerEventState(EventState.OPEN);
                        break;
                    case PENDING_CHILD_POSITION:
                        contextVSAndroid.setNavigationDrawerEventState(EventState.PENDING);
                        break;
                    case CLOSED_CHILD_POSITION:
                        contextVSAndroid.setNavigationDrawerEventState(EventState.CLOSED);
                        break;
                }
                updateActionBarTitle();
            }
        });
        contextVSAndroid = ContextVSAndroid.getInstance(getBaseContext());
        expListView = (ExpandableListView) findViewById(R.id.left_drawer);
        prepareListData();
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
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

            @Override public void onGroupExpand(int groupPosition) {
                Log.d(TAG +  ".expListView - GroupExpandListener", " - Expanded group " +
                        listDataHeader.get(groupPosition));
            }
        });

        expListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

            @Override public void onGroupCollapse(int groupPosition) {
                Log.d(TAG +  ".expListView - GroupExpandListener", " - Collapsed group " +
                        listDataHeader.get(groupPosition));
            }
        });

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                Log.d(TAG +  ".expListView - ChildClickListener", " - " +
                        listDataHeader.get(groupPosition) + " - : " +
                        listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition));
                searchQuery = null;
                SubSystem selectedSubSystem = null;
                EventState selectedEventState = null;
                switch(groupPosition) {
                    case VOTING_GROUP_POSITION:
                        selectedSubSystem = SubSystem.VOTING;
                        break;
                    case MANIFEST_GROUP_POSITION:
                        selectedSubSystem = SubSystem.MANIFESTS;
                        break;
                    case CLAIM_GROUP_POSITION:
                        selectedSubSystem = SubSystem.CLAIMS;
                        break;
                }
                switch(childPosition) {
                    case OPEN_CHILD_POSITION:
                        selectedEventState = EventState.OPEN;
                        break;
                    case PENDING_CHILD_POSITION:
                        selectedEventState = EventState.PENDING;
                        break;
                    case CLOSED_CHILD_POSITION:
                        selectedEventState = EventState.CLOSED;
                        break;
                }
                selectItem(selectedSubSystem, selectedEventState);
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
                getSupportActionBar().setTitle(
                        contextVSAndroid.getSelectedSubsystem().getDescription(getBaseContext()));
                getSupportActionBar().setSubtitle(
                        contextVSAndroid.getNavigationDrawerEventState().getDescription(
                        contextVSAndroid.getSelectedSubsystem(), getBaseContext()));
                updateActionbarLogo();
                ActivityCompat.invalidateOptionsMenu(NavigationDrawer.this);
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setLogo(R.drawable.password_22x22);
                getSupportActionBar().setTitle(getString(R.string.voting_system_lbl));
                getSupportActionBar().setSubtitle(null);
                ActivityCompat.invalidateOptionsMenu(NavigationDrawer.this);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
        } else searchQuery = null;
        selectItem(contextVSAndroid.getSelectedSubsystem(), contextVSAndroid.getNavigationDrawerEventState());
    }

    private void updateActionBarTitle() {
        getSupportActionBar().setTitle(contextVSAndroid.getSelectedSubsystem().getDescription(getBaseContext()));
        getSupportActionBar().setSubtitle(contextVSAndroid.getNavigationDrawerEventState().getDescription(
                contextVSAndroid.getSelectedSubsystem(), getBaseContext()));
        updateActionbarLogo();
    }

    private void updateActionbarLogo() {
        switch (contextVSAndroid.getSelectedSubsystem()) {
            case CLAIMS:
                getSupportActionBar().setLogo(R.drawable.filenew_22);
                break;
            case MANIFESTS:
                getSupportActionBar().setLogo(R.drawable.manifest_22);
                break;
            case VOTING:
                getSupportActionBar().setLogo(R.drawable.poll_22);
                break;
            default:
                getSupportActionBar().setLogo(R.drawable.password_22x22);
        }
    }

    @Override public void onSaveInstanceState(Bundle savedInstanceState) { }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG + ".onRestoreInstanceState(...) ", " - onRestoreInstanceState");
    }


    private void selectItem(SubSystem subSystem, EventState eventState) {
        Log.d(TAG + ".selectItem()", " - subSystem: " + subSystem + " - eventState: " + eventState +
             " - eventPosition: " +  eventState.getposition() + " - searchQuery: " + searchQuery);
        // update the main content by replacing fragments
        /*Fragment fragment = new EventListFragment();
        Bundle args = new Bundle();
        args.putString("eventState", eventState.toString());
        args.putString("subSystem", subSystem.toString());

        fragment.setArguments(args);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.pager, fragment).commit();*/
        boolean subSystemChanged = true;
        SubSystem previousSelectedSubsystem = contextVSAndroid.getSelectedSubsystem();
        if(subSystem == previousSelectedSubsystem) subSystemChanged = false;
        mDrawerLayout.closeDrawer(expListView);
        contextVSAndroid.setSelectedSubsystem(subSystem);
        contextVSAndroid.setNavigationDrawerEventState(eventState);
        updateActionBarTitle();
        if(subSystemChanged) {
            Log.d(TAG +  ".selectItem", " - changing subsystem");
            SubsystemPagerAdapter subsystemPagerAdapter = new SubsystemPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(subsystemPagerAdapter);
        }
        mViewPager.setCurrentItem(eventState.getposition(), true);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG +  ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
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
        Log.d(TAG +  ".onOptionsItemSelected(...)",
                " - Title: " + item.getTitle() + " - ItemId: " + item.getItemId());
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.search_item:
                onSearchRequested();
                return true;
            case R.id.get_cert:
                switch(contextVSAndroid.getEstado()) {
                    case SIN_CSR:
                        startActivity(new Intent(this, MainActivity.class));
                        return true;
                    case CON_CSR:
                        startActivity(new Intent(this, UserCertResponseActivity.class));
                        return true;
                    case CON_CERTIFICADO:
                        AlertDialog.Builder builder= new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.
                                menu_solicitar_certificado));
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
                                R.string.cancelar_button), null);
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
                                intent.putExtra(EventPublishingActivity.FORM_TYPE_KEY,
                                        OperationVSAndroid.Tipo.VOTING_PUBLISHING.toString());
                                break;
                            case 1:
                                intent.putExtra(EventPublishingActivity.FORM_TYPE_KEY,
                                        OperationVSAndroid.Tipo.MANIFEST_PUBLISHING.toString());
                                break;
                            case 2:
                                intent.putExtra(EventPublishingActivity.FORM_TYPE_KEY,
                                        OperationVSAndroid.Tipo.CLAIM_PUBLISHING.toString());
                                break;
                        }
                        startActivity(intent);
                    }
                })
                .create();
        dialog.show();
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        listDataHeader.add(VOTING_GROUP_POSITION, getString(R.string.voting_drop_down_lbl));
        listDataHeader.add(MANIFEST_GROUP_POSITION, getString(R.string.manifiests_drop_down_lbl));
        listDataHeader.add(CLAIM_GROUP_POSITION, getString(R.string.claims_drop_down_lbl));

        List<String> voting = new ArrayList<String>();
        voting.add(OPEN_CHILD_POSITION, getString(R.string.open_voting_lbl));
        voting.add(PENDING_CHILD_POSITION, getString(R.string.pending_voting_lbl));
        voting.add(CLOSED_CHILD_POSITION, getString(R.string.closed_voting_lbl));

        List<String> manifests = new ArrayList<String>();
        manifests.add(OPEN_CHILD_POSITION, getString(R.string.open_manifest_lbl));
        manifests.add(PENDING_CHILD_POSITION, getString(R.string.pending_manifest_lbl));
        manifests.add(CLOSED_CHILD_POSITION, getString(R.string.closed_manifest_lbl));

        List<String> claims = new ArrayList<String>();
        claims.add(OPEN_CHILD_POSITION, getString(R.string.open_claim_lbl));
        claims.add(PENDING_CHILD_POSITION, getString(R.string.pending_claim_lbl));
        claims.add(CLOSED_CHILD_POSITION, getString(R.string.closed_claim_lbl));

        listDataChild.put(listDataHeader.get(VOTING_GROUP_POSITION), voting);
        listDataChild.put(listDataHeader.get(MANIFEST_GROUP_POSITION), manifests);
        listDataChild.put(listDataHeader.get(CLAIM_GROUP_POSITION), claims);
    }

    public class SubsystemPagerAdapter extends FragmentStatePagerAdapter {

        public SubsystemPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override public Fragment getItem(int i) {
            EventState eventState = null;
            switch(i) {
                case OPEN_CHILD_POSITION:
                    eventState = EventState.OPEN;
                    break;
                case PENDING_CHILD_POSITION:
                    eventState = EventState.PENDING;
                    break;
                case CLOSED_CHILD_POSITION:
                    eventState = EventState.CLOSED;
                    break;
            }
            Log.d(TAG + ".SubsystemPagerAdapter.getItem(...) ", " - item: " + i + " - subSystem: " +
                    contextVSAndroid.getSelectedSubsystem() + " - eventState: " + eventState +
                    " - searchQuery: " + searchQuery);
            Bundle args = new Bundle();
            args.putString("subSystem", contextVSAndroid.getSelectedSubsystem().toString());
            args.putString("eventState", eventState.toString());
            args.putString(SearchManager.QUERY, searchQuery);
            Fragment fragment = new EventListFragment();
            fragment.setArguments(args);
            return fragment;
       }

        @Override public int getCount() {
            return 3;//OPEN, PENDING, CLOSED states -> 3
        }
    }

}