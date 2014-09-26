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
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.PublishEventVSFragment;
import org.votingsystem.android.fragment.VicketGridFragment;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.ui.EventNavigationPagerAdapter;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.ui.PagerAdapterVS;
import org.votingsystem.android.ui.SingleOptionPagerAdapter;
import org.votingsystem.android.ui.VicketPagerAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ScreenUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class NavigationDrawer extends ActivityVS {

    public static final String TAG = NavigationDrawer.class.getSimpleName();

    public static final String GROUP_POSITION_KEY = "groupPosition";
    public static final String CHILD_POSITION_KEY = "childPosition";

    private NavigatorDrawerOptionsAdapter listAdapter;
    private ProgressDialog progressDialog = null;
    private ExpandableListView expListView;
    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private ActionBarDrawerToggle mDrawerToggle;
    private AppContextVS contextVS = null;
    private String broadCastId = NavigationDrawer.class.getSimpleName();
    //private RepresentativeNavigationPagerAdapter representativePagerAdapter;
    private Menu mainMenu;
    private SingleOptionPagerAdapter singleOptionPagerAdapter;
    private EventNavigationPagerAdapter pagerAdapter;
    private VicketPagerAdapter vicketAdapter;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                switch(responseVS.getTypeVS()) {
                    case WEB_SOCKET_INIT:
                        showProgressDialog(getString(R.string.connecting_caption),
                                getString(R.string.connecting_to_service_msg));
                        toggleWebSocketServiceConnection();
                        break;
                }
            } else {
                Log.d(TAG + ".broadcastReceiver.onReceive(...)", "response typeVS: " + responseVS.getTypeVS());
                if(progressDialog != null) progressDialog.dismiss();
                switch(responseVS.getTypeVS()) {
                    case INIT_VALIDATED_SESSION:
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            if(mainMenu != null) {
                                MenuItem connectToServiceMenuItem = mainMenu.findItem(R.id.connect_to_service);
                                connectToServiceMenuItem.setTitle(getString(R.string.disconnect_from_service_lbl));
                            }
                        }
                        break;
                    case WEB_SOCKET_CLOSE:
                        if(mainMenu != null) {
                            MenuItem connectToServiceMenuItem = mainMenu.findItem(R.id.connect_to_service);
                            connectToServiceMenuItem.setTitle(getString(R.string.connect_to_service_lbl));
                        }
                        break;
                }

            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.navigation_drawer);
        initActivityVS((FrameLayout) findViewById(R.id.mainLayout), findViewById(R.id.progressContainer));

        mViewPager = (ViewPager) findViewById(R.id.pager);

        pagerAdapter = new EventNavigationPagerAdapter(getSupportFragmentManager(), mViewPager);
        //representativePagerAdapter = new RepresentativeNavigationPagerAdapter(
        //        getSupportFragmentManager(), mViewPager);
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                Log.d(TAG +  ".mViewPager.onPageSelected(...)", "position: " + position);
                ((PagerAdapterVS)mViewPager.getAdapter()).updateChildPosition(position);
                updateActionBarTitle();
            }
        });
        contextVS = (AppContextVS) getApplicationContext();
        expListView = (ExpandableListView) findViewById(R.id.left_drawer);
        listAdapter = new NavigatorDrawerOptionsAdapter(this);
        expListView.setAdapter(listAdapter);

        expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

            @Override public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                //Log.d(TAG +  ".GroupExpandListener", " - onGroupClick - groupPosition: " +
                //        listDataHeader.get(groupPosition));
                selectItem(groupPosition, null);
                //return true;
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
        int selectedGroupPosition = pagerAdapter.getSelectedGroupPosition();
        int selectedChildPosition = pagerAdapter.getSelectedChildPosition();
        if(savedInstanceState != null) {
            selectedGroupPosition = savedInstanceState.getInt(GROUP_POSITION_KEY);
            selectedChildPosition = savedInstanceState.getInt(CHILD_POSITION_KEY);
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) showProgressDialog(
                    getString(R.string.connecting_caption), getString(R.string.loading_data_msg));
        }
        selectItem(selectedGroupPosition,selectedChildPosition);
    }

    private void updateActionBarTitle() {
        getSupportActionBar().setTitle(((PagerAdapterVS)mViewPager.getAdapter()).
                getSelectedGroupDescription((AppContextVS) this.getApplicationContext()));
        getSupportActionBar().setSubtitle(((PagerAdapterVS)mViewPager.getAdapter()).
                getSelectedChildDescription((AppContextVS) this.getApplicationContext()));
        getSupportActionBar().setLogo(((PagerAdapterVS)mViewPager.getAdapter()).getLogo(
                (AppContextVS) this.getApplicationContext()));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (progressDialog != null && progressDialog.isShowing()) {
            outState.putBoolean(ContextVS.LOADING_KEY, true);
        }
        outState.putInt(GROUP_POSITION_KEY,
                ((PagerAdapterVS) mViewPager.getAdapter()).getSelectedGroupPosition());
        outState.putInt(CHILD_POSITION_KEY,
                ((PagerAdapterVS) mViewPager.getAdapter()).getSelectedChildPosition());
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState:" + outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG + ".onRestoreInstanceState(...)", "onRestoreInstanceState:" + savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void selectItem(Integer groupPosition, Integer childPosition) {
        Log.d(TAG + ".selectItem(...)", "groupPosition: " + groupPosition + " - childPosition: " +
                childPosition);
        NavigatorDrawerOptionsAdapter.GroupPosition selectedSubsystem =
                NavigatorDrawerOptionsAdapter.GroupPosition.valueOf(groupPosition);
        switch(selectedSubsystem) {
            case VOTING:
            case MANIFESTS:
            case CLAIMS:
                if(childPosition == null) return;
                if(!(mViewPager.getAdapter() instanceof  EventNavigationPagerAdapter)) {
                    mViewPager.setAdapter(pagerAdapter);
                }
                break;
            case RECEIPTS:
            case REPRESENTATIVES:
                if(singleOptionPagerAdapter == null) singleOptionPagerAdapter=
                        new SingleOptionPagerAdapter(getSupportFragmentManager(), mViewPager);
                singleOptionPagerAdapter.selectItem(groupPosition, childPosition);
                mViewPager.setAdapter(singleOptionPagerAdapter);
                childPosition = 0;
                /*if(mViewPager.getAdapter() instanceof EventNavigationPagerAdapter) {
                    mViewPager.setAdapter(representativePagerAdapter);
                }*/
                break;
            case VICKETS:
                if(childPosition == null) return;
                if(vicketAdapter == null) vicketAdapter = new VicketPagerAdapter(
                        getSupportFragmentManager(), mViewPager);
                if(!(mViewPager.getAdapter() instanceof VicketPagerAdapter)) {
                    mViewPager.setAdapter(vicketAdapter);
                }
                break;
        }
        mDrawerLayout.closeDrawer(expListView);
        ((PagerAdapterVS)mViewPager.getAdapter()).selectItem(groupPosition, childPosition);
        updateActionBarTitle();
        mViewPager.setCurrentItem(childPosition, true);
    }


    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        mainMenu = menu;
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
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.reload:
                ((FragmentStatePagerAdapter)mViewPager.getAdapter()).notifyDataSetChanged();
                return false;
            case R.id.search_item:
                onSearchRequested();
                return true;
            case R.id.get_cert:
                startActivity(new Intent(this, CertRequestActivity.class));
                return true;
            case R.id.publish_document:
                intent = new Intent(NavigationDrawer.this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, PublishEventVSFragment.class.getName());
                intent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VOTING_PUBLISHING);
                startActivity(intent);
                //showPublishDialog();
                return true;
            case R.id.vickets_menu_user_item:
                intent = new Intent(this, BrowserVSActivity.class);
                intent.putExtra(ContextVS.URL_KEY, contextVS.getVicketServer().getMenuUserURL());
                startActivity(intent);
                return true;
            case R.id.connect_to_service:
                if(contextVS.getWebSocketSessionId() == null) {
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId, getString(
                            R.string.init_authenticated_session_pin_msg), false, TypeVS.WEB_SOCKET_INIT);
                } else {toggleWebSocketServiceConnection();}
                return true;
            case R.id.admin_vickets_menu_item:
                intent = new Intent(this, BrowserVSActivity.class);
                intent.putExtra(ContextVS.URL_KEY, contextVS.getVicketServer().getMenuAdminURL());
                startActivity(intent);
                return true;
            case R.id.close_app:
                finish();
                System.exit(0);
                return true;
            case R.id.open_vicket_grid:
                intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, VicketGridFragment.class.getName());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    };


    private void toggleWebSocketServiceConnection() {
        Log.d(TAG + ".toggleWebSocketServiceConnection(...)", "toggleWebSocketServiceConnection");
        Intent startIntent = new Intent(contextVS, WebSocketService.class);
        TypeVS typeVS;
        if(contextVS.getWebSocketSessionId() != null) typeVS = TypeVS.WEB_SOCKET_CLOSE;
        else typeVS = TypeVS.WEB_SOCKET_INIT;
        startIntent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        startService(startIntent);
    }

    private void showProgressDialog(String title, String dialogMessage) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(true);
            progressDialog.setTitle(title);
            progressDialog.setMessage(dialogMessage);
            progressDialog.setIndeterminate(true);
            /*progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
                @Override public void onCancel(DialogInterface dialog){}});*/
        }
        progressDialog.show();
    }

    private void showPublishDialog(){
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.publish_document_lbl).
                setIcon(R.drawable.view_detailed_32).setItems(R.array.publish_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(NavigationDrawer.this, FragmentContainerActivity.class);
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

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG + ".onActivityResult(...)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}