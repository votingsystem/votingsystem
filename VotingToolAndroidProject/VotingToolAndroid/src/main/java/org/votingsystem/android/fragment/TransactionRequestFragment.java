package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CashRequestFormActivity;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.service.CooinService;
import org.votingsystem.android.service.TransactionVSService;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CooinAccountsInfo;
import org.votingsystem.model.Payment;
import org.votingsystem.model.TransactionRequest;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.CALLER_KEY;
import static org.votingsystem.model.ContextVS.JSON_DATA_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.TYPEVS_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionRequestFragment extends Fragment {

	public static final String TAG = TransactionRequestFragment.class.getSimpleName();

    private static final int COOIN_REQUEST   = 1;

    private String broadCastId = TransactionRequestFragment.class.getSimpleName();
    private TextView receptor;
    private TextView subject;
    private TextView amount;
    private TextView currency;
    private TransactionRequest transactionRequest;
    private Spinner payment_method_spinner;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            String pin = intent.getStringExtra(PIN_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(pin != null) {
                switch(responseVS.getTypeVS()) {
                    case SIGNED_TRANSACTION:
                        launchPayment(TypeVS.SIGNED_TRANSACTION);
                        break;
                    case ANONYMOUS_SIGNED_TRANSACTION:
                        launchPayment(TypeVS.ANONYMOUS_SIGNED_TRANSACTION);
                        break;
                    case COOIN:
                        break;
                }

            }
            else {
                setProgressDialogVisible(false);
                String caption = ResponseVS.SC_OK == responseVS.getStatusCode()?getString(
                        R.string.payment_ok_caption):getString(R.string.error_lbl);
                MessageDialogFragment.showDialog(responseVS.getStatusCode(), caption,
                        responseVS.getMessage(), getFragmentManager());
            }
        }
    };

    private void launchPayment(TypeVS paymentType) {
        LOGD(TAG + ".launchPayment() ", "launchPayment");
        Intent startIntent = new Intent(getActivity(), TransactionVSService.class);
        try {
            startIntent.putExtra(JSON_DATA_KEY, transactionRequest.toJSON().toString());
            startIntent.putExtra(CALLER_KEY, broadCastId);
            startIntent.putExtra(TYPEVS_KEY, paymentType);
            getActivity().startService(startIntent);
            setProgressDialogVisible(true);
        } catch (JSONException ex) { ex.printStackTrace(); }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.transaction_request_fragment, container, false);
        getActivity().setTitle(getString(R.string.payment_lbl));
        receptor = (TextView)rootView.findViewById(R.id.receptor);
        subject = (TextView)rootView.findViewById(R.id.subject);
        amount= (TextView)rootView.findViewById(R.id.amount);
        currency= (TextView)rootView.findViewById(R.id.currency);
        payment_method_spinner = (Spinner)rootView.findViewById(R.id.payment_method_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.transaction_request_spinner_item, Payment.getPaymentMethods(getActivity()));
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        payment_method_spinner.setAdapter(dataAdapter);
        try {
            JSONObject jsonObject = new JSONObject(getArguments().getString(ContextVS.JSON_DATA_KEY));
            transactionRequest = TransactionRequest.parse(jsonObject);
            receptor.setText(transactionRequest.getToUser());
            subject.setText(transactionRequest.getSubject());
            amount.setText(transactionRequest.getAmount().toString());
            currency.setText(transactionRequest.getCurrencyCode());
            switch(transactionRequest.getType()) {
                case DELIVERY_WITH_PAYMENT:
                case DELIVERY_WITHOUT_PAYMENT:
                    UIUtils.fillAddressInfo((LinearLayout)rootView.findViewById(R.id.address_info),
                            getActivity());
                    break;
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        Button save_button = (Button) rootView.findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        return rootView;
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

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode); //Activity.RESULT_OK;
        if(Activity.RESULT_OK == resultCode) {

        }
    }

    private void submitForm() {
        transactionRequest.setPaymentMethod(Payment.getByPosition(
                payment_method_spinner.getSelectedItemPosition()));
        CooinAccountsInfo userInfo = null;
        BigDecimal availableForTagVS = null;
        try {
            userInfo = PrefUtils.getCooinAccountsInfo(getActivity());
            availableForTagVS = userInfo.getAvailableForTagVS(
                    transactionRequest.getCurrencyCode(), transactionRequest.getTagVS());
        } catch(Exception ex) { ex.printStackTrace();}
        switch (transactionRequest.getPaymentMethod()) {
            case SIGNED_TRANSACTION:
                try {
                    if(availableForTagVS.compareTo(transactionRequest.getAmount()) < 0) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                getString(R.string.insufficient_cash_caption),
                                getString(R.string.insufficient_cash_msg,
                                transactionRequest.getCurrencyCode(),
                                transactionRequest.getAmount().toString(),
                                availableForTagVS.toString()), getActivity());
                        builder.setPositiveButton(getString(R.string.check_available_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                                        intent.putExtra(ContextVS.REFRESH_KEY, true);
                                        intent.putExtra(ContextVS.FRAGMENT_KEY, CooinAccountsFragment.class.getName());
                                        startActivity(intent);
                                    }
                                });
                        UIUtils.showMessageDialog(builder);
                        return;
                    } else  PinDialogFragment.showPinScreen(getActivity().getSupportFragmentManager(),
                            broadCastId, transactionRequest.getConfirmMessage(getActivity()),
                            false, TypeVS.SIGNED_TRANSACTION);
                } catch(Exception ex) { ex.printStackTrace();}
                break;
            case ANONYMOUS_SIGNED_TRANSACTION:
                if(Wallet.getCooinList() == null) {
                    PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                            getString(R.string.enter_wallet_pin_msg), false,
                            TypeVS.ANONYMOUS_SIGNED_TRANSACTION);
                    return;
                }
                BigDecimal availableForTagVSWallet = Wallet.getAvailableForTagVS(
                        transactionRequest.getCurrencyCode(), transactionRequest.getTagVS());
                if(availableForTagVSWallet.compareTo(transactionRequest.getAmount()) < 0) {
                    BigDecimal amountToRequest = transactionRequest.getAmount().subtract(
                            availableForTagVSWallet);
                    if(availableForTagVS.compareTo(amountToRequest) < 0) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                getString(R.string.insufficient_cash_caption),
                                getString(R.string.insufficient_anonymous_money_msg,
                                        transactionRequest.getCurrencyCode(),
                                        availableForTagVSWallet.toString(),
                                        amountToRequest.toString(),
                                        availableForTagVS.toString()), getActivity());
                        builder.setPositiveButton(getString(R.string.check_available_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                                        intent.putExtra(ContextVS.REFRESH_KEY, true);
                                        intent.putExtra(ContextVS.FRAGMENT_KEY, CooinAccountsFragment.class.getName());
                                        startActivity(intent);
                                    }
                                });
                        UIUtils.showMessageDialog(builder);
                        return;
                    } else {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                getString(R.string.insufficient_cash_caption),
                                getString(R.string.insufficient_anonymous_cash_msg,
                                        transactionRequest.getCurrencyCode(),
                                        transactionRequest.getAmount().toString(),
                                        availableForTagVS.toString()), getActivity());
                        builder.setPositiveButton(getString(R.string.check_available_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent intent = new Intent(getActivity(), CashRequestFormActivity.class);
                                        startActivityForResult(intent, COOIN_REQUEST);
                                    }
                                });
                        UIUtils.showMessageDialog(builder);
                    }
                }
                break;
            case COOIN_SEND:
                break;
        }

    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.sending_payment_lbl),
                    getString(R.string.wait_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

}