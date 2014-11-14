package org.votingsystem.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.service.TransactionVSService;
import org.votingsystem.android.service.VicketService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVSTransactionVSListInfo;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSAccountsFragment extends Fragment {

	public static final String TAG = UserVSAccountsFragment.class.getSimpleName();

    private String currencyCode;
    private ModalProgressDialogFragment progressDialog;
    private TransactionVS transactionVS;
    private View rootView;
    private String broadCastId = UserVSAccountsFragment.class.getSimpleName();
    private AppContextVS contextVS;
    private TextView last_request_date;
    private TextView time_remaining_info;
    private ListView accounts_list_view;
    private String IBAN;
    private String pin;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            pin = intent.getStringExtra(ContextVS.PIN_KEY);
            switch(responseVS.getTypeVS()) {
                case VICKET_USER_INFO:
                    sendUserInfoRequest();
                    break;
                case VICKET_REQUEST:
                    sendVicketRequest(pin);
                    break;
                case VICKET_SEND:
                    sendVicket();
                    break;
                case TRANSACTIONVS:
                    sendTransactionVS();
                    break;
            }
        } else {
            switch(responseVS.getTypeVS()) {
                case VICKET_REQUEST:
                    if(ResponseVS.SC_PROCESSING == responseVS.getStatusCode()) {
                        transactionVS = (TransactionVS) responseVS.getData();
                        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                MsgUtils.getVicketRequestMessage(transactionVS, getActivity()),
                                false, TypeVS.VICKET_REQUEST);
                    } else {
                        UIUtils.launchMessageActivity(getActivity(), responseVS);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {

                            loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
                        }
                    }
                    break;
                case VICKET_SEND:
                    ((ActivityVS)getActivity()).showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode())
                        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
                    break;
                case VICKET_USER_INFO:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
                } else  MessageDialogFragment.showDialog(responseVS, getFragmentManager());
                    break;
                default: ((ActivityVS)getActivity()).showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                        responseVS.getNotificationMessage());
            }
            ((ActivityVS)getActivity()).refreshingStateChanged(false);
            if(progressDialog != null) progressDialog.dismiss();
        }
        }
    };

    public static Fragment newInstance(Long representativeId) {
        UserVSAccountsFragment fragment = new UserVSAccountsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(Bundle args) {
        UserVSAccountsFragment fragment = new UserVSAccountsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.uservs_accounts, container, false);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        time_remaining_info = (TextView)rootView.findViewById(R.id.time_remaining_info);
        accounts_list_view = (ListView) rootView.findViewById(R.id.accounts_list_view);
        setHasOptionsMenu(true);
        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
        if(savedInstanceState != null) {
            transactionVS = (TransactionVS)savedInstanceState.getSerializable(ContextVS.TRANSACTION_KEY);
        }
        return rootView;
    }

    @Override public void onStart() {
        super.onStart();
        if(getArguments().getParcelable(ContextVS.URI_KEY) != null) {
            transactionVS = TransactionVS.parse((Uri) getArguments().getParcelable(
                    ContextVS.URI_KEY));
        }
        OperationVS operationVS = null;
        if(getArguments().getSerializable(ContextVS.OPERATIONVS_KEY) != null) {
            operationVS = (OperationVS)getArguments().getSerializable(ContextVS.OPERATIONVS_KEY);
            try {
                transactionVS = TransactionVS.parse(operationVS);
            } catch (Exception ex) {ex.printStackTrace();}
        }
        if(operationVS != null){
            BigDecimal cashAvailable = BigDecimal.ZERO;
            try {
                UserVSTransactionVSListInfo userInfo = PrefUtils.getUserVSTransactionVSListInfo(contextVS);
                IBAN = userInfo.getUserVS().getIBAN();
                cashAvailable = userInfo.getAvailableForTagVS(transactionVS.getCurrencyCode(),
                        transactionVS.getTagVS().getName());
            } catch(Exception ex) {ex.printStackTrace();}
            if(cashAvailable != null && cashAvailable.compareTo(transactionVS.getAmount()) == 1) {
                TransactionVSDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.transactionvs_send_pin_msg, transactionVS.getAmount(),
                            transactionVS.getCurrencyCode(), transactionVS.getToUserVS().getName(),
                            transactionVS.getSubject()), false, null);
            } else {
                String caption = null;
                if(transactionVS.getTagVS() != null){
                    caption = getString(R.string.insufficient_cash_for_tagvs_caption,
                            transactionVS.getTagVS().getName());
                } else caption = getString(R.string.insufficient_cash_caption);
                ((ActivityVS)getActivity()).showMessage(ResponseVS.SC_ERROR, caption,
                        getString(R.string.insufficient_cash_msg, transactionVS.getCurrencyCode(),
                        transactionVS.getAmount().toString(), cashAvailable));
            }
        }
    }

    private void loadUserInfo(DateUtils.TimePeriod timePeriod) {
        Date lastCheckedTime = PrefUtils.getLastVicketAccountCheckTime(getActivity());
        if(lastCheckedTime == null) {
            ((ActivityVS)getActivity()).showMessage(ResponseVS.SC_ERROR, getString(R.string.uservs_accountvs_info_missing_caption),
                    getString(R.string.uservs_accountvs_info_missing));
            return;
        }
        try {
            last_request_date.setText(Html.fromHtml(getString(R.string.vicket_last_request_info_lbl,
                    DateUtils.getDayWeekDateStr(lastCheckedTime))));
            time_remaining_info.setText(Html.fromHtml(getString(R.string.time_remaining_info_lbl,
                    DateUtils.getDayWeekDateStr(timePeriod.getDateTo()))));
            UserVSTransactionVSListInfo userInfo = PrefUtils.getUserVSTransactionVSListInfo(contextVS);
            if(userInfo != null) {
                Map<String, TagVS> tagVSBalancesMap = userInfo.getTagVSBalancesMap(
                        Currency.getInstance("EUR").getCurrencyCode());
                if(tagVSBalancesMap != null) {
                    String[] tagVSArray = tagVSBalancesMap.keySet().toArray(new String[tagVSBalancesMap.keySet().size()]);
                    AccountVSInfoAdapter accountVSInfoAdapter = new AccountVSInfoAdapter(contextVS,
                            tagVSBalancesMap, Currency.getInstance("EUR").getCurrencyCode(), tagVSArray);
                    accounts_list_view.setAdapter(accountVSInfoAdapter);
                    //accountVSInfoAdapter.notifyDataSetChanged();
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.vicket_user_info, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_signers_info:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.VICKET_USER_INFO);
                return true;
            case R.id.open_vicket_grid:
                UIUtils.launchEmbeddedFragment(VicketGridFragment.class.getName(), getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendUserInfoRequest() {
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                VicketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_USER_INFO);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        ((ActivityVS)getActivity()).refreshingStateChanged(true);
        getActivity().startService(startIntent);
    }

    private void sendVicketRequest(String pin) {
        progressDialog = ModalProgressDialogFragment.showDialog(getFragmentManager(),
                MsgUtils.getVicketRequestMessage(transactionVS, getActivity()),
                getString(R.string.vicket_request_msg_subject));
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                VicketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_REQUEST);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.PIN_KEY, pin);
        startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionVS);
        ((ActivityVS)getActivity()).refreshingStateChanged(true);
        getActivity().startService(startIntent);
    }

    private void sendVicket() {
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                VicketService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_SEND);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.IBAN_KEY, IBAN);
        startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionVS);
        ((ActivityVS)getActivity()).refreshingStateChanged(true);
        getActivity().startService(startIntent);
    }

    private void sendTransactionVS() {
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                TransactionVSService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.TRANSACTIONVS);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.IBAN_KEY, IBAN);
        startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionVS);
        ((ActivityVS)getActivity()).refreshingStateChanged(true);
        getActivity().startService(startIntent);
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

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.TRANSACTION_KEY, transactionVS);
    }

    public class AccountVSInfoAdapter extends ArrayAdapter<String> {
        private final Context context;
        private Map<String, TagVS> tagVSListBalances;
        private List<String> tagVSList;
        private String currencyCode;

        public AccountVSInfoAdapter(Context context, Map<String, TagVS> tagVSListBalances,
                    String currencyCode, String[] tagArray) {
            super(context, R.layout.accountvs_info, tagArray);
            this.context = context;
            this.currencyCode = currencyCode;
            this.tagVSListBalances = tagVSListBalances;
            tagVSList = new ArrayList<String>();
            tagVSList.addAll(tagVSListBalances.keySet());
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View accountView = inflater.inflate(R.layout.accountvs_info, parent, false);
            String selectedtag = tagVSList.get(position);
            final BigDecimal accountBalance = tagVSListBalances.get(selectedtag).getTotal();
            Button request_button = (Button) accountView.findViewById(R.id.request_button);
            if(accountBalance.compareTo(BigDecimal.ZERO) == 1) {
                request_button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        UserVSAccountsFragment.this.currencyCode = currencyCode;
                        CashDialogFragment.showDialog(getFragmentManager(), broadCastId,
                                getString(R.string.cash_request_dialog_caption),
                                getString(R.string.cash_dialog_msg, accountBalance, currencyCode),
                                accountBalance, currencyCode, TypeVS.VICKET_REQUEST);
                    }
                });
                request_button.setVisibility(View.VISIBLE);
            } else request_button.setVisibility(View.GONE);

            TextView vicket_account_info = (TextView)accountView.findViewById(R.id.vicket_account_info);
            vicket_account_info.setText(Html.fromHtml(getString(R.string.vicket_account_amount_info_lbl,
                    accountBalance, currencyCode)));
            //vicket_cash_info.setText(Html.fromHtml(getString(R.string.vicket_cash_amount_info_lbl,
            //        accountBalance, currencyCode)));
            TextView vicket_cash_info = (TextView)accountView.findViewById(R.id.vicket_cash_info);
            vicket_cash_info.setText("vicket_cash_info ");
            return accountView;
        }
    }

}