package org.votingsystem.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventVSPublishingFragment;
import org.votingsystem.model.OperationVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventVSPublishingActivity extends ActionBarActivity {

    public static final String TAG = "EventVSPublishingActivity";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.generic_fragment_container_activity);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        EventVSPublishingFragment publishFragment = new EventVSPublishingFragment();
        publishFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                publishFragment, EventVSPublishingFragment.TAG).commit();
    }

    public void processOperation(OperationVS operationVS) {
        EventVSPublishingFragment fragment = (EventVSPublishingFragment)getSupportFragmentManager().
                findFragmentByTag(EventVSPublishingFragment.TAG);
        if (fragment != null) fragment.processOperation(operationVS);

    }

    /*@Override public void onBackPressed() {
        EventVSPublishingFragment fragment = (EventVSPublishingFragment)getSupportFragmentManager().
                findFragmentByTag(EventVSPublishingFragment.TAG);
        if (fragment != null) fragment.onBackPressed();
        else  super.onBackPressed();
    }*/

    //@Override public boolean onKeyDown(int keyCode, KeyEvent event) {}

}