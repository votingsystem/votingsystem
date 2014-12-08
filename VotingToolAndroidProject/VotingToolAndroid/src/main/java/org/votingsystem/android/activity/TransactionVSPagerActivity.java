package org.votingsystem.android.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.fragment.TransactionVSFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import java.util.Calendar;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSPagerActivity extends ActionBarActivity {

    public static final String TAG = TransactionVSPagerActivity.class.getSimpleName();

    private AppContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.pager_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? ";
        cursor = getContentResolver().query(TransactionVSContentProvider.CONTENT_URI, null, selection,
                new String[]{contextVS.getCurrentWeekLapseId()}, null);
        TransactionVSPagerAdapter pagerAdapter = new TransactionVSPagerAdapter(
                getSupportFragmentManager(), cursor.getCount());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        //cursor.moveToFirst();
        LOGD(TAG + ".onCreate", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState + " - count: " + cursor.getCount());
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class TransactionVSPagerAdapter extends FragmentStatePagerAdapter {

        private int numPages;
        public TransactionVSPagerAdapter(FragmentManager fm, int numPages) {
            super(fm);
            this.numPages = numPages;
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".TransactionVSPagerAdapter.getItem", "item: " + i);
            return TransactionVSFragment.newInstance(i);
        }

        @Override public int getCount() {
            return numPages;
        }

    }

}