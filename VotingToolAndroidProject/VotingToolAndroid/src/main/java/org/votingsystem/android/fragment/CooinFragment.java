package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinFragment extends Fragment {

    public static final String TAG = CooinFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private Cooin selectedCooin;
    private TextView cooin_amount, cooin_state, cooin_currency, date_info;
    private SMIMEMessage selectedCooinSMIME;
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) ;
            else {
                setProgressDialogVisible(false);
                switch(responseVS.getTypeVS()) {
                    case DEVICE_SELECT:
                        break;
                    default: MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                           responseVS.getCaption(), responseVS.getNotificationMessage(),
                           getFragmentManager());
                }


            }
        }
    };


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        selectedCooin = (Cooin) getArguments().getSerializable(ContextVS.COOIN_KEY);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        broadCastId = CooinFragment.class.getSimpleName() + "_" + selectedCooin.getHashCertVS();
        View rootView = inflater.inflate(R.layout.cooin, container, false);
        cooin_amount = (TextView)rootView.findViewById(R.id.cooin_amount);
        cooin_state = (TextView)rootView.findViewById(R.id.cooin_state);
        cooin_currency = (TextView)rootView.findViewById(R.id.cooin_currency);
        date_info = (TextView)rootView.findViewById(R.id.date_info);
        initCooinScreen(selectedCooin);
        setHasOptionsMenu(true);
        return rootView;
    }

    private void initCooinScreen(Cooin cooin) {
        try {
            cooin_amount.setText(cooin.getAmount().toPlainString());
            cooin_currency.setText(cooin.getCurrencyCode());
            selectedCooinSMIME = cooin.getReceipt();
            getActivity().setTitle(MsgUtils.getCooinDescriptionMessage(cooin, getActivity()));
            date_info.setText(getString(R.string.cooin_date_info,
                    DateUtils.getDateStr(cooin.getDateFrom(), "dd MMM yyyy' 'HH:mm"),
                    DateUtils.getDateStr(cooin.getDateTo(), "dd MMM yyyy' 'HH:mm")));
            if(Cooin.State.OK != selectedCooin.getState()) {
                cooin_state.setText(MsgUtils.getCooinStateMessage(selectedCooin, getActivity()));
                cooin_state.setVisibility(View.VISIBLE);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedCooin != null) outState.putSerializable(ContextVS.RECEIPT_KEY, selectedCooin);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected model type:" + selectedCooin.getTypeVS());
        menuInflater.inflate(R.menu.cooin_fragment, menu);
        try {
            if(selectedCooin.getReceipt() == null) {
                menu.removeItem(R.id.show_timestamp_info);
                menu.removeItem(R.id.share_cooin);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    getFragmentManager().popBackStackImmediate();
                    //getActivity().finish();
                    return true;
                case R.id.cert_info:
                    MessageDialogFragment.showDialog(null, getString(R.string.cooin_cert_caption),
                            MsgUtils.getCertInfoMessage(selectedCooin.getCertificationRequest().
                            getCertificate(), getActivity()), getFragmentManager());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(selectedCooin.getReceipt().getSigner().
                            getTimeStampToken(), contextVS.getTimeStampCert(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.send_to_wallet:
                    if(contextVS.getWebSocketSession() == null) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                getString(R.string.send_to_wallet),
                                getString(R.string.send_to_wallet_connection_required_msg),
                                getActivity()).setPositiveButton(getString(R.string.accept_lbl), null);
                        UIUtils.showMessageDialog(builder);
                    } else SelectDeviceDialogFragment.showDialog(broadCastId, getActivity().
                            getSupportFragmentManager(), SelectDeviceDialogFragment.TAG);
                    break;
                case R.id.share_cooin:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = new String(selectedCooin.getReceipt().getBytes());
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, receiptStr);
                        sendIntent.setType(ContentTypeVS.TEXT.getName());
                        startActivity(sendIntent);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    public class CooinFetcher extends AsyncTask<String, String, ResponseVS> {

        public CooinFetcher() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String cooinURL = urls[0];
            return HttpHelper.getData(cooinURL, null);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    selectedCooin.setReceiptBytes(responseVS.getMessageBytes());
                    initCooinScreen(selectedCooin);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                            getString(R.string.exception_lbl), ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                        getString(R.string.error_lbl), responseVS.getMessage(),
                        getFragmentManager());
            }
            setProgressDialogVisible(false);
        }
    }


}