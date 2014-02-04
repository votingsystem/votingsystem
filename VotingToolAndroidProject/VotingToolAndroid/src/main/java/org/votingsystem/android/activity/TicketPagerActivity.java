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
import org.votingsystem.android.contentprovider.TicketContentProvider;
import org.votingsystem.android.fragment.TicketFragment;
import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketPagerActivity extends ActionBarActivity {

    public static final String TAG = TicketPagerActivity.class.getSimpleName();

    private AppContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, 0);
        Log.d(TAG + ".onCreate(...) ", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        TicketPagerAdapter pagerAdapter = new TicketPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        cursor = getContentResolver().query(TicketContentProvider.CONTENT_URI,null, null, null,
                null);
        cursor.moveToPosition(cursorPosition);
        mViewPager.setCurrentItem(cursorPosition);
        getSupportActionBar().setLogo(R.drawable.euro_32);
        getSupportActionBar().setTitle(getString(R.string.ticket_lbl));
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

    class TicketPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public TicketPagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(TicketContentProvider.CONTENT_URI,
                    null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".TicketPagerAdapter.getItem(...) ", " - item: " + i);
            cursor.moveToPosition(i);
            return TicketFragment.newInstance(cursor.getPosition());
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}