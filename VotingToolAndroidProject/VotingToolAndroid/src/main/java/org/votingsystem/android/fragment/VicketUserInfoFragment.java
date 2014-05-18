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
import org.votingsystem.android.activity.BrowserVSActivity;
import org.votingsystem.android.contentprovider.Utils;
import org.votingsystem.android.service.VicketService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CurrencyData;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VicketUserInfoFragment extends Fragment {

	public static final String TAG = VicketUserInfoFragment.class.getSimpleName();

    private static final int ADMIN_ACCESS_CONTROL = 1;

    private BigDecimal amount;
    private CurrencyVS currencyVS = CurrencyVS.EURO;
    private Uri uriData;
    private String IBAN;
    private String subject;
    private String receptor;
    private View rootView;
    private String broadCastId = null;
    private AppContextVS contextVS;
    private Button request_button;
    private View progressContainer;
    private TextView vicket_account_info;
    private TextView lapse_info;
    private TextView vicket_cash_info;
    private TextView last_request_date;
    private TextView time_remaining_info;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras: " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case VICKET_USER_INFO:
                    launchUpdateUserInfoService();
                    break;
                case VICKET_REQUEST:
                    launchVicketRequest();
                    break;
                case VICKET_SEND:
                    launchVicketSend();
                    break;
            }

        } else {
            switch(responseVS.getTypeVS()) {
                case VICKET_REQUEST:
                    if(ResponseVS.SC_PROCESSING == responseVS.getStatusCode()) {
                        amount = (BigDecimal) responseVS.getData();
                        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                getString(R.string.vicket_request_pin_msg, amount,
                                currencyVS.toString()), false,
                                TypeVS.VICKET_REQUEST);
                    } else {
                        showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                                responseVS.getNotificationMessage());
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) loadUserInfo();
                    }
                    break;
                case VICKET_SEND:
                    showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                            responseVS.getNotificationMessage());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) loadUserInfo();
                    break;
                case VICKET_USER_INFO:
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
        VicketUserInfoFragment fragment = new VicketUserInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.USER_KEY, representativeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadCastId = VicketUserInfoFragment.class.getSimpleName();
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        contextVS = (AppContextVS) getActivity().getApplicationContext();

        rootView = inflater.inflate(R.layout.vicket_user_info, container, false);
        vicket_account_info = (TextView)rootView.findViewById(R.id.vicket_account_info);
        vicket_cash_info = (TextView)rootView.findViewById(R.id.vicket_cash_info);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        time_remaining_info = (TextView)rootView.findViewById(R.id.time_remaining_info);

        request_button = (Button) rootView.findViewById(R.id.request_button);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        loadUserInfo();
        if(savedInstanceState != null) {
            currencyVS = (CurrencyVS) savedInstanceState.getSerializable(ContextVS.CURRENCY_KEY);
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
            currencyVS = CurrencyVS.valueOf(uriData.getQueryParameter("currency"));
            subject = uriData.getQueryParameter("subject");
            receptor = uriData.getQueryParameter("receptor");
            Log.d(TAG + ".onStart(...)", "amount: " + amount + " - subject: " + subject +
                    " - receptor: " + receptor);
            CurrencyData currencyData = Utils.getCurrencyData(contextVS, currencyVS);
            BigDecimal cashAvailable = currencyData.getCashBalance();
            if(cashAvailable != null && cashAvailable.compareTo(amount) >= 0) {
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.vicket_send_pin_msg, amount,
                                currencyVS.toString(), receptor, subject), false, TypeVS.VICKET_SEND);
            } else {
                showMessage(ResponseVS.SC_ERROR, getString(R.string.insufficient_cash_caption),
                        getString(R.string.insufficient_cash_msg, currencyVS.toString(),
                                amount.toString(), cashAvailable.toString()));
            }
        }
    }
    private void loadUserInfo() {
        Date lastCheckedTime = contextVS.getVicketAccountLastChecked();
        if(lastCheckedTime == null) {
            showMessage(ResponseVS.SC_ERROR, getString(R.string.empty_vicket_user_info_caption),
                    getString(R.string.empty_vicket_user_info, contextVS.getLapseWeekLbl(
                            Calendar.getInstance())));
            return;
        }
        final CurrencyData currencyData = Utils.getCurrencyData(contextVS, CurrencyVS.EURO);
        if(currencyData != null) {
            request_button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CashDialogFragment.showDialog(getFragmentManager(), broadCastId,
                            getString(R.string.cash_request_dialog_caption),
                            getString(R.string.cash_dialog_msg,
                                    currencyData.getAccountBalance(), CurrencyVS.EURO.toString()),
                            currencyData.getAccountBalance(), TypeVS.VICKET_REQUEST);
                }
            });
            request_button.setVisibility(View.VISIBLE);
            last_request_date.setText(Html.fromHtml(getString(R.string.vicket_last_request_info_lbl,
                    DateUtils.getLongDate_Es(lastCheckedTime))));
            vicket_account_info.setText(Html.fromHtml(getString(R.string.vicket_account_amount_info_lbl,
                    currencyData.getAccountBalance(), currencyVS.toString())));
            vicket_cash_info.setText(Html.fromHtml(getString(R.string.vicket_cash_amount_info_lbl,
                    currencyData.getCashBalance(), currencyVS.toString())));

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            time_remaining_info.setText(Html.fromHtml(getString(R.string.time_remaining_info_lbl,
                    DateUtils.getLongDate_Es(DateUtils.getMonday(calendar).getTime()))));
        }
        if (currencyData == null || !(currencyData.getAccountBalance().intValue() > 0)) {
            request_button.setVisibility(View.GONE);
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
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_signers_info:

                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.VICKET_USER_INFO);
                return true;
            case R.id.send_message:

                return true;
            case R.id.test_menu_item:
                Intent intent = new Intent(getActivity(), BrowserVSActivity.class);
                intent.putExtra(ContextVS.URL_KEY, "http://vickets/Vickets/app/admin?menu=admin");
                startActivityForResult(intent, ADMIN_ACCESS_CONTROL);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchUpdateUserInfoService() {
        Log.d(TAG + ".launchUpdateUserInfoService(...) ", "");
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

    private void launchVicketRequest() {
        Log.d(TAG + ".launchVicketRequest(...) ", "amount: " + amount);
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VicketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_REQUEST);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.VALUE_KEY, amount);
            startIntent.putExtra(ContextVS.CURRENCY_KEY, currencyVS);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void launchVicketSend() {
        Log.d(TAG + ".launchVicketSend(...) ", "amount: " + amount);
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
        outState.putSerializable(ContextVS.CURRENCY_KEY, currencyVS);
        outState.putSerializable(ContextVS.VALUE_KEY, amount);
        outState.putSerializable(ContextVS.IBAN_KEY, IBAN);
    }

}