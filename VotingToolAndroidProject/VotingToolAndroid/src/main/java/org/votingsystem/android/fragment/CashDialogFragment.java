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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertRequestActivity;
import org.votingsystem.android.activity.CertResponseActivity;
import org.votingsystem.android.ui.HorizontalNumberPicker;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

import java.math.BigDecimal;

import static org.votingsystem.android.util.LogUtils.LOGD;

public class CashDialogFragment extends DialogFragment {

    public static final String TAG = CashDialogFragment.class.getSimpleName();

    public static final String MAX_VALUE_KEY = "MAX_VALUE_KEY";

    private TypeVS typeVS;
    private LinearLayout tag_info;
    private TextView tag_text;
    private TagVS tagVS;
    private TextView msgTextView;
    private Button add_tag_btn;
    private TextView errorMsgTextView;
    private String broadCastId = CashDialogFragment.class.getSimpleName();
    private HorizontalNumberPicker horizontal_number_picker;
    private CheckBox time_limited_checkbox;
    private String dialogCaller = null;
    private String currencyCode = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            setTagVS((TagVS) intent.getSerializableExtra(ContextVS.TAG_KEY));
        }
    };

    public static void showDialog(FragmentManager fragmentManager, String broadCastId,
             String caption, String message, BigDecimal maxValue, String currencyCode, TypeVS type){
        boolean isWithCertValidation = true;
        CashDialogFragment pinDialog = CashDialogFragment.newInstance(caption,
                message, maxValue, currencyCode, isWithCertValidation, broadCastId, type);
        pinDialog.show(fragmentManager, CashDialogFragment.TAG);
    }

    public static CashDialogFragment newInstance(String caption, String msg,
           BigDecimal maxValue, String currencyCode, boolean isWithCertValidation, String caller,
           TypeVS type) {
        CashDialogFragment dialog = new CashDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, msg);
        args.putString(ContextVS.CURRENCY_KEY, currencyCode);
        args.putSerializable(MAX_VALUE_KEY, maxValue);
        args.putString(ContextVS.CALLER_KEY, caller);
        args.putBoolean(ContextVS.CERT_VALIDATION_KEY, isWithCertValidation);
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        dialog.setArguments(args);
        return dialog;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCancelable(false);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateDialog", "savedInstanceState: " + savedInstanceState);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AppContextVS contextVS = (AppContextVS) getActivity().getApplicationContext();
        boolean isWithCertValidation = getArguments().getBoolean(ContextVS.CERT_VALIDATION_KEY);
        typeVS = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        BigDecimal maxValue = (BigDecimal) getArguments().getSerializable(MAX_VALUE_KEY);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
        currencyCode = getArguments().getString(ContextVS.CURRENCY_KEY);
        final ContextVS.State appState = contextVS.getState();
        if(!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState()) && isWithCertValidation) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.cert_not_found_caption),
                    getString(R.string.cert_not_found_msg), getActivity());
            builder.setPositiveButton(getString(R.string.request_certificate_menu),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = null;
                            switch(appState) {
                                case WITH_CSR:
                                    intent = new Intent(getActivity(), CertResponseActivity.class);
                                    break;
                                case WITHOUT_CSR:
                                    intent = new Intent(getActivity(), CertRequestActivity.class);
                                    break;
                            }
                            if(intent != null) startActivity(intent);
                        }
                    }).setNegativeButton(getString(R.string.cancel_lbl), null);
            Dialog dialog = builder.show();
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            return dialog;
        } else {
            View view = inflater.inflate(R.layout.cash_dialog_fragment, null);
            tag_text = (TextView) view.findViewById(R.id.tag_text);
            tag_info = (LinearLayout) view.findViewById(R.id.tag_info);
            ((TextView) view.findViewById(R.id.caption)).setText(caption);
            msgTextView = (TextView) view.findViewById(R.id.msg);
            horizontal_number_picker = (HorizontalNumberPicker)view.findViewById(R.id.horizontal_number_picker);
            time_limited_checkbox = (CheckBox) view.findViewById(R.id.time_limited_checkbox);
            errorMsgTextView = (TextView) view.findViewById(R.id.errorMsg);
            horizontal_number_picker.setMaxValue(maxValue, currencyCode);
       d     add_tag_btn = (Button) view.findViewById(R.id.add_tag_btn);
            add_tag_btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { addTagClicked(); }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if(savedInstanceState != null) {
                setTagVS((TagVS) savedInstanceState.getSerializable(ContextVS.TAG_KEY));
            }
            if(getArguments().getString(ContextVS.MESSAGE_KEY) == null) {
                msgTextView.setVisibility(View.GONE);
            } else {
                msgTextView.setVisibility(View.VISIBLE);
                msgTextView.setText(Html.fromHtml(getArguments().getString(ContextVS.MESSAGE_KEY)));
            }
            builder.setView(view);
            dialogCaller = getArguments().getString(ContextVS.CALLER_KEY);
            builder.setPositiveButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    sendCashValue();
                }
            });
            builder.setNegativeButton(getString(R.string.cancel_lbl), null);
            return builder.create();
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ContextVS.TAG_KEY, tagVS);
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

    private void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
        if(tagVS != null) {
            add_tag_btn.setText(getString(R.string.remove_tag_lbl));
            tag_text.setText(getString(R.string.selected_tag_lbl,tagVS.getName()));
            tag_info.setVisibility(View.VISIBLE);
        } else {
            add_tag_btn.setText(getString(R.string.add_tag_lbl));
            tag_info.setVisibility(View.GONE);
        }
    }

    public void addTagClicked() {
        if(tagVS == null) SelectTagVSDialogFragment.showDialog(broadCastId,
                getActivity().getSupportFragmentManager(), SelectTagVSDialogFragment.TAG);
        else setTagVS(null);
    }

    private void sendCashValue() {
        if(horizontal_number_picker.getValue().compareTo(new BigDecimal(0)) > 0) {
            if(dialogCaller != null) {
                Intent intent = new Intent(dialogCaller);
                ResponseVS responseVS = new ResponseVS(ResponseVS.SC_PROCESSING, typeVS);
                tagVS = (tagVS == null) ? new TagVS(TagVS.WILDTAG):tagVS;
                TransactionVS transactionVS = new TransactionVS(horizontal_number_picker.getValue(),
                        currencyCode, tagVS, time_limited_checkbox.isChecked());
                responseVS.setData(transactionVS);
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
            getDialog().dismiss();
        } else {
            errorMsgTextView.setVisibility(View.VISIBLE);
        }
    }

}