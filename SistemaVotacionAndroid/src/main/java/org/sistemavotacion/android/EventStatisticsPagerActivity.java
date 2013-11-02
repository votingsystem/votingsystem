package org.sistemavotacion.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.util.DateUtils;

/**
 * Created by jgzornoza on 9/10/13.
 */
public class EventStatisticsPagerActivity extends ActionBarActivity {

    public static final String TAG = "EventStatisticsPagerActivity";

    private AppData appData;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appData = AppData.getInstance(getBaseContext());
        setContentView(R.layout.event_pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                setActionBarTitle(appData.getEvents().get(position));
            }
        });
        mViewPager.setCurrentItem(appData.getEventIndex(appData.getEvent()), true);
        setActionBarTitle(appData.getEvent());
    }

    private void setActionBarTitle(Evento event) {
        String title = null;
        String subtTitle = null;
        switch(event.getTipo()) {
            case EVENTO_FIRMA:
                getSupportActionBar().setLogo(R.drawable.manifest_32);
                title = getString(R.string.manifest_info_lbl) + " '"+ event.getAsunto() + "'";
                switch(event.getEstadoEnumValue()) {
                    case ACTIVO:
                        subtTitle = getString(R.string.manifest_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin()));
                        break;
                    case PENDIENTE_COMIENZO:
                        subtTitle = getString(R.string.manifest_pendind_lbl) + " - "+
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                        break;
                    case CANCELADO:
                        subtTitle = getString(R.string.manifest_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ") - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case FINALIZADO:
                        subtTitle = getString(R.string.manifest_closed_lbl) + " - " +
                                getString(R.string.inicio_lbl) + ": " +
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
                title = getString(R.string.claim_info_lbl) + " '"+ event.getAsunto() + "'";
                switch(event.getEstadoEnumValue()) {
                    case ACTIVO:
                        subtTitle = getString(R.string.claim_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin()));
                        break;
                    case PENDIENTE_COMIENZO:
                        subtTitle = getString(R.string.claim_pending_lbl) +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                        break;
                    case CANCELADO:
                        subtTitle = getString(R.string.claim_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ") " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case FINALIZADO:
                        subtTitle = getString(R.string.claim_closed_lbl) + " - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
                    default:
                        subtTitle = getString(R.string.claim_closed_lbl);
                }
                break;
            case EVENTO_VOTACION:
                getSupportActionBar().setLogo(R.drawable.poll_32);
                title = getString(R.string.voting_info_lbl) + " '"+ event.getAsunto() + "'";
                switch(event.getEstadoEnumValue()) {
                    case ACTIVO:
                        subtTitle = getString(R.string.voting_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin()));
                        break;
                    case PENDIENTE_COMIENZO:
                        subtTitle = getString(R.string.voting_pending_lbl) + " - " +
                                getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(event.getFechaFin());
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
            return appData.getEvents().size();
        }

    }

}