package org.votingsystem.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.service.CooinService;
import org.votingsystem.android.service.TransactionVSService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TagVSInfo;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.CooinAccountsInfo;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinAccountsFragment extends Fragment {

	public static final String TAG = CooinAccountsFragment.class.getSimpleName();

    private String currencyCode;
    private TransactionVS transactionVS;
    private View rootView;
    private String broadCastId = CooinAccountsFragment.class.getSimpleName();
    private AppContextVS contextVS;
    private TextView last_request_date;
    private RecyclerView accounts_recycler_view;
    private String IBAN;
    private String pin;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            pin = intent.getStringExtra(ContextVS.PIN_KEY);
            switch(responseVS.getTypeVS()) {
                case COOIN_REQUEST:
                    sendCooinRequest(pin);
                    break;
                case COOIN_SEND:
                    sendCooin();
                    break;
                case TRANSACTIONVS:
                    sendTransactionVS();
                    break;
            }
        } else {
            switch(responseVS.getTypeVS()) {
                case COOIN_REQUEST:
                    if(ResponseVS.SC_PROCESSING == responseVS.getStatusCode()) {
                        transactionVS = (TransactionVS) responseVS.getData();
                        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                MsgUtils.getCooinRequestMessage(transactionVS, getActivity()),
                                false, TypeVS.COOIN_REQUEST);
                    } else {
                        MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                                responseVS.getCaption(), responseVS.getNotificationMessage(),
                                getFragmentManager());
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            loadUserInfo(DateUtils.getCurrentWeekPeriod());
                        }
                    }
                    break;
                case COOIN_SEND:
                    MessageDialogFragment.showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage(), getFragmentManager());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode())
                        loadUserInfo(DateUtils.getCurrentWeekPeriod());
                    break;
                case COOIN_ACCOUNTS_INFO:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        loadUserInfo(DateUtils.getCurrentWeekPeriod());
                    }
                    break;
                default: MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                        responseVS.getCaption(), responseVS.getNotificationMessage(),
                        getFragmentManager());
            }
            setProgressDialogVisible(false, null, null);
        }
        }
    };

    public static Fragment newInstance(Long representativeId) {
        CooinAccountsFragment fragment = new CooinAccountsFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(Bundle args) {
        CooinAccountsFragment fragment = new CooinAccountsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.uservs_accounts, container, false);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        //https://developer.android.com/training/material/lists-cards.html
        accounts_recycler_view = (RecyclerView) rootView.findViewById(R.id.accounts_recycler_view);
        accounts_recycler_view.setHasFixedSize(true);
        accounts_recycler_view.setLayoutManager(new LinearLayoutManager(getActivity()));
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
                CooinAccountsInfo userInfo = PrefUtils.getCooinAccountsInfo(contextVS);
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
                MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, caption,
                        getString(R.string.insufficient_cash_msg, transactionVS.getCurrencyCode(),
                                transactionVS.getAmount().toString(), cashAvailable), getFragmentManager());
            }
        }
    }

    private void loadUserInfo(DateUtils.TimePeriod timePeriod) {
        Date lastCheckedTime = PrefUtils.getCooinAccountsLastCheckDate(getActivity());
        if(lastCheckedTime == null) {
            updateCooinAccountsInfo();
            return;
        }
        try {
            last_request_date.setText(Html.fromHtml(getString(R.string.cooin_last_request_info_lbl,
                    DateUtils.getDayWeekDateStr(lastCheckedTime))));
            CooinAccountsInfo userInfo = PrefUtils.getCooinAccountsInfo(contextVS);
            if(userInfo != null) {
                Map<String, TagVSInfo> tagVSBalancesMap = userInfo.getTagVSBalancesMap(
                        Currency.getInstance("EUR").getCurrencyCode());
                if(tagVSBalancesMap != null) {
                    String[] tagVSArray = tagVSBalancesMap.keySet().toArray(new String[tagVSBalancesMap.keySet().size()]);
                    AccountVSInfoAdapter accountVSInfoAdapter = new AccountVSInfoAdapter(contextVS,
                            tagVSBalancesMap, Currency.getInstance("EUR").getCurrencyCode(), tagVSArray);
                    accounts_recycler_view.setAdapter(accountVSInfoAdapter);
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.uservs_accounts, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_signers_info:
                updateCooinAccountsInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void updateCooinAccountsInfo() {
        Intent startIntent = new Intent(getActivity(), CooinService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.COOIN_ACCOUNTS_INFO);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                getString(R.string.fetching_uservs_accounts_info_msg));
        getActivity().startService(startIntent);
    }

    private void sendCooinRequest(String pin) {
        Intent startIntent = new Intent(getActivity(), CooinService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.COOIN_REQUEST);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.PIN_KEY, pin);
        startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionVS);
        setProgressDialogVisible(true, getString(R.string.cooin_request_msg_subject),
                MsgUtils.getCooinRequestMessage(transactionVS, getActivity()));
        getActivity().startService(startIntent);
    }

    private void sendCooin() {
        Intent startIntent = new Intent(getActivity(), CooinService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.COOIN_SEND);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.IBAN_KEY, IBAN);
        startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionVS);
        setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg));
        getActivity().startService(startIntent);
    }

    private void sendTransactionVS() {
        Intent startIntent = new Intent(getActivity(), TransactionVSService.class);
        startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.TRANSACTIONVS);
        startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
        startIntent.putExtra(ContextVS.IBAN_KEY, IBAN);
        startIntent.putExtra(ContextVS.TRANSACTION_KEY, transactionVS);
        setProgressDialogVisible(true, getString(R.string.loading_data_msg),
                getString(R.string.loading_info_msg));
        getActivity().startService(startIntent);
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

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.TRANSACTION_KEY, transactionVS);
    }

    public class AccountVSInfoAdapter extends RecyclerView.Adapter<AccountVSInfoAdapter.ViewHolder>{

        private final Context context;
        private Map<String, TagVSInfo> tagVSListBalances;
        private List<String> tagVSList;
        private String currencyCode;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public View accountView;
            public ViewHolder(View accountView) {
                super(accountView);
                this.accountView = accountView;
            }
        }

        public AccountVSInfoAdapter(Context context, Map<String, TagVSInfo> tagVSListBalances,
                        String currencyCode, String[] tagArray) {
            this.context = context;
            this.currencyCode = currencyCode;
            this.tagVSListBalances = tagVSListBalances;
            tagVSList = new ArrayList<String>();
            tagVSList.addAll(tagVSListBalances.keySet());
        }

        @Override public AccountVSInfoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
            View accountView = ((LayoutInflater) context.getSystemService(Context.
                    LAYOUT_INFLATER_SERVICE)).inflate(R.layout.accountvs_card, parent, false);
            return new ViewHolder(accountView);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            View accountView = holder.accountView;
            TagVSInfo selectedTag = tagVSListBalances.get(tagVSList.get(position));
            final BigDecimal accountBalance = selectedTag.getCash();
            Button request_button = (Button) accountView.findViewById(R.id.cash_button);
            if(accountBalance.compareTo(BigDecimal.ZERO) == 1) {
                request_button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        CooinAccountsFragment.this.currencyCode = currencyCode;
                        CashDialogFragment.showDialog(getFragmentManager(), broadCastId,
                                getString(R.string.cash_request_dialog_caption),
                                getString(R.string.cash_dialog_msg, accountBalance, currencyCode),
                                accountBalance, currencyCode, TypeVS.COOIN_REQUEST);
                    }
                });
                request_button.setVisibility(View.VISIBLE);
            } else request_button.setVisibility(View.GONE);
            TextView tag_text = (TextView)accountView.findViewById(R.id.tag_text);
            String tag_text_msg = "'" + MsgUtils.getTagVSMessage(selectedTag.getName(), getActivity()) +
                    "' " + getString(R.string.currency_lbl) + " " + currencyCode;
            tag_text.setText(tag_text_msg);
            TextView cash_info = (TextView)accountView.findViewById(R.id.cash_info);
            cash_info.setText(Html.fromHtml(getString(R.string.account_amount_info_lbl,
                    accountBalance, currencyCode)));
            if(selectedTag.getTimeLimitedRemaining().compareTo(BigDecimal.ZERO) > 0) {
                String timeLimitedMsg = selectedTag.getTimeLimitedRemaining().toPlainString() +
                        " " + currencyCode + " " + getString(R.string.in_lbl) + " " +
                        selectedTag.getName();
                TextView time_limited_text = ((TextView)accountView.findViewById(R.id.time_limited_text));
                time_limited_text.setText(getString(R.string.time_remaining_info_lbl, timeLimitedMsg));
                time_limited_text.setVisibility(View.VISIBLE);
            }
        }

        @Override public int getItemCount() {
            return tagVSListBalances.size();
        }
    }

}