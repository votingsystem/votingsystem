package org.votingsystem.android.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.CooinAccountsFragment;
import org.votingsystem.android.fragment.TransactionVSGridFragment;
import org.votingsystem.android.service.CooinService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinAccountsMainActivity extends ActivityBase {

    public static final String TAG = CooinAccountsMainActivity.class.getSimpleName();

    //corresponds to finance section child screens order
    private static final int USER_ACCOUNTS_POS       = 0;
    private static final int TRANSANCTIONVS_LIST_POS = 1;

    private AppContextVS contextVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.cooin_accounts_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) { }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            public void onPageSelected(int position) {
                LOGD(TAG + ".onCreate", "onPageSelected: " + position);
                switch (position) {
                    case USER_ACCOUNTS_POS:
                        getSupportActionBar().setSubtitle(null);
                        break;
                    case TRANSANCTIONVS_LIST_POS:
                        getSupportActionBar().setSubtitle(getString(R.string.movements_lbl));
                        break;
                }

            }
        });
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        CooinPagerAdapter pagerAdapter = new CooinPagerAdapter(getSupportFragmentManager(),
                getIntent().getExtras());
        mViewPager.setAdapter(pagerAdapter);
        getSupportActionBar().setTitle(getString(R.string.cooin_accounts_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            /*case R.id.admin_cooins_menu_item:
                Intent intent = new Intent(this, BrowserVSActivity.class);
                intent.putExtra(ContextVS.URL_KEY, contextVS.getCooinServer().getMenuAdminURL());
                startActivity(intent);
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_COOIN_ACCOUNTS;// we only have a nav drawer if we are in top-level
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
    }

    class CooinPagerAdapter extends FragmentStatePagerAdapter {

        final String TAG = CooinPagerAdapter.class.getSimpleName();

        private static final int USERVS_MONETARY_INFO = 0;
        private static final int COOIN_LIST = 1;

        private String searchQuery = null;
        private Bundle args;

        public CooinPagerAdapter(FragmentManager fragmentManager, Bundle args) {
            super(fragmentManager);
            this.args = (args != null)? args:new Bundle();
        }

        @Override public Fragment getItem(int position) {
            Fragment selectedFragment = null;
            switch(position) {
                case USERVS_MONETARY_INFO:
                    selectedFragment = new CooinAccountsFragment();
                    break;
                case COOIN_LIST:
                    selectedFragment = new TransactionVSGridFragment();
                    break;
            }
            args.putString(SearchManager.QUERY, searchQuery);
            selectedFragment.setArguments(args);
            LOGD(TAG + ".getItem", "position:" + position + " - args: " + args +
                    " - selectedFragment.getClass(): " + ((Object)selectedFragment).getClass());
            return selectedFragment;
        }

        @Override public int getCount() {
            return 2;
        } //COOIN_ACCOUNTS_INFO and COOIN_LIST

    }

}