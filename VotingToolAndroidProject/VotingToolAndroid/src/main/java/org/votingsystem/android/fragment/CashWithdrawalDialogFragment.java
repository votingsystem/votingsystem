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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertRequestActivity;
import org.votingsystem.android.activity.UserCertResponseActivity;
import org.votingsystem.android.ui.HorizontalNumberPicker;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.math.BigDecimal;

public class CashWithdrawalDialogFragment extends DialogFragment {

    public static final String TAG = "CashWithdrawalDialogFragment";

    public static final String MAX_VALUE_KEY = "MAX_VALUE_KEY";

    private TypeVS typeVS;
    private TextView msgTextView;
    private TextView errorMsgTextView;
    private HorizontalNumberPicker horizontal_number_picker;
    private String dialogCaller = null;


    public static void showDialog(FragmentManager fragmentManager, String broadCastId,
             String caption, String message, BigDecimal maxValue,TypeVS type) {
        boolean isWithCertValidation = true;
        CashWithdrawalDialogFragment pinDialog = CashWithdrawalDialogFragment.newInstance(caption,
                message, maxValue, isWithCertValidation, broadCastId, type);
        pinDialog.show(fragmentManager, CashWithdrawalDialogFragment.TAG);

    }


    public static CashWithdrawalDialogFragment newInstance(String caption, String msg,
           BigDecimal maxValue, boolean isWithCertValidation, String caller, TypeVS type) {
        CashWithdrawalDialogFragment dialog = new CashWithdrawalDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, msg);
        args.putSerializable(MAX_VALUE_KEY, maxValue);
        args.putString(ContextVS.CALLER_KEY, caller);
        args.putBoolean(ContextVS.CERT_VALIDATION_KEY, isWithCertValidation);
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        dialog.setArguments(args);
        return dialog;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        this.setCancelable(false);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateDialog(...) ", "savedInstanceState: " + savedInstanceState);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AppContextVS contextVS = (AppContextVS) getActivity().getApplicationContext();
        boolean isWithCertValidation = getArguments().getBoolean(ContextVS.CERT_VALIDATION_KEY);
        typeVS = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        BigDecimal maxValue = (BigDecimal) getArguments().getSerializable(MAX_VALUE_KEY);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
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
                                    UserCertResponseActivity.class);
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
            View view = inflater.inflate(R.layout.cash_withdrawal_dialog_fragment, null);
            msgTextView = (TextView) view.findViewById(R.id.msg);
            horizontal_number_picker = (HorizontalNumberPicker)view.findViewById(R.id.horizontal_number_picker);
            errorMsgTextView = (TextView) view.findViewById(R.id.errorMsg);
            horizontal_number_picker.setMaxValue(maxValue);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(caption);
            if(getArguments().getString(ContextVS.MESSAGE_KEY) == null) {
                msgTextView.setVisibility(View.GONE);
            } else {
                msgTextView.setVisibility(View.VISIBLE);
                msgTextView.setText(Html.fromHtml(getArguments().getString(ContextVS.MESSAGE_KEY)));
            }
            builder.setView(view);
            dialogCaller = getArguments().getString(ContextVS.CALLER_KEY);
            builder.setPositiveButton(getString(R.string.ok_lbl), null);
            builder.setNegativeButton(getString(R.string.cancel_lbl), null);
            return builder.create();
        }
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart()", "onStart");
        super.onStart();
        Button positiveButton = ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View onClick) {
                sendCashValue();
            }
        });
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void sendCashValue() {
        if(horizontal_number_picker.getValue().compareTo(new BigDecimal(0)) > 0) {
            if(dialogCaller != null) {
                Intent intent = new Intent(dialogCaller);
                ResponseVS responseVS = new ResponseVS(TypeVS.TICKET_REQUEST_DIALOG,
                        horizontal_number_picker.getValue());
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
            getDialog().dismiss();
        } else {
            errorMsgTextView.setVisibility(View.VISIBLE);
        }
    }

}