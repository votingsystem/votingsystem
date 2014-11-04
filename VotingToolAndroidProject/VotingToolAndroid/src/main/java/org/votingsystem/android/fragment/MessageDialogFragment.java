package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageDialogFragment extends DialogFragment {

    public static final String TAG = MessageDialogFragment.class.getSimpleName();

    public static MessageDialogFragment newInstance(Integer statusCode, String caption,
                    String message){
        MessageDialogFragment frag = new MessageDialogFragment();
        Bundle args = new Bundle();
        if(statusCode != null) args.putInt(ContextVS.RESPONSE_STATUS_KEY, statusCode);
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, message);
        frag.setArguments(args);
        return frag;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_dialog, null);
        int statusCode = getArguments().getInt(ContextVS.RESPONSE_STATUS_KEY, -1);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
        String message = getArguments().getString(ContextVS.MESSAGE_KEY);
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        if(caption != null) builder.setTitle(caption);
        if(message != null) messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog dialog = builder.create();
        dialog.setView(view);
        if(statusCode > 0) {
            if(ResponseVS.SC_OK == statusCode) dialog.setIcon(R.drawable.fa_check_32);
            else dialog.setIcon(R.drawable.fa_times_32);
        }
        return dialog;
    }

}
