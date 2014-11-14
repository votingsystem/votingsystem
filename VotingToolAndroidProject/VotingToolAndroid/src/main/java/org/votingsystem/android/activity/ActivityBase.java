package org.votingsystem.android.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.ui.debug.DebugActionRunnerActivity;
import org.votingsystem.android.ui.widget.MultiSwipeRefreshLayout;
import org.votingsystem.android.ui.widget.SwipeRefreshLayout;
import org.votingsystem.android.util.BuildConfig;
import org.votingsystem.android.util.HelpUtils;
import org.votingsystem.android.util.LPreviewUtils;
import org.votingsystem.android.util.LPreviewUtilsBase;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.WebSocketRequest;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.LOGW;
import static org.votingsystem.android.util.LogUtils.makeLogTag;
import static org.votingsystem.model.ContextVS.USER_KEY;


public abstract class ActivityBase extends FragmentActivity implements ActivityVS,
        SharedPreferences.OnSharedPreferenceChangeListener,
        MultiSwipeRefreshLayout.CanChildScrollUpCallback  {

    private static final String TAG = makeLogTag(ActivityBase.class);

    private ProgressDialog progressDialog = null;
    private AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private DrawerLayout mDrawerLayout;
    private LPreviewUtilsBase.ActionBarDrawerToggleWrapper mDrawerToggle;
    private AppContextVS contextVS = null;
    // allows access to L-Preview APIs through an abstract interface so we can compile with
    // both the L Preview SDK and with the API 19 SDK
    private LPreviewUtilsBase mLPreviewUtils;
    private ObjectAnimator mStatusBarColorAnimator;
    private ViewGroup mDrawerItemsListContainer;

    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();

    // Durations for certain animations we use:
    private static final int HEADER_HIDE_ANIM_DURATION = 300;
    private static final int ACCOUNT_BOX_EXPAND_ANIM_DURATION = 200;

    // symbols for navdrawer items (indices must correspond to array below). This is
    // not a list of items that are necessarily *present* in the Nav Drawer; rather,
    // it's a list of all possible items.
    protected static final int NAVDRAWER_ITEM_POLLS = 0;
    protected static final int NAVDRAWER_ITEM_REPRESENTATIVES = 1;
    protected static final int NAVDRAWER_ITEM_VICKETS = 2;

    protected static final int NAVDRAWER_ITEM_SETTINGS = 3;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;

    // titles for navdrawer items (indices must correspond to the above)
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.polls_lbl,
            R.string.representative_lbl,
            R.string.finance_lbl,
            R.string.navdrawer_item_settings,
    };

    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[] {
            R.drawable.poll_32,
            R.drawable.system_users_32,
            R.drawable.fa_money_32,
            R.drawable.ic_drawer_settings,
    };

    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    private ArrayList<Integer> mNavDrawerItems = new ArrayList<Integer>();
    private View[] mNavDrawerItemViews = null;
    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // handle to our sync observer (that notifies us about changes in our sync state)
    private Object mSyncObserverHandle;
    Thread mDataBootstrapThread = null;

    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = false;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;
    private TextView connectionStatusText;
    private ImageView connectionStatusView;

    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private int mProgressBarTopWhenActionBarShown;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    private String broadCastId = ActivityBase.class.getSimpleName();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            WebSocketRequest request = intent.getParcelableExtra(ContextVS.WEBSOCKET_REQUEST_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
                switch(responseVS.getTypeVS()) {
                    case WEB_SOCKET_INIT:
                        showProgressDialog(getString(R.string.connecting_caption),
                                getString(R.string.connecting_to_service_msg));
                        new Thread(new Runnable() {
                            @Override public void run() { toggleWebSocketServiceConnection(); }
                        }).start();
                        break;
                }
            } else if(request != null) {
                LOGD(TAG + ".broadcastReceiver", "WebSocketRequest typeVS: " + request.getTypeVS());
                if(progressDialog != null) progressDialog.dismiss();
                switch(request.getTypeVS()) {
                    case INIT_VALIDATED_SESSION:
                        if(ResponseVS.SC_OK == request.getStatusCode()) {
                            connectionStatusText.setText(getString(R.string.connected_lbl));
                            connectionStatusView.setVisibility(View.VISIBLE);
                        } else if(ResponseVS.SC_ERROR == request.getStatusCode())
                                ActivityBase.this.showMessage(request.getStatusCode(),
                                request.getCaption(), request.getMessage());
                        break;
                    case WEB_SOCKET_CLOSE:
                        connectionStatusText.setText(getString(R.string.disconnected_lbl));
                        connectionStatusView.setVisibility(View.GONE);
                        break;
                }
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        /*if (!PrefUtils.isTosAccepted(this)) {//Check if the EULA has been accepted; if not, show it.
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }*/
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        PrefUtils.registerPreferenceChangeListener(this, this);
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);
        mLPreviewUtils = LPreviewUtils.getInstance(this);
    }

    private void trySetupSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorScheme(
                    R.color.refresh_progress_1,
                    R.color.refresh_progress_2,
                    R.color.refresh_progress_3,
                    R.color.refresh_progress_4);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    requestDataRefresh();
                }
            });
            if (mSwipeRefreshLayout instanceof MultiSwipeRefreshLayout) {
                MultiSwipeRefreshLayout mswrl = (MultiSwipeRefreshLayout) mSwipeRefreshLayout;
                mswrl.setCanChildScrollUpCallback(this);
            }
        }
    }

    private void updateSwipeRefreshProgressBarTop() {
        if (mSwipeRefreshLayout == null) return;
        if (mActionBarShown) mSwipeRefreshLayout.setProgressBarTop(mProgressBarTopWhenActionBarShown);
        else  mSwipeRefreshLayout.setProgressBarTop(0);
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what nav drawer item corresponds to them
     * Return NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    /**
     * Sets up the navigation drawer as appropriate. Note that the nav drawer will be
     * different depending on whether the attendee indicated that they are attending the
     * event on-site vs. attending remotely.
     */
    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = getSelfNavDrawerItem();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            View navDrawer = mDrawerLayout.findViewById(R.id.navdrawer);
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }
        mDrawerToggle = mLPreviewUtils.setupDrawerToggle(mDrawerLayout, new DrawerLayout.DrawerListener() {
            @Override public void onDrawerClosed(View drawerView) {
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                updateStatusBarForNavDrawerSlide(0f);
                onNavDrawerStateChanged(false, false);
            }
            @Override public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                updateStatusBarForNavDrawerSlide(1f);
                onNavDrawerStateChanged(true, false);
            }
            @Override public void onDrawerStateChanged(int newState) {
                invalidateOptionsMenu();
                onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
            }
            @Override public void onDrawerSlide(View drawerView, float slideOffset) {
                updateStatusBarForNavDrawerSlide(slideOffset);
                onNavDrawerSlide(slideOffset);
            }
        });
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        // populate the nav drawer with the correct items
        populateNavDrawer();
        mDrawerToggle.syncState();
        // When the user runs the app for the first time, we want to land them with the
        // navigation drawer open. But just the first time.
        if (!PrefUtils.isDataBootstrapDone(this)) {
            // first run of the app starts with the nav drawer open
            PrefUtils.isDataBootstrapDone(this);
            mDrawerLayout.openDrawer(Gravity.START);
        }
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (mActionBarAutoHideEnabled && isOpen) {
            autoShowOrHideActionBar(true);
        }
    }

    protected void onNavDrawerSlide(float offset) {}

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START);
    }

    private void populateNavDrawer() {
        mNavDrawerItems.clear();
        mNavDrawerItems.add(NAVDRAWER_ITEM_POLLS);
        mNavDrawerItems.add(NAVDRAWER_ITEM_REPRESENTATIVES);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR);
        mNavDrawerItems.add(NAVDRAWER_ITEM_VICKETS);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR_SPECIAL);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);
        createNavDrawerItems();
    }

    private void createNavDrawerItems() {
        mDrawerItemsListContainer = (ViewGroup) findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }
        connectionStatusText = (TextView) findViewById(R.id.connection_status_text);
        connectionStatusView = (ImageView) findViewById(R.id.connection_status_img);
        LinearLayout userBox = (LinearLayout)findViewById(R.id.user_box);
        userBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGD(TAG, "userBox clicked");
                if(contextVS.getWebSocketSessionId() == null) {
                    PinDialogFragment.showPinScreen(getSupportFragmentManager(), broadCastId, getString(
                            R.string.init_authenticated_session_pin_msg), false, TypeVS.WEB_SOCKET_INIT);
                } else {showConnectionStatusDialog();}
            }
        });
        mNavDrawerItemViews = new View[mNavDrawerItems.size()];
        mDrawerItemsListContainer.removeAllViews();
        int i = 0;
        for (int itemId : mNavDrawerItems) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(itemId, mDrawerItemsListContainer);
            mDrawerItemsListContainer.addView(mNavDrawerItemViews[i]);
            ++i;
        }
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could
     * also be accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(int itemId) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    formatNavDrawerItem(mNavDrawerItemViews[i], thisItemId, itemId == thisItemId);
                }
            }
        }
    }

    public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing: ");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, "onSharedPreferenceChanged - key: " + key);
        if(USER_KEY.equals(key)) {
            setupAccountBox();
        }
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();
        setupAccountBox();
        trySetupSwipeRefresh();
        updateSwipeRefreshProgressBarTop();
        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
           // mainContent.setAlpha(0);
           // mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        } else {
            LOGW(TAG, "No view with ID main_content to fade in.");
        }
    }

    /**
     * Sets up the account box. The account box is the area at the top of the nav drawer that
     * shows which account the user is logged in as, and lets them switch accounts. It also
     * shows the user's Google+ cover photo as background.
     */
    private void setupAccountBox() {
        final View chosenAccountView = findViewById(R.id.chosen_account_view);
        if(chosenAccountView != null) { //there are Activitys withou
            chosenAccountView.setVisibility(View.VISIBLE);
            TextView nameTextView = (TextView) chosenAccountView.findViewById(R.id.profile_name_text);
            TextView email = (TextView) chosenAccountView.findViewById(R.id.profile_email_text);
            UserVS sessionUserVS = PrefUtils.getSessionUserVS(this);
            if(sessionUserVS != null) {
                nameTextView.setText(sessionUserVS.getName());
                email.setText(sessionUserVS.getEmail());
                chosenAccountView.setEnabled(true);
            }
        }
    }

    protected void onAccountChangeRequested() {
        // override if you want to be notified when another account has been selected account has changed
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_base, menu);
        MenuItem debugItem = menu.findItem(R.id.menu_debug);
        if (debugItem != null) {
            debugItem.setVisible(BuildConfig.DEBUG);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (id) {
            case R.id.menu_about:
                HelpUtils.showAbout(this);
                return true;
            case R.id.menu_debug:
                if (BuildConfig.DEBUG) {
                    startActivity(new Intent(this, DebugActionRunnerActivity.class));
                }
                return true;
            case R.id.close_app:
                isCancelled.set(true);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showMessage(int statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
        refreshingStateChanged(false);
    }

    private void showProgressDialog(final String title, final String dialogMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(ActivityBase.this);
                progressDialog.setCancelable(true);
                progressDialog.setTitle(title);
                progressDialog.setMessage(dialogMessage);
                progressDialog.setIndeterminate(true);
            }
            progressDialog.show();
            }
        });
    }


    public boolean isRefreshing() {
        return isRefreshing.get();
    }

    private void goToNavDrawerItem(int item) {
        Intent intent;
        switch (item) {
            case NAVDRAWER_ITEM_POLLS:
                intent = new Intent(this, EventsVSActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_REPRESENTATIVES:
                intent = new Intent(this, RepresentativesActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_VICKETS:
                intent = new Intent(this, VicketPagerActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void onNavDrawerItemClicked(final int itemId) {
        LOGD(TAG + ".onNavDrawerItemClicked", "itemId: " + itemId +
                " - selfNavDrawerItem: " + getSelfNavDrawerItem());
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (isSpecialItem(itemId)) {
            goToNavDrawerItem(itemId);
        } else {
            goToNavDrawerItem(itemId);
            // change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem(itemId);
            // fade out the main content
            View mainContent = findViewById(R.id.main_content);
            /*if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }*/
        }
        mDrawerLayout.closeDrawer(Gravity.START);
    }

    protected void configureStandardMenuItems(Menu menu) {
        MenuItem debugItem = menu.findItem(R.id.menu_debug);
        if (debugItem != null) debugItem.setVisible(BuildConfig.DEBUG);
    }

    @Override protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
        mSyncStatusObserver.onStatusChanged(0);
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override public void onStart() {
        super.onStart();
        if (!PrefUtils.isDataBootstrapDone(this) && mDataBootstrapThread == null) {
            performDataBootstrap();
        }
    }

    private void performDataBootstrap() {
        final Context appContext = getApplicationContext();
        LOGD(TAG, "performDataBootstrap - starting activity bootstrap background thread");
        mDataBootstrapThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGD(TAG, "Starting data bootstrap process.");
                try {// Load data from bootstrap raw resource
                    String bootstrapJson = StringUtils.parseResource(appContext, R.raw.bootstrap_data);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                mDataBootstrapThread = null;
            }
        });
        mDataBootstrapThread.start();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override public void onStop() {
        super.onStop();
    }

    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }
        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else mActionBarAutoHideSignal += deltaY;
        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    protected void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown)  return;
        mActionBarShown = show;
        getLPreviewUtils().showHideActionBarIfPartOfDecor(show);
        onActionBarAutoShowOrHide(show);
    }

    protected void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            final static int ITEMS_THRESHOLD = 3;
            int lastFvi = 0;
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }

            @Override public void onScroll(AbsListView view, int firstVisibleItem,
                               int visibleItemCount, int totalItemCount) {
                onMainContentScrolled(firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE
                );
                lastFvi = firstVisibleItem;
            }
        });
    }

    private void showConnectionStatusDialog() {
        if(contextVS.getWebSocketSessionId() != null) {
            View dialogView = getLayoutInflater().inflate(R.layout.connection_status_dialog, null);
            TextView userInfoText = (TextView) dialogView.findViewById(R.id.user_info_text);
            UserVS sessionUserVS = PrefUtils.getSessionUserVS(this);
            if(sessionUserVS != null) {
                userInfoText.setText(sessionUserVS.getEmail());
            }
            final AlertDialog dialog = new AlertDialog.Builder(this).setTitle(
                    getString(R.string.connected_with_lbl)).setView(dialogView).create();
            Button connectionButton = (Button) dialogView.findViewById(R.id.connection_button);
            connectionButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    toggleWebSocketServiceConnection();
                    dialog.hide();
                }
            });
            dialog.show();
        }
    }

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        boolean selected = getSelfNavDrawerItem() == itemId;
        int layoutToInflate = 0;
        if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if (itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = getLayoutInflater().inflate(layoutToInflate, container, false);
        if (isSeparator(itemId)) {
            UIUtils.setAccessibilityIgnore(view);
            return view;
        }
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        int iconId = itemId >= 0 && itemId < NAVDRAWER_ICON_RES_ID.length ?
                NAVDRAWER_ICON_RES_ID[itemId] : 0;
        int titleId = itemId >= 0 && itemId < NAVDRAWER_TITLE_RES_ID.length ?
                NAVDRAWER_TITLE_RES_ID[itemId] : 0;
        iconView.setVisibility(iconId > 0 ? View.VISIBLE : View.GONE);
        if (iconId > 0)  iconView.setImageResource(iconId);
        titleView.setText(getString(titleId));
        formatNavDrawerItem(view, itemId, selected);
        view.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onNavDrawerItemClicked(itemId);
            }
        });

        return view;
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS;
    }

    private boolean isSeparator(int itemId) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR || itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    private void formatNavDrawerItem(View view, int itemId, boolean selected) {
        if (isSeparator(itemId)) {
            // not applicable
            return;
        }
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        // configure its appearance according to whether or not it's selected
        titleView.setTextColor(selected ?
                getResources().getColor(R.color.navdrawer_text_color_selected) :
                getResources().getColor(R.color.navdrawer_text_color));
        iconView.setColorFilter(selected ?
                getResources().getColor(R.color.navdrawer_icon_tint_selected) :
                getResources().getColor(R.color.navdrawer_icon_tint));
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterPreferenceChangeListener(this, this);
        if(isCancelled.get()) ((AppContextVS)getApplicationContext()).finish();
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(final int which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LOGD(TAG, "SyncStatusObserver  - onStatusChanged - which: " + which);
                }
            });
        }
    };

    public void refreshingStateChanged(boolean refreshing) {
        this.isRefreshing.set(refreshing);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    public void enableDisableSwipeRefresh(boolean enable) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(enable);
        }
    }

    protected void registerHideableHeaderView(View hideableHeaderView) {
        if (!mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.add(hideableHeaderView);
        }
    }

    protected void deregisterHideableHeaderView(View hideableHeaderView) {
        if (mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.remove(hideableHeaderView);
        }
    }

    public LPreviewUtilsBase getLPreviewUtils() {
        return mLPreviewUtils;
    }

    private void updateStatusBarForNavDrawerSlide(float slideOffset) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }
        if (!mActionBarShown) {
            mLPreviewUtils.setStatusBarColor(Color.BLACK);
        }
    }

    private void toggleWebSocketServiceConnection() {
        Intent startIntent = new Intent(((AppContextVS)getApplicationContext()), WebSocketService.class);
        TypeVS typeVS = TypeVS.WEB_SOCKET_INIT;
        if(((AppContextVS)getApplicationContext()).getWebSocketSessionId() != null) typeVS = TypeVS.WEB_SOCKET_CLOSE;
        LOGD(TAG + ".toggleWebSocketServiceConnection", "operation: " + typeVS.toString());
        startIntent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
        startService(startIntent);
    }

    protected void onActionBarAutoShowOrHide(boolean shown) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }
        mStatusBarColorAnimator.setEvaluator(ARGB_EVALUATOR);
        mStatusBarColorAnimator.start();
        updateSwipeRefreshProgressBarTop();
        for (View view : mHideableHeaderViews) {
            if (shown) {
                view.animate().translationY(0).alpha(1).setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                view.animate().translationY(-view.getBottom()).alpha(0).setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            }
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return false;
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) refreshingStateChanged(true);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        boolean refreshing = (progressDialog != null && progressDialog.isShowing()) || isRefreshing.get();
        outState.putBoolean(ContextVS.LOADING_KEY, refreshing);
    }

}
