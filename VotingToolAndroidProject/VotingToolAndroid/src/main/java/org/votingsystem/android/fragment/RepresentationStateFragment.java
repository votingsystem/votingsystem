package org.votingsystem.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Representation;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentationStateFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = RepresentationStateFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private Representation representation;
    private View rootView;
    private String broadCastId = RepresentationStateFragment.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) ;
            else setProgressDialogVisible(false);
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        rootView = inflater.inflate(R.layout.representative_state, container, false);
        ((WebView)rootView.findViewById(R.id.representative_description)).setBackgroundColor(
                getResources().getColor(R.color.bkg_screen_vs));
        setRepresentationView();
        setHasOptionsMenu(true);
        return rootView;
    }

    private void setRepresentationView() {
        this.representation = PrefUtils.getRepresentationState(getActivity());
        if(representation == null) {
            ((TextView)rootView.findViewById(R.id.last_checked_date)).setText(getString(
                    R.string.representation_state_missing_lbl));
            return;
        } else {
            ((TextView)rootView.findViewById(R.id.last_checked_date)).setText(getString(
                    R.string.representation_last_checked_msg,
                    DateUtils.getDayWeekDateStr(representation.getLastCheckedDate())));
        }
        switch(representation.getState()) {
            case WITHOUT_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.without_representative_msg));
                break;
            case WITH_PUBLIC_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.with_public_representation_msg));
                rootView.findViewById(R.id.representative_container).setVisibility(View.VISIBLE);
                break;
            case WITH_ANONYMOUS_REPRESENTATION:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.with_anonymous_representation_msg, DateUtils.getDayWeekDateStr(
                                representation.getDateTo())));
                break;
            case REPRESENTATIVE:
                ((TextView)rootView.findViewById(R.id.msg)).setText(getString(
                        R.string.representative_msg));
                ((TextView)rootView.findViewById(R.id.representative_name)).setText(
                        representation.getRepresentative().getFullName());
                String representativeDescription = "<html style='background-color:#eeeeee;margin:0 auto;'>" +
                                representation.getRepresentative().getDescription() + "</html>";
                ((WebView)rootView.findViewById(R.id.representative_description)).loadData(
                        representativeDescription, "text/html; charset=UTF-8", "UTF-8");
                UIUtils.setImage(((ImageView)rootView.findViewById(R.id.representative_image)),
                        representation.getRepresentative().getImageBytes(), getActivity());
                rootView.findViewById(R.id.representative_container).setVisibility(View.VISIBLE);
                break;
        }
    }

    private void launchRepresentativeService(TypeVS operationType) {
        LOGD(TAG + ".revokeRepresentative", "revokeRepresentative");
        Intent startIntent = new Intent(getActivity(), RepresentativeService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true);
        getActivity().startService(startIntent);
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ModalProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ModalProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onResume() {
        super.onResume();
        PrefUtils.registerPreferenceChangeListener(getActivity(), this);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        PrefUtils.unregisterPreferenceChangeListener(this, getActivity());
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                //getActivity().finish();
                return true;
            case R.id.check_representation_state:
                launchRepresentativeService(TypeVS.STATE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(Representation.class.getSimpleName().equals(key)) {
            LOGD(TAG + ".onSharedPreferenceChanged", "key: " + key);
            setRepresentationView();
        }
    }

}