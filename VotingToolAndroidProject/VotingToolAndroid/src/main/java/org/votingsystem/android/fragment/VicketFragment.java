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
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.android.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketFragment extends Fragment {

    public static final String TAG = VicketFragment.class.getSimpleName();

    private Vicket selectedVicket;
    private TextView vicket_amount, vicket_state, vicket_currency, date_info;
    private SMIMEMessage selectedVicketSMIME;
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) ;
            else {
                ((ActivityVS)getActivity()).refreshingStateChanged(false);
                MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                        responseVS.getCaption(), responseVS.getNotificationMessage(),
                        getFragmentManager());
            }
        }
    };


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedVicket = (Vicket) getArguments().getSerializable(ContextVS.VICKET_KEY);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        broadCastId = VicketFragment.class.getSimpleName() + "_" + selectedVicket.getHashCertVS();
        View rootView = inflater.inflate(R.layout.vicket, container, false);
        vicket_amount = (TextView)rootView.findViewById(R.id.vicket_amount);
        vicket_state = (TextView)rootView.findViewById(R.id.vicket_state);
        vicket_currency = (TextView)rootView.findViewById(R.id.vicket_currency);
        date_info = (TextView)rootView.findViewById(R.id.date_info);
        initVicketScreen(selectedVicket);
        setHasOptionsMenu(true);
        return rootView;
    }

    private void initVicketScreen(Vicket vicket) {
        try {
            vicket_amount.setText(vicket.getAmount().toPlainString());
            vicket_currency.setText(vicket.getCurrencyCode());
            selectedVicketSMIME = vicket.getReceipt();
            getActivity().setTitle(MsgUtils.getVicketDescriptionMessage(vicket, getActivity()));
            date_info.setText(getString(R.string.vicket_date_info,
                    DateUtils.getDateStr(vicket.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                    DateUtils.getDateStr(vicket.getValidTo(), "dd MMM yyyy' 'HH:mm")));
            if(Vicket.State.OK != selectedVicket.getState()) {
                vicket_state.setText(MsgUtils.getVicketStateMessage(selectedVicket, getActivity()));
                vicket_state.setVisibility(View.VISIBLE);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedVicket != null) outState.putSerializable(ContextVS.RECEIPT_KEY, selectedVicket);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected model type:" + selectedVicket.getTypeVS());
        menuInflater.inflate(R.menu.vicket_fragment, menu);
        try {
            if(selectedVicket.getReceipt() == null) {
                menu.removeItem(R.id.show_timestamp_info);
                menu.removeItem(R.id.share_vicket);
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
                    MessageDialogFragment.showDialog(null, getString(R.string.vicket_cert_caption),
                            MsgUtils.getCertInfoMessage(selectedVicket.getCertificationRequest().
                            getCertificate(), getActivity()), getFragmentManager());
                    break;
                case R.id.show_timestamp_info:
                    TimeStampInfoDialogFragment newFragment = TimeStampInfoDialogFragment.newInstance(
                            selectedVicket.getReceipt().getSigner().getTimeStampToken(),
                            (AppContextVS) getActivity().getApplicationContext());
                    newFragment.show(getFragmentManager(), TimeStampInfoDialogFragment.TAG);
                    break;
                case R.id.cancel_vicket:
                    PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                            getString(R.string.cancel_vicket_dialog_msg), false, null);
                    break;
                case R.id.share_vicket:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = new String(selectedVicket.getReceipt().getBytes());
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

    public class VicketDownloader extends AsyncTask<String, String, ResponseVS> {

        public VicketDownloader() { }

        @Override protected void onPreExecute() {((ActivityVS)getActivity()).refreshingStateChanged(true); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String vicketURL = urls[0];
            return HttpHelper.getData(vicketURL, null);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    selectedVicket.setReceiptBytes(responseVS.getMessageBytes());
                    initVicketScreen(selectedVicket);
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
            ((ActivityVS)getActivity()).refreshingStateChanged(false);
        }
    }


}