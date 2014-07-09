package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DownloadReceiptDialogFragment extends DialogFragment {

    public static final String TAG = DownloadReceiptDialogFragment.class.getSimpleName();

    public static DownloadReceiptDialogFragment newInstance(Integer statusCode, String caption,
                    String message, String url, TypeVS typeVS){
        DownloadReceiptDialogFragment frag = new DownloadReceiptDialogFragment();
        Bundle args = new Bundle();
        if(statusCode != null) args.putInt(ContextVS.RESPONSE_STATUS_KEY, statusCode);
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, message);
        args.putString(ContextVS.URL_KEY, url);
        args.putSerializable(ContextVS.TYPEVS_KEY, typeVS);
        frag.setArguments(args);
        return frag;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.download_receipt_dialog, null);
        int statusCode = getArguments().getInt(ContextVS.RESPONSE_STATUS_KEY, -1);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
        String message = getArguments().getString(ContextVS.MESSAGE_KEY);
        final TypeVS typeVS = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        final String url = getArguments().getString(ContextVS.URL_KEY);
        Button downloadButton = (Button) view.findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.URL_KEY, url);
                intent.putExtra(ContextVS.TYPEVS_KEY, typeVS);
                intent.putExtra(ContextVS.FRAGMENT_KEY, ReceiptFragment.class.getName());
                startActivity(intent);
                dismiss();
            }
        });
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
        TextView messageTextView = (TextView)view.findViewById(R.id.message);
        if(caption != null) builder.setTitle(caption);
        if(message != null) messageTextView.setText(Html.fromHtml(message));
        messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog dialog = builder.create();
        dialog.setView(view);
        return dialog;
    }

}
