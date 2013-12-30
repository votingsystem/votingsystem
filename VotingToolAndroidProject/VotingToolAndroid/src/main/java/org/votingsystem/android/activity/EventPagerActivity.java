package org.votingsystem.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventFragment;
import org.votingsystem.android.fragment.RepresentativeFragment;
import org.votingsystem.android.fragment.VotingEventFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventPagerActivity  extends ActionBarActivity {

    public static final String TAG = "EventPagerActivity";


    private ContextVS contextVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getBaseContext());
        if(contextVS.getEvents() == null) {
            Log.d(TAG + ".onCreate(...) ", " - Events not found in context");
            return;
        }
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(
                getSupportFragmentManager(), contextVS.getEvents());
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                EventVS selectedEvent = contextVS.getEvents().get(position);
                contextVS.setEvent(selectedEvent);
                setActionBarTitle(selectedEvent);
            }
        });
        mViewPager.setCurrentItem(contextVS.getEventIndex(contextVS.getEvent()), true);
        setActionBarTitle(contextVS.getEvent());
    }

    private void setActionBarTitle(EventVS event) {
        String subtTitle = null;
        switch(event.getTypeVS()) {
            case MANIFEST_EVENT:
                getSupportActionBar().setLogo(R.drawable.manifest_32);
                switch(event.getStateEnumValue()) {
                    case ACTIVE:
                        getSupportActionBar().setTitle(getString(R.string.manifest_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getDateFinish())));
                        break;
                    case AWAITING:
                        getSupportActionBar().setTitle(getString(R.string.manifest_pendind_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    case CANCELLED:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case TERMINATED:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
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
                switch(event.getStateEnumValue()) {
                    case ACTIVE:
                        getSupportActionBar().setTitle(getString(R.string.claim_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getDateFinish())));
                        break;
                    case AWAITING:
                        getSupportActionBar().setTitle(getString(R.string.claim_pending_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    case CANCELLED:
                        getSupportActionBar().setTitle(getString(R.string.claim_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case TERMINATED:
                        setTitle(getString(R.string.claim_closed_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                    default:
                        getSupportActionBar().setTitle(getString(R.string.claim_closed_lbl));
                }
                break;
            case VOTING_EVENT:
                getSupportActionBar().setLogo(R.drawable.poll_32);
                switch(event.getStateEnumValue()) {
                    case ACTIVE:
                        getSupportActionBar().setTitle(getString(R.string.voting_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getDateFinish())));
                        break;
                    case AWAITING:
                        getSupportActionBar().setTitle(getString(R.string.voting_pending_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateBegin()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getDateFinish());
                        break;
                    default:
                        getSupportActionBar().setTitle(getString(R.string.voting_closed_lbl));
                }
                break;
        }
        if(subtTitle != null) getSupportActionBar().setSubtitle(subtTitle);
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

    public class EventsPagerAdapter extends FragmentStatePagerAdapter {

        private List<EventVS> events = null;

        public EventsPagerAdapter(FragmentManager fm, List<EventVS> events) {
            super(fm);
            this.events = events;
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".EventsPagerAdapter.getItem(...) ", " - item: " + i);
            Fragment fragment = null;
            if (contextVS.getEvent().getTypeVS().equals(TypeVS.VOTING_EVENT)) {
                fragment = VotingEventFragment.newInstance(i);
            } else fragment = EventFragment.newInstance(i);
            return fragment;
        }

        @Override public int getCount() {
            return events.size();
        }

    }

}