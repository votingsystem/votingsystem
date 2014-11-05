/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertRequestActivity;
import org.votingsystem.android.activity.CertResponseActivity;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.util.ResponseVS;

public class TransactionVSDialogFragment extends DialogFragment implements OnKeyListener {

    public static final String TAG = TransactionVSDialogFragment.class.getSimpleName();

    private TextView msgTextView;
    private EditText userPinEditText;
    private Boolean withPasswordConfirm = null;
    private String dialogCaller = null;
    private String firstPin = null;
    private CheckBox anonymousTransactionCheckBox;
    private AlertDialog mainDialog;


    public static void showPinScreen(FragmentManager fragmentManager, String broadCastId,
             String message, boolean isWithPasswordConfirm, TypeVS type) {
        boolean isWithCertValidation = true;
        TransactionVSDialogFragment pinDialog = TransactionVSDialogFragment.newInstance(
                message, isWithPasswordConfirm, isWithCertValidation, broadCastId, type);
        pinDialog.show(fragmentManager, TransactionVSDialogFragment.TAG);
    }

    public static TransactionVSDialogFragment newInstance(String msg, boolean isWithPasswordConfirm,
            boolean isWithCertValidation, String caller, TypeVS type) {
        TransactionVSDialogFragment dialog = new TransactionVSDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.MESSAGE_KEY, msg);
        args.putString(ContextVS.CALLER_KEY, caller);
        args.putBoolean(ContextVS.PASSWORD_CONFIRM_KEY, isWithPasswordConfirm);
        args.putBoolean(ContextVS.CERT_VALIDATION_KEY, isWithCertValidation);
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        dialog.setArguments(args);
        return dialog;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        if(savedInstanceState != null) firstPin = savedInstanceState.getString(ContextVS.PIN_KEY);
        this.setCancelable(false);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateDialog(...) ", "savedInstanceState: " + savedInstanceState);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AppContextVS contextVS = (AppContextVS) getActivity().getApplicationContext();
        boolean isWithCertValidation = getArguments().getBoolean(ContextVS.CERT_VALIDATION_KEY);
        final ContextVS.State appState = contextVS.getState();
        if(!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState()) && isWithCertValidation) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(
                    getString(R.string.cert_not_found_caption)).setMessage(
                    Html.fromHtml(getString(R.string.cert_not_found_msg))).setPositiveButton(
                    R.string.request_certificate_menu, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = null;
                    switch(appState) {
                        case WITH_CSR:
                            intent = new Intent(getActivity().getApplicationContext(),
                                    CertResponseActivity.class);
                            break;
                        case WITHOUT_CSR:
                            intent = new Intent(getActivity().getApplicationContext(),
                                    CertRequestActivity.class);
                            break;
                    }
                    if(intent != null) startActivity(intent);
                }
            }).setNegativeButton(R.string.cancel_lbl, null);
            return builder.create();
        } else {
            View view = inflater.inflate(R.layout.transactionvs_dialog, null);
            msgTextView = (TextView) view.findViewById(R.id.msg);
            userPinEditText = (EditText)view.findViewById(R.id.user_pin);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(
                    getString(R.string.pin_dialog_caption));
            if(getArguments().getString(ContextVS.MESSAGE_KEY) == null) {
                msgTextView.setVisibility(View.GONE);
            } else {
                msgTextView.setVisibility(View.VISIBLE);
                msgTextView.setText(Html.fromHtml(getArguments().getString(ContextVS.MESSAGE_KEY)));
            }
            withPasswordConfirm = getArguments().getBoolean(ContextVS.PASSWORD_CONFIRM_KEY);
            dialogCaller = getArguments().getString(ContextVS.CALLER_KEY);
            builder.setView(view).setOnKeyListener(this);
            mainDialog = builder.create();

            anonymousTransactionCheckBox = (CheckBox) view.findViewById(R.id.anonymous_transaction_checkbox);
            anonymousTransactionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(anonymousTransactionCheckBox.isChecked()) {
                        mainDialog.setTitle(getString(R.string.anonymous_transactionvs_lbl).toUpperCase() +
                                " - " + getString(R.string.pin_dialog_caption));
                    } else {
                        mainDialog.setTitle(getString(R.string.pin_dialog_caption));
                    }
                }
            });

            return mainDialog;
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.PIN_KEY, firstPin);
    }

    private void setPin(final String pin) {
        if(withPasswordConfirm) {
            if(firstPin == null) {
                firstPin = pin;
                msgTextView.setText(getString(R.string.repeat_password));
                userPinEditText.setText("");
                return;
            } else {
                if (!firstPin.equals(pin)) {
                    firstPin = null;
                    userPinEditText.setText("");
                    msgTextView.setText(getString(R.string.password_mismatch));
                    return;
                }
            }
        }

        AppContextVS contextVS = (AppContextVS) getActivity().getApplicationContext();
        try {
            String storedPasswordHash =  PrefUtils.getStoredPasswordHash(contextVS);
            if(storedPasswordHash != null) {
                String passwordHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
                if(!passwordHash.equals(storedPasswordHash)) {
                    msgTextView.setVisibility(View.VISIBLE);
                    msgTextView.setText(getString(R.string.pin_error_msg));
                    return;
                }
            }
            InputMethodManager imm = (InputMethodManager)getActivity().
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), 0);
            if(dialogCaller != null) {
                Intent intent = new Intent(dialogCaller);
                intent.putExtra(ContextVS.PIN_KEY, pin);
                TypeVS typeVS = anonymousTransactionCheckBox.isChecked()? TypeVS.VICKET_SEND:TypeVS.TRANSACTIONVS;
                intent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
                ResponseVS responseVS = new ResponseVS(typeVS, pin);
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
            firstPin = null;
            getDialog().dismiss();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        //OnKey is fire twice: the first time for key down, and the second time for key up,
        //so you have to filter:
        if (event.getAction()!=KeyEvent.ACTION_DOWN) return true;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG + ".onKey(...) ", "KEYCODE_BACK KEYCODE_BACK ");
            dialog.dismiss();
        }
        //if (keyCode == KeyEvent.KEYCODE_DEL) { } 
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            Log.d(TAG + ".onKey(...) ", "KEYCODE_ENTER");
            String pin = userPinEditText.getText().toString();
            if(pin != null && pin.length() == 4) {
                setPin(pin);
            }
        }
        //True if the listener has consumed the event, false otherwise.
        return false;
    }

}