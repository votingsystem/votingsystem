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

import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.android.fragment.ReceiptFragment;
import org.votingsystem.android.fragment.RepresentativeFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ReceiptPagerActivity extends ActionBarActivity {

    public static final String TAG = "ReceiptPagerActivity";

    private ContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getBaseContext());
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, 0);
        Log.d(TAG + ".onCreate(...) ", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        ReceiptPagerAdapter pagerAdapter = new ReceiptPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        cursor = getContentResolver().query(ReceiptContentProvider.CONTENT_URI,null, null, null,
                null);
        cursor.moveToPosition(cursorPosition);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
                updateActionBarTitle();
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
        getSupportActionBar().setLogo(R.drawable.receipt_32);
        updateActionBarTitle();
    }

    private void updateActionBarTitle() {
        String title = null;
        String subtitle = "";
        String typeStr = cursor.getString(cursor.getColumnIndex(
                ReceiptContentProvider.TYPE_COL));
        TypeVS type = TypeVS.valueOf(typeStr);
        switch(type) {
            case VOTEVS:
                title = getString(R.string.receipt_vote_page_title);
                break;
            case CANCEL_VOTE:
                title = getString(R.string.receipt_cancel_vote_page_title);
                break;
        }
        Date dateCreated = null;
        Date dateUpdated = null;
        Long timestampCreated = cursor.getLong(cursor.getColumnIndex(
                ReceiptContentProvider.TIMESTAMP_CREATED_COL));
        if(timestampCreated != null) dateCreated = new Date(timestampCreated);
        Long timestampUpdated = cursor.getLong(cursor.getColumnIndex(
                ReceiptContentProvider.TIMESTAMP_UPDATED_COL));
        if(timestampUpdated != null) dateUpdated = new Date(timestampUpdated);
        if(dateCreated != null) {
            subtitle = getString(R.string.saved_lbl) + " " + DateUtils.
                    getShortSpanishStringFromDate(dateCreated);
        }
        if(dateUpdated != null) {
            if(timestampCreated != timestampUpdated)
            subtitle = getString(R.string.updated_lbl) + " " + DateUtils.
                    getShortSpanishStringFromDate(dateCreated);
        }
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setSubtitle(subtitle);
    }

    public void setActionBarTitle(String title, String subtitle) {
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setSubtitle(subtitle);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState:" + outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG + ".onRestoreInstanceState(...)", "onRestoreInstanceState:" + savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
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

    class ReceiptPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public ReceiptPagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(ReceiptContentProvider.CONTENT_URI,
                    null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".ReceiptPagerAdapter.getItem(...) ", " - item: " + i);
            cursor.moveToPosition(i);
            return ReceiptFragment.newInstance(cursor.getPosition());
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}