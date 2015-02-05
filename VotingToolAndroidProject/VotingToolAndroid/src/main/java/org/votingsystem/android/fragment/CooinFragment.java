package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.WalletActivity;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.WebSocketMessage;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.util.Arrays;

import static org.votingsystem.util.LogUtils.LOGD;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinFragment extends Fragment {

    public static final String TAG = CooinFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private Cooin cooin;
    private TextView cooin_amount, cooin_state, cooin_currency, date_info;
    private SMIMEMessage cooinSMIME;
    private String broadCastId = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        WebSocketMessage socketMsg = intent.getParcelableExtra(ContextVS.WEBSOCKET_MSG_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case WEB_SOCKET_INIT:
                    setProgressDialogVisible(true, getString(R.string.connecting_caption),
                            getString(R.string.connecting_to_service_msg));
                    Utils.toggleWebSocketServiceConnection(contextVS);
                    break;
            }
        } else if(socketMsg != null){
            setProgressDialogVisible(false, null, null);
            switch(socketMsg.getOperation()) {
                case INIT_VALIDATED_SESSION:
                    break;
                case MESSAGEVS_TO_DEVICE:
                    break;
                case COOIN_WALLET_CHANGE:
                    if(ResponseVS.SC_OK == socketMsg.getStatusCode()) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                            getString(R.string.send_to_wallet),
                            getString(R.string.item_sended_ok_msg),
                            getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    startActivity(new Intent(contextVS, WalletActivity.class));
                                }
                            });
                        UIUtils.showMessageDialog(builder);
                    } else MessageDialogFragment.showDialog(socketMsg.getStatusCode(),
                                getString(R.string.error_lbl), getString(R.string.device_not_found_error_msg),
                                getFragmentManager());
                    break;
                default:
                    LOGD(TAG + ".broadcastReceiver", "socketMsg: " + socketMsg.getOperation());
            }
        } else {
            setProgressDialogVisible(false, null, null);
            switch(responseVS.getTypeVS()) {
                case INIT_VALIDATED_SESSION:
                    break;
                case DEVICE_SELECT:
                    try {
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            DeviceVS targetDevice = DeviceVS.parse(responseVS.getMessageJSON());
                            JSONObject requestJSON = WebSocketMessage.getCooinWalletChangeRequest(
                                    targetDevice, Arrays.asList(cooin), contextVS);
                            responseVS = new ResponseVS(ResponseVS.SC_OK, requestJSON.toString());
                            Intent startIntent = new Intent(getActivity(), WebSocketService.class);
                            startIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                            setProgressDialogVisible(true, getString(R.string.send_to_wallet),
                                    getString(R.string.connecting_lbl));
                            getActivity().startService(startIntent);
                            Toast.makeText(getActivity(),
                                    getString(R.string.send_to_wallet) + " - " +
                                    getString(R.string.check_target_device_lbl),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch(Exception ex) {ex.printStackTrace();}
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
        cooin = (Cooin) getArguments().getSerializable(ContextVS.COOIN_KEY);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        broadCastId = CooinFragment.class.getSimpleName() + "_" + cooin.getHashCertVS();
        View rootView = inflater.inflate(R.layout.cooin, container, false);
        cooin_amount = (TextView)rootView.findViewById(R.id.cooin_amount);
        cooin_state = (TextView)rootView.findViewById(R.id.cooin_state);
        cooin_currency = (TextView)rootView.findViewById(R.id.cooin_currency);
        date_info = (TextView)rootView.findViewById(R.id.date_info);
        initCooinScreen(cooin);
        setHasOptionsMenu(true);
        return rootView;
    }

    private void initCooinScreen(Cooin cooin) {
        try {
            cooin_amount.setText(cooin.getAmount().toPlainString());
            cooin_currency.setText(cooin.getCurrencyCode());
            cooinSMIME = cooin.getReceipt();
            getActivity().setTitle(MsgUtils.getCooinDescriptionMessage(cooin, getActivity()));
            date_info.setText(getString(R.string.cooin_date_info,
                    DateUtils.getDateStr(cooin.getDateFrom(), "dd MMM yyyy' 'HH:mm"),
                    DateUtils.getDateStr(cooin.getDateTo(), "dd MMM yyyy' 'HH:mm")));
            if(Cooin.State.OK != cooin.getState()) {
                cooin_state.setText(MsgUtils.getCooinStateMessage(cooin, getActivity()));
                cooin_state.setVisibility(View.VISIBLE);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.loading_data_msg);
            if(message == null) message = getString(R.string.loading_info_msg);
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(cooin != null) outState.putSerializable(ContextVS.RECEIPT_KEY, cooin);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected model type:" + cooin.getTypeVS());
        menuInflater.inflate(R.menu.cooin_fragment, menu);
        try {
            if(cooin.getReceipt() == null) {
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
                            MsgUtils.getCertInfoMessage(cooin.getCertificationRequest().
                            getCertificate(), getActivity()), getFragmentManager());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(cooin.getReceipt().getSigner().
                            getTimeStampToken(), contextVS.getTimeStampCert(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.send_to_wallet:
                    if(!contextVS.hasWebSocketConnection()) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                getString(R.string.send_to_wallet),
                                getString(R.string.send_to_wallet_connection_required_msg),
                                getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                            PinDialogFragment.showPinScreen(getFragmentManager(),
                                            broadCastId, getString(R.string.init_authenticated_session_pin_msg),
                                            false, TypeVS.WEB_SOCKET_INIT);
                                    }
                                }).setNegativeButton(getString(R.string.cancel_lbl), null);
                        UIUtils.showMessageDialog(builder);
                    } else SelectDeviceDialogFragment.showDialog(broadCastId, getActivity().
                            getSupportFragmentManager(), SelectDeviceDialogFragment.TAG);
                    break;
                case R.id.share_cooin:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = new String(cooin.getReceipt().getBytes());
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

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.loading_data_msg), getString(R.string.loading_info_msg)); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String cooinURL = urls[0];
            return HttpHelper.getData(cooinURL, null);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    cooin.setReceiptBytes(responseVS.getMessageBytes());
                    initCooinScreen(cooin);
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
            setProgressDialogVisible(false, null, null);
        }
    }


}