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

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.fragment.EventVSStatsFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSStatsPagerActivity extends ActionBarActivity {

    public static final String TAG = EventVSStatsPagerActivity.class.getSimpleName();

    private AppContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        Integer cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, -1);
        EventVS.State eventState = (EventVS.State)getIntent().getSerializableExtra(ContextVS.EVENT_STATE_KEY);
        TypeVS eventType = (TypeVS)getIntent().getSerializableExtra(ContextVS.TYPEVS_KEY);
        Long eventId = getIntent().getLongExtra(ContextVS.ITEM_ID_KEY, -1L);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        LOGD(TAG + ".onCreate", "eventId: " + eventId + " - cursorPosition: " +
                cursorPosition + " - eventState:" + eventState + " - eventType: " + eventType);
        cursor = getContentResolver().query(EventVSContentProvider.CONTENT_URI,
                null, selection, new String[]{eventType.toString(), eventState.toString()}, null);
        cursor.moveToFirst();
        if(cursorPosition < 0) {
            while (!cursor.isLast()) {
                if (cursor.getLong(cursor.getColumnIndex(EventVSContentProvider.ID_COL)) == eventId) {
                    cursorPosition = cursor.getPosition();
                    break;
                } else cursor.moveToNext();
            }
        } else cursor.moveToPosition(cursorPosition);
        setContentView(R.layout.pager_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EventVSPagerAdapter eventsPagerAdapter = new EventVSPagerAdapter(getSupportFragmentManager(),
                eventState.toString(), eventType.toString());
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                cursor.moveToPosition(position);
                setActionBarTitle();
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
        setActionBarTitle();
    }

    private void setActionBarTitle() {
        String eventJSON = cursor.getString(cursor.getColumnIndex(
                EventVSContentProvider.JSON_DATA_COL));
        try {
            EventVS event = EventVS.parse(new JSONObject(eventJSON));
            String subtTitle = null;
            String title = getString(R.string.voting_info_lbl) + " '"+ event.getSubject() + "'";
            switch(event.getState()) {
                case ACTIVE:
                    subtTitle = getString(R.string.voting_open_lbl,
                            DateUtils.getElapsedTimeStr(event.getDateFinish()));
                    break;
                case PENDING:
                    subtTitle = getString(R.string.voting_pending_lbl) + " - " +
                            getString(R.string.init_lbl) + ": " +
                            DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                            "" + getString(R.string.finish_lbl) + ": " +
                            DateUtils.getDayWeekDateStr(event.getDateFinish());
                    break;
                default:
                    subtTitle = getString(R.string.voting_closed_lbl);
            }
            if(title != null) getSupportActionBar().setTitle(title);
            if(subtTitle != null) getSupportActionBar().setSubtitle(subtTitle);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
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

    public class EventVSPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public EventVSPagerAdapter(FragmentManager fm, String eventStateStr, String eventTypeStr) {
            super(fm);
            String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                    EventVSContentProvider.STATE_COL + "= ? ";
            cursor = getContentResolver().query(EventVSContentProvider.CONTENT_URI,
                    null, selection, new String[]{eventTypeStr, eventStateStr}, null);
        }

        @Override public Fragment getItem(int i) {
            LOGD(TAG + ".EventVSPagerAdapter.getItem", "item: " + i);
            cursor.moveToPosition(i);
            Long eventId = cursor.getLong(cursor.getColumnIndex(EventVSContentProvider.ID_COL));
            return EventVSStatsFragment.newInstance(eventId);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}