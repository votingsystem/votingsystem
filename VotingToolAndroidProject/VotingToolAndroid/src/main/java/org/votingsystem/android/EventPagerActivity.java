package org.votingsystem.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import org.votingsystem.android.model.ContextVSAndroid;
import org.votingsystem.android.model.EventVSAndroid;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.util.List;

/**
 * Created by jgzornoza on 9/10/13.
 */
public class EventPagerActivity  extends ActionBarActivity {

    public static final String TAG = "EventPagerActivity";

    private ContextVSAndroid contextVSAndroid;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", " - onCreate ");
        super.onCreate(savedInstanceState);
        contextVSAndroid = ContextVSAndroid.getInstance(getBaseContext());
        if(contextVSAndroid.getEvents() == null) {
            Log.d(TAG + ".onCreate(...) ", " - Events not found in context");
            return;
        }
        setContentView(R.layout.event_pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(
                getSupportFragmentManager(), contextVSAndroid.getEvents());
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                EventVSAndroid selectedEvent = (EventVSAndroid) contextVSAndroid.getEvents().get(position);
                contextVSAndroid.setEvent(selectedEvent);
                setActionBarTitle(selectedEvent);

            }
        });
        mViewPager.setCurrentItem(contextVSAndroid.getEventIndex(contextVSAndroid.getEvent()), true);
        setActionBarTitle(contextVSAndroid.getEvent());
    }

    private void setActionBarTitle(EventVSAndroid event) {
        String subtTitle = null;
        switch(event.getTypeVS()) {
            case EVENTO_FIRMA:
                getSupportActionBar().setLogo(R.drawable.manifest_32);
                switch(event.getEstadoEnumValue()) {
                    case ACTIVO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin())));
                        break;
                    case PENDIENTE_COMIENZO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_pendind_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                        break;
                    case CANCELADO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case FINALIZADO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                        break;
                    default:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                }
                break;
            case EVENTO_RECLAMACION:
                getSupportActionBar().setLogo(R.drawable.filenew_32);
                switch(event.getEstadoEnumValue()) {
                    case ACTIVO:
                        getSupportActionBar().setTitle(getString(R.string.claim_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin())));
                        break;
                    case PENDIENTE_COMIENZO:
                        getSupportActionBar().setTitle(getString(R.string.claim_pending_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                        break;
                    case CANCELADO:
                        getSupportActionBar().setTitle(getString(R.string.claim_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case FINALIZADO:
                        setTitle(getString(R.string.claim_closed_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                    default:
                        getSupportActionBar().setTitle(getString(R.string.claim_closed_lbl));
                }
                break;
            case EVENTO_VOTACION:
                getSupportActionBar().setLogo(R.drawable.poll_32);
                switch(event.getEstadoEnumValue()) {
                    case ACTIVO:
                        getSupportActionBar().setTitle(getString(R.string.voting_open_lbl,
                                        DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin())));
                        break;
                    case PENDIENTE_COMIENZO:
                        getSupportActionBar().setTitle(getString(R.string.voting_pending_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
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

        public static final String EVENT_INDEX_KEY = "eventIndex";

        private List<EventVS> events = null;

        public EventsPagerAdapter(FragmentManager fm, List<EventVS> events) {
            super(fm);
            this.events = events;
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".EventsPagerAdapter.getItem(...) ", " - item: " + i);
            Fragment fragment = null;
            if (contextVSAndroid.getEvent().getTypeVS().equals(TypeVS.EVENTO_VOTACION)) {
                fragment = new VotingEventFragment();
            } else {
                fragment = new EventFragment();
            }
            Bundle args = new Bundle();
            args.putInt(EVENT_INDEX_KEY, i);
            fragment.setArguments(args);
            return fragment;
        }

        @Override public int getCount() {
            return events.size();
        }

    }

}