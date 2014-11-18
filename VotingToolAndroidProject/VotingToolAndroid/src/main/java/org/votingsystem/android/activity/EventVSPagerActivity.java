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

import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.fragment.EventVSFragment;
import org.votingsystem.model.TypeVS;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.CURSOR_POSITION_KEY;
import static org.votingsystem.model.ContextVS.EVENT_STATE_KEY;
import static org.votingsystem.model.ContextVS.TYPEVS_KEY;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSPagerActivity extends ActionBarActivity {

    public static final String TAG = EventVSPagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        Integer cursorPosition = getIntent().getIntExtra(CURSOR_POSITION_KEY, -1);
        String eventStateStr = getIntent().getStringExtra(EVENT_STATE_KEY);
        String eventTypeStr = getIntent().getStringExtra(TYPEVS_KEY);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        cursor = getContentResolver().query(EventVSContentProvider.CONTENT_URI,
                null, selection, new String[]{eventTypeStr, eventStateStr}, null);
        cursor.moveToPosition(cursorPosition);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(getSupportFragmentManager(),
                eventStateStr, eventTypeStr);
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                cursor.moveToPosition(position);
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
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

    public class EventsPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;
        private TypeVS eventType;

        public EventsPagerAdapter(FragmentManager fm, String eventStateStr, String eventTypeStr) {
            super(fm);
            eventType = TypeVS.valueOf(eventTypeStr);
            String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                    EventVSContentProvider.STATE_COL + "= ? ";
            cursor = getContentResolver().query(EventVSContentProvider.CONTENT_URI,
                    null, selection, new String[]{eventTypeStr, eventStateStr}, null);
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".EventsPagerAdapter.getItem", "item: " + i);
            cursor.moveToPosition(i);
            String eventJSONStr = cursor.getString(cursor.getColumnIndex(
                    EventVSContentProvider.JSON_DATA_COL));
            return EventVSFragment.newInstance(eventJSONStr);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}