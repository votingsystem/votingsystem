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
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.android.fragment.RepresentativeFragment;
import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativePagerActivity extends ActionBarActivity {

    public static final String TAG = "RepresentativePagerActivity";

    private ContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getBaseContext());
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        Log.d(TAG + ".onCreate(...) ", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        RepresentativePagerAdapter eventsPagerAdapter = new RepresentativePagerAdapter(
                getSupportFragmentManager());
        mViewPager.setAdapter(eventsPagerAdapter);
        cursor = getContentResolver().query(RepresentativeContentProvider.CONTENT_URI,
                null, null, null, null);
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

    private void updateActionBarTitle() {
        getSupportActionBar().setLogo(R.drawable.system_users_22);
        getSupportActionBar().setTitle(getString(R.string.representative_lbl));
        String fullName = cursor.getString(cursor.getColumnIndex(
                RepresentativeContentProvider.FULL_NAME_COL));
        getSupportActionBar().setSubtitle(fullName);
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

    class RepresentativePagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public RepresentativePagerAdapter(FragmentManager fm) {
            super(fm);
            cursor = getContentResolver().query(RepresentativeContentProvider.CONTENT_URI,
                    null, null, null, null);
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".RepresentativePagerAdapter.getItem(...) ", " - item: " + i);
            cursor.moveToPosition(i);
            Long representativeId = cursor.getLong(cursor.getColumnIndex(
                    RepresentativeContentProvider.ID_COL));
            return RepresentativeFragment.newInstance(representativeId);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}