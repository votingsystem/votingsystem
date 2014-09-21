package org.votingsystem.android.fragment;

import android.app.Activity;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;

import org.votingsystem.android.service.VicketService;
import org.votingsystem.model.ContextVS;


import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVSTransactionVSListInfo;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSAccountsFragment extends Fragment {

	public static final String TAG = UserVSAccountsFragment.class.getSimpleName();

    private static final int ADMIN_ACCESS_CONTROL = 1;

    private BigDecimal amount;
    private String tagVS;
    private String currencyCode = Currency.getInstance("EUR").getCurrencyCode();
    private Uri uriData;
    private String IBAN;
    private String subject;
    private String receptor;
    private View rootView;
    private String broadCastId = UserVSAccountsFragment.class.getSimpleName();
    private AppContextVS contextVS;
    private View progressContainer;
    private TextView lapse_info;
    private TextView last_request_date;
    private TextView time_remaining_info;
    private ListView accounts_list_view;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private Menu fragmentMenu;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case VICKET_USER_INFO:
                    sendUserInfoRequest();
                    break;
                case VICKET_REQUEST:
                    sendVicketRequest();
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
                        amount = (BigDecimal) responseVS.getData();
                        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                getString(R.string.vicket_request_pin_msg, amount,
                                currencyCode), false, TypeVS.VICKET_REQUEST);
                    } else {
                        showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                                responseVS.getNotificationMessage());
                        if(ResponseVS.SC_OK == responseVS.getStatusCode())
                            loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
                    }
                    break;
                case VICKET_SEND:
                    showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode())
                        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
                    break;
                case VICKET_USER_INFO:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
                    } else showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage());
                    break;
                case INIT_VALIDATED_SESSION:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        if(fragmentMenu != null) {
                            MenuItem connectToServiceMenuItem = fragmentMenu.findItem(R.id.connect_to_service);
                            connectToServiceMenuItem.setTitle(getString(R.string.disconnect_from_service_lbl));
                        }
                    }
                    break;
                case WEB_SOCKET_CLOSE:
                    if(fragmentMenu != null) {
                        MenuItem connectToServiceMenuItem = fragmentMenu.findItem(R.id.connect_to_service);
                        connectToServiceMenuItem.setTitle(getString(R.string.connect_to_service_lbl));
                    }
                    break;
                default: showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                        responseVS.getNotificationMessage());
            }
            showProgress(false, false);
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

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        rootView = inflater.inflate(R.layout.uservs_accounts_fragment, container, false);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        time_remaining_info = (TextView)rootView.findViewById(R.id.time_remaining_info);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        accounts_list_view = (ListView) rootView.findViewById(R.id.accounts_list_view);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        loadUserInfo(DateUtils.getWeekPeriod(Calendar.getInstance()));
        if(savedInstanceState != null) {
            currencyCode = (String) savedInstanceState.getSerializable(ContextVS.CURRENCY_KEY);
            amount = (BigDecimal) savedInstanceState.getSerializable(ContextVS.VALUE_KEY);
            IBAN = savedInstanceState.getString(ContextVS.IBAN_KEY);
        }
        return rootView;
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart()", "onStart");
        super.onStart();
        uriData = getArguments().getParcelable(ContextVS.URI_KEY);
        if(uriData != null) {
            amount = new BigDecimal(uriData.getQueryParameter("amount"));
            tagVS = uriData.getQueryParameter("tagVS");
            if(tagVS == null || tagVS.isEmpty()) tagVS = TagVS.WILDTAG;
            currencyCode = uriData.getQueryParameter("currencyCode");
            subject = uriData.getQueryParameter("subject");
            receptor = uriData.getQueryParameter("receptor");
            Log.d(TAG + ".onStart(...)", "tag: " +  tagVS + " - amount: " + amount + " - currency: "
                    + currencyCode + " - subject: " + subject + " - receptor: " + receptor);
            BigDecimal cashAvailable = null;
            try {
                cashAvailable = contextVS.getUserVSTransactionVSListInfo().getAvailableForTagVS(
                        currencyCode, tagVS);
            } catch(Exception ex) {ex.printStackTrace();}
            if(cashAvailable != null && cashAvailable.compareTo(amount) == 1) {
                TransactionVSDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.transactionvs_send_pin_msg, amount,
                                currencyCode.toString(), receptor, subject), false);
            } else {
                showMessage(ResponseVS.SC_ERROR, getString(R.string.insufficient_cash_caption),
                        getString(R.string.insufficient_cash_msg, currencyCode,
                                amount.toString(), cashAvailable));
            }
        }
    }
    private void loadUserInfo(DateUtils.TimePeriod timePeriod) {
        Date lastCheckedTime = contextVS.getVicketAccountLastChecked();
        if(lastCheckedTime == null) {
            showMessage(ResponseVS.SC_ERROR, getString(R.string.uservs_accountvs_info_missing_caption),
                    getString(R.string.uservs_accountvs_info_missing));
            return;
        }
        try {
            last_request_date.setText(Html.fromHtml(getString(R.string.vicket_last_request_info_lbl,
                    DateUtils.getLongDate_Es(lastCheckedTime))));
            time_remaining_info.setText(Html.fromHtml(getString(R.string.time_remaining_info_lbl,
                    DateUtils.getLongDate_Es(timePeriod.getDateTo()))));
            UserVSTransactionVSListInfo userInfo = contextVS.getUserVSTransactionVSListInfo();
            if(userInfo != null) {
                Map<String, BigDecimal> tagVSBalancesMap = contextVS.getUserVSTransactionVSListInfo().
                        getTagVSBalancesMap(Currency.getInstance("EUR").getCurrencyCode());
                String[] tagVSArray = tagVSBalancesMap.keySet().toArray(new String[tagVSBalancesMap.keySet().size()]);
                AccountVSInfoAdapter accountVSInfoAdapter = new AccountVSInfoAdapter(contextVS,
                        tagVSBalancesMap, Currency.getInstance("EUR").getCurrencyCode(), tagVSArray);
                accounts_list_view.setAdapter(accountVSInfoAdapter);
                accountVSInfoAdapter.notifyDataSetChanged();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG + ".onActivityResult(...)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        int statusCode = -1;
        String caption = null;
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            statusCode = ResponseVS.SC_OK;
            caption = getString(R.string.operation_ok_msg);
            showMessage(statusCode, caption, message);
        } else if(message != null) {
            statusCode = ResponseVS.SC_ERROR;
            caption = getString(R.string.operation_error_msg);
            showMessage(statusCode, caption, message);
        }
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.vicket_user_info, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
        fragmentMenu = menu;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_signers_info:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.VICKET_USER_INFO);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendUserInfoRequest() {
        Log.d(TAG + ".sendUserInfoRequest(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VicketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_USER_INFO);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendVicketRequest() {
        Log.d(TAG + ".sendVicketRequest(...) ", "amount: " + amount);
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VicketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_REQUEST);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.VALUE_KEY, amount);
            startIntent.putExtra(ContextVS.CURRENCY_KEY, currencyCode);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendVicket() {
        Log.d(TAG + ".sendVicket(...) ", "amount: " + amount);
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VicketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_SEND);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URI_KEY, uriData);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendTransactionVS() {
        Log.d(TAG + ".sendTransactionVS(...) - TODO - ", "sendTransactionVS: " + amount);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));

    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        outState.putSerializable(ContextVS.CURRENCY_KEY, currencyCode);
        outState.putSerializable(ContextVS.VALUE_KEY, amount);
        outState.putSerializable(ContextVS.IBAN_KEY, IBAN);
    }

    public class AccountVSInfoAdapter extends ArrayAdapter<String> {
        private final Context context;
        private Map<String, BigDecimal> tagVSListBalances;
        private List<String> tagVSList;
        private String currencyCode;

        public AccountVSInfoAdapter(Context context, Map<String, BigDecimal> tagVSListBalances,
                    String currencyCode, String[] tagArray) {
            super(context, R.layout.accountvs_info, tagArray);
            this.context = context;
            this.currencyCode = currencyCode;
            this.tagVSListBalances = tagVSListBalances;
            tagVSList = new ArrayList<String>();
            tagVSList.addAll(tagVSListBalances.keySet());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View accountView = inflater.inflate(R.layout.accountvs_info, parent, false);
            String selectedtag = tagVSList.get(position);
            final BigDecimal accountBalance = tagVSListBalances.get(selectedtag);
            Button request_button = (Button) accountView.findViewById(R.id.request_button);
            if(accountBalance.compareTo(BigDecimal.ZERO) == 1) {
                request_button.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        CashDialogFragment.showDialog(getFragmentManager(), broadCastId,
                                getString(R.string.cash_request_dialog_caption),
                                getString(R.string.cash_dialog_msg, accountBalance, currencyCode),
                                accountBalance, TypeVS.VICKET_REQUEST);
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