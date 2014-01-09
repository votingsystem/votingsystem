package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class NewFieldDialogFragment extends DialogFragment {

    public static final String TAG = "NewFieldDialogFragment";


    private LinearLayout errorPanel;
    private EditText fieldEditText;
    private String dialogCaller;
    private TypeVS typeVS;
    private String caption;
    private String message;
    private AlertDialog alertDialog;

    public static NewFieldDialogFragment newInstance(String caption,  String message,
             String dialogCaller, TypeVS typeVS){
        NewFieldDialogFragment frag = new NewFieldDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, message);
        args.putString(ContextVS.CALLER_KEY, dialogCaller);
        args.putSerializable(ContextVS.TYPEVS_KEY, typeVS);
        frag.setArguments(args);
        return frag;
    }


    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.new_field_dialog_fragment, null);
        caption = getArguments().getString(ContextVS.CAPTION_KEY);
        message = getArguments().getString(ContextVS.MESSAGE_KEY);
        String htmlMessage = getArguments().getString(ContextVS.HTML_MESSAGE_KEY);
        dialogCaller = getArguments().getString(ContextVS.CALLER_KEY);
        typeVS = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        fieldEditText = (EditText)view.findViewById(R.id.field_content);
        errorPanel = (LinearLayout) view.findViewById(R.id.error_panel);
        if(savedInstanceState != null) {
            errorPanel.setVisibility(savedInstanceState.getInt(
                    ContextVS.ERROR_PANEL_KEY, View.GONE));
        }
        if(caption != null) builder.setTitle(caption);
        if(htmlMessage != null) messageTextView.setText(Html.fromHtml(htmlMessage));
        else if(message != null) messageTextView.setText(message);
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setPositiveButton(getString(R.string.aceptar_button), null)
                .setNegativeButton(R.string.cancel_button, null);
        alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialog) {
            Button okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    submitForm();
                }
            });
            }
        });

        alertDialog.setView(view);
        return alertDialog;
    }

    private void submitForm() {
        if(TextUtils.isEmpty(fieldEditText.getText())) {
            errorPanel.setVisibility(View.VISIBLE);
        } else {
            Intent intent = new Intent(dialogCaller);
            intent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
            if(caption != null) intent.putExtra(ContextVS.CAPTION_KEY, caption);
            if(message != null) intent.putExtra(ContextVS.MESSAGE_KEY,
                    fieldEditText.getText().toString());
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                    sendBroadcast(intent);
            alertDialog.dismiss();
        }
    }


    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ContextVS.ERROR_PANEL_KEY, errorPanel.getVisibility());
        Log.d(TAG + ".onSaveInstanceState(...)", "outState: " + outState);
    }

}