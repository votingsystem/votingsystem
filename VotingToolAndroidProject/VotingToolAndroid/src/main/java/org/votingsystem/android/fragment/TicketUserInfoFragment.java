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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.service.TicketService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CurrencyData;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketUserInfoFragment extends Fragment {

	public static final String TAG = "TicketUserInfoFragment";

    private BigDecimal withdrawalAmount;
    private View rootView;
    private String broadCastId = null;
    private AppContextVS contextVS;
    private Button withdrawal_button;
    private View progressContainer;
    private TextView ticket_account_info;
    private TextView ticket_cash_info;
    private TextView last_request_date;
    private TextView time_remaining_info;
    private FrameLayout mainLayout;
    private TicketAccount ticketUserInfo;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras: " + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(pin != null) {
            switch(responseVS.getTypeVS()) {
                case TICKET_USER_INFO:
                    launchUpdateUserInfoService(pin);
                    break;
                case TICKET_REQUEST_DIALOG:
                    launchTicketWithdrawal(pin);
                    break;
            }

        } else {
            switch(responseVS.getTypeVS()) {
                case TICKET_REQUEST:
                    showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) loadUserInfo();
                    break;
                case TICKET_REQUEST_DIALOG:
                    Log.d(TAG + ".broadcastReceiver.onReceive(...)", "TICKET_REQUEST_DIALOG: " + responseVS.getData());
                    withdrawalAmount = (BigDecimal) responseVS.getData();
                    PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                            getString(R.string.ticket_request_pin_msg, withdrawalAmount), false,
                            TypeVS.TICKET_REQUEST_DIALOG);
                    break;
                case TICKET_USER_INFO:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        loadUserInfo();
                    } else showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage());
                    break;
            }
            showProgress(false, false);
        }
        }
    };


    public static Fragment newInstance(Long representativeId) {
        TicketUserInfoFragment fragment = new TicketUserInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadCastId = this.getClass().getSimpleName();
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        contextVS = (AppContextVS) getActivity().getApplicationContext();

        rootView = inflater.inflate(R.layout.ticket_user_info, container, false);
        ticket_account_info = (TextView)rootView.findViewById(R.id.ticket_account_info);
        ticket_cash_info = (TextView)rootView.findViewById(R.id.ticket_cash_info);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        time_remaining_info = (TextView)rootView.findViewById(R.id.time_remaining_info);
        withdrawal_button = (Button) rootView.findViewById(R.id.withdrawal_button);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        loadUserInfo();
        return rootView;
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart()", "onStart");
        super.onStart();
        Uri uriData = getArguments().getParcelable(ContextVS.URI_KEY);
        if(uriData != null) {
            String amount = uriData.getQueryParameter("amount");
            Log.d(TAG + ".onCreateView(...)", "amount: " + amount);
        }
    }
    private void loadUserInfo() {
        ticketUserInfo = ((AppContextVS)getActivity().getApplicationContext()).getTicketAccount();
        if(ticketUserInfo == null) {
            showMessage(ResponseVS.SC_ERROR, getString(R.string.empty_ticket_user_info_caption),
                    getString(R.string.empty_ticket_user_info));
            return;
        }
        final CurrencyData currencyData;
        if(ticketUserInfo!= null && ticketUserInfo.getCurrencyMap() != null) {
            currencyData = ticketUserInfo.getCurrencyMap().get(CurrencyVS.Euro);
        } else currencyData = null;


        if(currencyData != null) {
            withdrawal_button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CashWithdrawalDialogFragment.showDialog(getFragmentManager(), broadCastId,
                            getString(R.string.cash_withdrawal_dialog_caption),
                            getString(R.string.cash_withdrawal_dialog_msg, currencyData.getAccountBalance(),
                                    CurrencyVS.Euro.toString()),
                            currencyData.getAccountBalance(), null);
                }
            });
            last_request_date.setText(Html.fromHtml(getString(R.string.ticket_last_request_info_lbl,
                    DateUtils.getLongDate_Es(ticketUserInfo.getLastRequestDate()))));
            ticket_account_info.setText(Html.fromHtml(getString(R.string.ticket_account_amount_info_lbl,
                    currencyData.getAccountBalance())));
            ticket_cash_info.setText(Html.fromHtml(getString(R.string.ticket_cash_amount_info_lbl,
                    currencyData.getCashBalance())));

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            time_remaining_info.setText(Html.fromHtml(getString(R.string.time_remaining_info_lbl,
                    DateUtils.getLongDate_Es(DateUtils.getNextMonday(calendar.getTime()).getTime()))));
        }
        if (currencyData == null || !(currencyData.getAccountBalance().intValue() > 0)) {
            withdrawal_button.setVisibility(View.GONE);
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
        menuInflater.inflate(R.menu.ticket_user_info, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_signers_info:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.TICKET_USER_INFO);
                return true;
            case R.id.send_message:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchUpdateUserInfoService(String pin) {
        Log.d(TAG + ".launchUpdateUserInfoService(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    TicketService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.TICKET_USER_INFO);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void launchTicketWithdrawal(String pin) {
        Log.d(TAG + ".launchTicketWithdrawal(...) ", "amount: " + withdrawalAmount);
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    TicketService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.TICKET_REQUEST);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.VALUE_KEY, withdrawalAmount);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
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
    }

}