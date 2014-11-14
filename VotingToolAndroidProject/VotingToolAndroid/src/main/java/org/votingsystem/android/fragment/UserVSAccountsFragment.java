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
import org.votingsystem.android.activity.ActivityBase;
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.service.TransactionVSService;
import org.votingsystem.android.service.VicketService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TagVSInfo;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVSAccountsInfo;
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
public class UserVSAccountsFragment extends Fragment {

	public static final String TAG = UserVSAccountsFragment.class.getSimpleName();

    private String currencyCode;
    private ModalProgressDialogFragment progressDialog;
    private TransactionVS transactionVS;
    private View rootView;
    private String broadCastId = UserVSAccountsFragment.class.getSimpleName();
    private AppContextVS contextVS;
    private TextView last_request_date;
    private ListView accounts_list_view;
    private String IBAN;
    private String pin;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            pin = intent.getStringExtra(ContextVS.PIN_KEY);
            switch(responseVS.getTypeVS()) {
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
                            loadUserInfo(DateUtils.getCurrentWeekPeriod());
                        }
                    }
                    break;
                case VICKET_SEND:
                    MessageDialogFragment.showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage(), getFragmentManager());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode())
                        loadUserInfo(DateUtils.getCurrentWeekPeriod());
                    break;
                case VICKET_USER_INFO:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        loadUserInfo(DateUtils.getCurrentWeekPeriod());
                    }
                    break;
                default: MessageDialogFragment.showDialog(responseVS.getStatusCode(),
                        responseVS.getCaption(), responseVS.getNotificationMessage(),
                        getFragmentManager());
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
        accounts_list_view = (ListView) rootView.findViewById(R.id.accounts_list_view);
        setHasOptionsMenu(true);
        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
        if(savedInstanceState != null) {
            transactionVS = (TransactionVS)savedInstanceState.getSerializable(ContextVS.TRANSACTION_KEY);
        }
        if(getActivity() instanceof ActivityBase) {
            ((ActivityBase)getActivity()).enableDisableSwipeRefresh(false);
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
                UserVSAccountsInfo userInfo = PrefUtils.getUserVSAccountsInfo(contextVS);
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
        Date lastCheckedTime = PrefUtils.getUserVSAccountsLastCheckDate(getActivity());
        if(lastCheckedTime == null) {
            MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(
                    R.string.uservs_accountvs_info_missing_caption),  getString(
                    R.string.uservs_accountvs_info_missing), getFragmentManager());
            return;
        }
        try {
            last_request_date.setText(Html.fromHtml(getString(R.string.vicket_last_request_info_lbl,
                    DateUtils.getDayWeekDateStr(lastCheckedTime))));
            UserVSAccountsInfo userInfo = PrefUtils.getUserVSAccountsInfo(contextVS);
            if(userInfo != null) {
                Map<String, TagVSInfo> tagVSBalancesMap = userInfo.getTagVSBalancesMap(
                        Currency.getInstance("EUR").getCurrencyCode());
                if(tagVSBalancesMap != null) {
                    String[] tagVSArray = tagVSBalancesMap.keySet().toArray(new String[tagVSBalancesMap.keySet().size()]);
                    AccountVSInfoAdapter accountVSInfoAdapter = new AccountVSInfoAdapter(contextVS,
                            tagVSBalancesMap, Currency.getInstance("EUR").getCurrencyCode(), tagVSArray);
                    accounts_list_view.setAdapter(accountVSInfoAdapter);
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
                sendUserInfoRequest();
                return true;
            case R.id.open_vicket_grid:
                UIUtils.launchEmbeddedFragment(WalletFragment.class.getName(), getActivity());
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
        progressDialog = ModalProgressDialogFragment.showDialog(
                MsgUtils.getVicketRequestMessage(transactionVS, getActivity()),
                getString(R.string.vicket_request_msg_subject), getFragmentManager());
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
        private Map<String, TagVSInfo> tagVSListBalances;
        private List<String> tagVSList;
        private String currencyCode;

        public AccountVSInfoAdapter(Context context, Map<String, TagVSInfo> tagVSListBalances,
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
            TagVSInfo selectedTag = tagVSListBalances.get(tagVSList.get(position));
            final BigDecimal accountBalance = selectedTag.getCash();
            Button request_button = (Button) accountView.findViewById(R.id.cash_button);
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
            return accountView;
        }
    }

}