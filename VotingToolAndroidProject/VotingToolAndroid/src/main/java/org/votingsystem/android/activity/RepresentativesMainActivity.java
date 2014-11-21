package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.ProgressDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.RepresentationStateFragment;
import org.votingsystem.android.fragment.RepresentativeGridFragment;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Representation;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

import java.lang.ref.WeakReference;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativesMainActivity extends ActivityBase {

	public static final String TAG = RepresentativesMainActivity.class.getSimpleName();

    private AppContextVS contextVS = null;
    private String broadCastId = RepresentativesMainActivity.class.getSimpleName();
    private WeakReference<RepresentativeGridFragment> weakRefToFragment;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null)
                launchRepresentativeService(TypeVS.REPRESENTATIVE_REVOKE);
            else {
                setProgressDialogVisible(null, null, false);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    MessageDialogFragment.showDialog(responseVS, getSupportFragmentManager());
                } else {
                    if(responseVS.getTypeVS() == TypeVS.REPRESENTATIVE_REVOKE) {
                        MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                                getString(R.string.revoke_representative_msg_subject),
                                getString(R.string.operation_ok_msg), getSupportFragmentManager());
                    }
                }
            }
        }
    };

    private void launchRepresentativeService(TypeVS operationType) {
        LOGD(TAG + ".revokeRepresentative", "revokeRepresentative");
        Intent startIntent = new Intent(this, RepresentativeService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        setProgressDialogVisible(getString(R.string.wait_msg),
                getString(R.string.revoke_representative_msg_subject),true);
        startService(startIntent);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        contextVS = (AppContextVS) getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.representatives_drop_down_lbl));
        RepresentationStateFragment fragment = new RepresentationStateFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                fragment, RepresentationStateFragment.TAG).commit();
    }


    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
                /*case R.id.reload:
                fetchItems(offset);
                rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
                getLoaderManager().restartLoader(loaderId, null, this);
                return true;*/
            case R.id.cancel_anonymouys_representation:
                return true;
            case R.id.new_representative:
                Intent intent = new Intent(this, RepresentativeNewActivity.class);
                startActivity(intent);
                return true;
            case R.id.cancel_representative:
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.remove_representative_caption),
                        getString(R.string.remove_representative_msg), this);
                builder.setPositiveButton(getString(R.string.continue_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                PinDialogFragment.showPinScreen(getSupportFragmentManager(),
                                        broadCastId, getString(R.string.enter_signature_pin_msg),
                                        false, null);
                            }
                        }).setNegativeButton(getString(R.string.cancel_lbl), null);
                builder.show().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                return true;
            case R.id.edit_representative:
                Intent editIntent = new Intent(this, RepresentativeNewActivity.class);
                editIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.REPRESENTATIVE);
                startActivity(editIntent);
                return true;
            case R.id.representative_list:
                intent = new Intent(this, FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, RepresentativeGridFragment.class.getName());
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu(..)", " - onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        menu.removeGroup(R.id.general_items);
        inflater.inflate(R.menu.representative_main, menu);
        Representation representation = PrefUtils.getRepresentationState(this);
        if(representation != null) {
            switch(representation.getState()) {
                case REPRESENTATIVE:
                    menu.removeGroup(R.id.options_for_uservs);
                    break;
                case WITH_ANONYMOUS_REPRESENTATION:
                    menu.removeGroup(R.id.options_for_representatives);
                    break;
                case WITH_PUBLIC_REPRESENTATION:
                    menu.removeGroup(R.id.options_for_representatives);
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                    break;
                case WITHOUT_REPRESENTATION:
                    menu.removeGroup(R.id.options_for_representatives);
                    menu.removeItem(R.id.cancel_anonymouys_representation);
                    break;
            }
        } else {
            menu.removeGroup(R.id.options_for_representatives);
            menu.removeGroup(R.id.options_for_uservs);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if (isVisible) ProgressDialogFragment.showDialog(
                caption, message, getSupportFragmentManager());
        else ProgressDialogFragment.hide(getSupportFragmentManager());
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

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }


}