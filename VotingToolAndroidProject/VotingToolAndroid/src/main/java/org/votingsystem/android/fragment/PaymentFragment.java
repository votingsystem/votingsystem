package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CashRequestFormActivity;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.service.TransactionVSService;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.CooinAccountsInfo;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.Payment;
import org.votingsystem.model.TransactionRequest;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.CALLER_KEY;
import static org.votingsystem.model.ContextVS.JSON_DATA_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.TYPEVS_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PaymentFragment extends Fragment {

	public static final String TAG = PaymentFragment.class.getSimpleName();

    private static final int COOIN_REQUEST   = 1;

    private String broadCastId = PaymentFragment.class.getSimpleName();
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
                        launchSignedTransaction();
                        break;
                    case ANONYMOUS_SIGNED_TRANSACTION:
                        launchAnonymousSignedTransaction();
                        break;
                    case COOIN:
                        try {
                            List<Cooin> cooinList = Wallet.getCooinList((String) responseVS.getData(),
                                    (AppContextVS) getActivity().getApplicationContext());
                            submitForm();
                        } catch(Exception ex) { ex.printStackTrace(); }
                        break;
                }

            } else {
                setProgressDialogVisible(false);
                String caption = ResponseVS.SC_OK == responseVS.getStatusCode()?getString(
                        R.string.payment_ok_caption):getString(R.string.error_lbl);
                MessageDialogFragment.showDialog(responseVS.getStatusCode(), caption,
                        responseVS.getMessage(), getFragmentManager());
            }
        }
    };

    private void launchSignedTransaction() {
        LOGD(TAG + ".launchSignedTransaction() ", "launchSignedTransaction");
        Intent startIntent = new Intent(getActivity(), TransactionVSService.class);
        try {
            startIntent.putExtra(JSON_DATA_KEY, transactionRequest.toJSON().toString());
            startIntent.putExtra(CALLER_KEY, broadCastId);
            startIntent.putExtra(TYPEVS_KEY, TypeVS.SIGNED_TRANSACTION);
            getActivity().startService(startIntent);
            setProgressDialogVisible(true);
        } catch (JSONException ex) { ex.printStackTrace(); }
    }

    private void launchAnonymousSignedTransaction() {
        LOGD(TAG + ".launchAnonymousSignedTransaction() ", "launchAnonymousSignedTransaction");
        Intent startIntent = new Intent(getActivity(), TransactionVSService.class);
        try {
            startIntent.putExtra(JSON_DATA_KEY, transactionRequest.toJSON().toString());
            startIntent.putExtra(CALLER_KEY, broadCastId);
            startIntent.putExtra(TYPEVS_KEY, TypeVS.ANONYMOUS_SIGNED_TRANSACTION);
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
            receptor.setText(transactionRequest.getToUserName());
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

    @Override public void onStart() {
        super.onStart();
        TransactionVS transactionVS = null;
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
            submitForm();
        }
    }

    private void submitForm() {
        try {
            transactionRequest.setPaymentMethod(Payment.getByPosition(
                    payment_method_spinner.getSelectedItemPosition()));
            CooinAccountsInfo userInfo = PrefUtils.getCooinAccountsInfo(getActivity());
            final BigDecimal availableForTagVS = userInfo.getAvailableForTagVS(
                    transactionRequest.getCurrencyCode(), transactionRequest.getTagVS());
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
                                            Intent intent = new Intent(getActivity(),
                                                    FragmentContainerActivity.class);
                                            intent.putExtra(ContextVS.REFRESH_KEY, true);
                                            intent.putExtra(ContextVS.FRAGMENT_KEY,
                                                    CooinAccountsFragment.class.getName());
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
                                TypeVS.COOIN);
                        return;
                    }
                    final BigDecimal availableForTagVSWallet = Wallet.getAvailableForTagVS(
                            transactionRequest.getCurrencyCode(), transactionRequest.getTagVS());
                    if(availableForTagVSWallet.compareTo(transactionRequest.getAmount()) < 0) {
                        final BigDecimal amountToRequest = transactionRequest.getAmount().subtract(
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
                                            Intent intent = new Intent(getActivity(),
                                                    FragmentContainerActivity.class);
                                            intent.putExtra(ContextVS.REFRESH_KEY, true);
                                            intent.putExtra(ContextVS.FRAGMENT_KEY,
                                                    CooinAccountsFragment.class.getName());
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
                                            availableForTagVSWallet.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.request_cash_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    CashRequestFormActivity.class);
                                            intent.putExtra(ContextVS.MAX_VALUE_KEY,
                                                    availableForTagVS);
                                            intent.putExtra(ContextVS.DEFAULT_VALUE_KEY,
                                                    amountToRequest);
                                            intent.putExtra(ContextVS.CURRENCY_KEY,
                                                    transactionRequest.getCurrencyCode());
                                            intent.putExtra(ContextVS.MESSAGE_KEY,
                                                    getString(R.string.cash_for_payment_dialog_msg,
                                                    transactionRequest.getCurrencyCode(),
                                                    amountToRequest.toString(),
                                                    availableForTagVS.toString()));
                                            startActivityForResult(intent, COOIN_REQUEST);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                        }
                    } else {
                        launchAnonymousSignedTransaction();
                    }
                    break;
                case CASH_SEND:
                    break;
            }
        } catch(Exception ex) { ex.printStackTrace();}
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.sending_payment_lbl),
                    getString(R.string.wait_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

}