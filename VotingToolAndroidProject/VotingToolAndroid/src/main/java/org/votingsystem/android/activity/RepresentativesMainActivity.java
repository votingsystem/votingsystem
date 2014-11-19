package org.votingsystem.android.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.RepresentativeGridFragment;
import org.votingsystem.android.util.UIUtils;

import java.lang.ref.WeakReference;

import static org.votingsystem.android.util.LogUtils.LOGD;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativesMainActivity extends ActivityBase {

	public static final String TAG = RepresentativesMainActivity.class.getSimpleName();

    WeakReference<RepresentativeGridFragment> weakRefToFragment;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        RepresentativeGridFragment fragment = new RepresentativeGridFragment();
        weakRefToFragment = new WeakReference<RepresentativeGridFragment>(fragment);
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment,
                        ((Object)fragment).getClass().getSimpleName()).commit();
        getSupportActionBar().setTitle(getString(R.string.representatives_drop_down_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        menu.removeGroup(R.id.general_items);
        inflater.inflate(R.menu.representative_grid, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override protected int getSelfNavDrawerItem() {
        // we only have a nav drawer if we are in top-level Representatives mode.
        return NAVDRAWER_ITEM_REPRESENTATIVES;
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
        RepresentativeGridFragment fragment = weakRefToFragment.get();
        fragment.fetchItems(fragment.getOffset());
    }

}