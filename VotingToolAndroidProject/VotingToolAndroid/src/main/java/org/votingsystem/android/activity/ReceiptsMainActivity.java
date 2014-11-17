package org.votingsystem.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.ReceiptGridFragment;

import java.lang.ref.WeakReference;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptsMainActivity extends ActivityBase {

    public static final String TAG = ReceiptsMainActivity.class.getSimpleName();

    WeakReference<ReceiptGridFragment> weakRefToFragment;
    private AppContextVS contextVS;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.activity_base);
        ReceiptGridFragment fragment = new ReceiptGridFragment();
        weakRefToFragment = new WeakReference<ReceiptGridFragment>(fragment);
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment,
                ((Object)fragment).getClass().getSimpleName()).commit();
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        getActionBar().setLogo(null);
        getActionBar().setTitle(getString(R.string.receipts_lbl));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_RECEIPTS;// we only have a nav drawer if we are in top-level
    }

    @Override public void requestDataRefresh() {
        LOGD(TAG, ".requestDataRefresh() - Requesting manual data refresh - refreshing:");
    }

}