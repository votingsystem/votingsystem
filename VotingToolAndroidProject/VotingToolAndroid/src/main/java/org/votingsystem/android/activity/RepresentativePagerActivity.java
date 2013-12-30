package org.votingsystem.android.activity;

import android.content.Intent;
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
import org.votingsystem.android.fragment.EventFragment;
import org.votingsystem.android.fragment.RepresentativeFragment;
import org.votingsystem.android.fragment.VotingEventFragment;
import org.votingsystem.android.ui.PagerAdapterVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.util.List;

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
        cursor = getContentResolver().query(RepresentativeContentProvider.CONTENT_URI,
                null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        RepresentativePagerAdapter eventsPagerAdapter = new RepresentativePagerAdapter(
                getSupportFragmentManager(), cursor.getCount());
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
                updateActionBarTitle();
            }
        });
    }

    private void updateActionBarTitle() {
        if(cursor != null) {
            getSupportActionBar().setLogo(R.drawable.system_users_22);
            getSupportActionBar().setTitle(contextVS.getMessage("representativeLbl"));
            String fullName = cursor.getString(cursor.getColumnIndex(
                    RepresentativeContentProvider.FULL_NAME_COL));
            getSupportActionBar().setSubtitle(fullName);
        } else Log.d(TAG + ".updateActionBarTitle(...) ", "cursor null");
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
                Intent intent = new Intent(this, NavigationDrawer.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class RepresentativePagerAdapter extends FragmentStatePagerAdapter {

        private int numRepresentatives;

        public RepresentativePagerAdapter(FragmentManager fm, int numRepresentatives) {
            super(fm);
            this.numRepresentatives = numRepresentatives;
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".RepresentativePagerAdapter.getItem(...) ", " - item: " + i);
            Cursor cursor = getContentResolver().query(RepresentativeContentProvider.CONTENT_URI,
                    null, null, null, null);
            cursor.moveToPosition(i);
            Long representativeId = cursor.getLong(cursor.getColumnIndex(
                    RepresentativeContentProvider.ID_COL));
            return RepresentativeFragment.newInstance(representativeId);
        }

        @Override public int getCount() {
            return numRepresentatives;
        }

    }

}