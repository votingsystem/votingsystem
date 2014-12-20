package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageVSInputDialogFragment extends DialogFragment {

    public static final String TAG = MessageVSInputDialogFragment.class.getSimpleName();

    public static void showDialog(String caption, String broadCastId, TypeVS type,
            FragmentManager fragmentManager) {
        MessageVSInputDialogFragment newFragment = MessageVSInputDialogFragment.newInstance(caption,
                broadCastId, type);
        newFragment.show(fragmentManager, MessageVSInputDialogFragment.TAG);
    }

    public static MessageVSInputDialogFragment newInstance(String caption, String broadCastId, TypeVS type){
        MessageVSInputDialogFragment fragment = new MessageVSInputDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.CALLER_KEY, broadCastId);
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.messagevs_input_dialog, null);
        final EditText messageEditText = (EditText) view.findViewById(R.id.message);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
        final String broadCastId = getArguments().getString(ContextVS.CALLER_KEY);
        final TypeVS typeVS = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity()).setPositiveButton(
            getString(R.string.accept_lbl),  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(messageEditText.getText().toString().trim().isEmpty()) return;
                    Intent intent = new Intent(broadCastId);
                    ResponseVS responseVS = new ResponseVS(typeVS, messageEditText.getText());
                    intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
            });
        TextView captionTextView = (TextView) view.findViewById(R.id.caption_text);
        if(caption != null) captionTextView.setText(caption);
        else {
            captionTextView.setVisibility(View.GONE);
            view.findViewById(R.id.separator).setVisibility(View.GONE);
        }
        AlertDialog dialog = builder.create();
        dialog.setView(view);
        this.setCancelable(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    ((MessageVSInputDialogFragment) getFragmentManager().
                            findFragmentByTag(MessageVSInputDialogFragment.TAG)).dismiss();
                    return true;
                } else return false;
            }
        });
        return dialog;
    }

}
