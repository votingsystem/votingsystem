package org.votingsystem.android.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.fragment.TransactionVSFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import java.util.Calendar;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSPagerActivity extends ActionBarActivity {

    public static final String TAG = TransactionVSPagerActivity.class.getSimpleName();

    private AppContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        Log.d(TAG + ".onCreate(...) ", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        TransactionVSPagerAdapter pagerAdapter = new TransactionVSPagerAdapter(
                getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        String weekLapse = DateUtils.getDirPath(DateUtils.getMonday(Calendar.getInstance()).getTime());
        String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? ";
        cursor = getContentResolver().query(TransactionVSContentProvider.CONTENT_URI, null, selection,
                new String[]{weekLapse}, null);
        cursor.moveToFirst();
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
                updateActionBarTitle();
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
        updateActionBarTitle();
    }

    private void updateActionBarTitle() { }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState:" + outState);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class TransactionVSPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public TransactionVSPagerAdapter(FragmentManager fm) {
            super(fm);
            String weekLapse = DateUtils.getDirPath(DateUtils.getMonday(Calendar.getInstance()).getTime());
            String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? ";
            cursor = getContentResolver().query(TransactionVSContentProvider.CONTENT_URI, null, selection,
                    new String[]{weekLapse}, null);
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".TransactionVSPagerAdapter.getItem(...) ", "item: " + i);
            cursor.moveToPosition(i);
            return TransactionVSFragment.newInstance(i);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}