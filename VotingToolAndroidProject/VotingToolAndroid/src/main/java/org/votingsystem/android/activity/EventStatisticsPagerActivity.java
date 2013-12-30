package org.votingsystem.android.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventStatisticsFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.util.DateUtils;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventStatisticsPagerActivity extends ActionBarActivity {

    public static final String TAG = "EventStatisticsPagerActivity";

    private ContextVS contextVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getBaseContext());
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                setActionBarTitle((EventVS) contextVS.getEvents().get(position));
            }
        });
        mViewPager.setCurrentItem(contextVS.getEventIndex(contextVS.getEvent()), true);
        setActionBarTitle(contextVS.getEvent());
    }

    private void setActionBarTitle(EventVS event) {
        String title = null;
        String subtTitle = null;
        switch(event.getTypeVS()) {
            case MANIFEST_EVENT:
                getSupportActionBar().setLogo(R.drawable.manifest_32);
                title = getString(R.string.manifest_info_lbl) + " '"+ event.getSubject() + "'";
                switch(event.getState()) {
                    case ACTIVE:
                        subtTitle = getString(R.string.manifest_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getDateFinish()));
                        break;
                    case AWAITING:
                        subtTitle = getString(R.string.manifest_pendind_lbl) + " - "+
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    case CANCELLED:
                        subtTitle = getString(R.string.manifest_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ") - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case TERMINATED:
                        subtTitle = getString(R.string.manifest_closed_lbl) + " - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    default:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                }
                break;
            case CLAIM_EVENT:
                getSupportActionBar().setLogo(R.drawable.filenew_32);
                title = getString(R.string.claim_info_lbl) + " '"+ event.getSubject() + "'";
                switch(event.getState()) {
                    case ACTIVE:
                        subtTitle = getString(R.string.claim_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getDateFinish()));
                        break;
                    case AWAITING:
                        subtTitle = getString(R.string.claim_pending_lbl) +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    case CANCELLED:
                        subtTitle = getString(R.string.claim_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ") " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case TERMINATED:
                        subtTitle = getString(R.string.claim_closed_lbl) + " - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                    default:
                        subtTitle = getString(R.string.claim_closed_lbl);
                }
                break;
            case VOTING_EVENT:
                getSupportActionBar().setLogo(R.drawable.poll_32);
                title = getString(R.string.voting_info_lbl) + " '"+ event.getSubject() + "'";
                switch(event.getState()) {
                    case ACTIVE:
                        subtTitle = getString(R.string.voting_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getDateFinish()));
                        break;
                    case AWAITING:
                        subtTitle = getString(R.string.voting_pending_lbl) + " - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    default:
                        subtTitle = getString(R.string.voting_closed_lbl);
                }
                break;
        }
        if(title != null) getSupportActionBar().setTitle(title);
        if(subtTitle != null) getSupportActionBar().setSubtitle(subtTitle);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class EventsPagerAdapter extends FragmentStatePagerAdapter {

        public static final String EVENT_INDEX_KEY = "eventIndex";

        public EventsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".EventsPagerAdapter.getItem(...) ", " - item: " + i);
            Fragment fragment = new EventStatisticsFragment();
            Bundle args = new Bundle();
            args.putInt(EVENT_INDEX_KEY, i);
            fragment.setArguments(args);
            return fragment;
        }

        @Override public int getCount() {
            return contextVS.getEvents().size();
        }

    }

}