package org.votingsystem.android.ui;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.ObjectUtils;

public class CancelVoteDialog  extends DialogFragment {

	public static final String TAG = "CancelVoteDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public CancelVoteDialog() { }

    private TextView msgTextView;
    private Button saveCancellationButton;

    public static CancelVoteDialog newInstance(String caption,  String msg, VoteVS vote) {
    	CancelVoteDialog cancelVoteDialog = new CancelVoteDialog();
        Bundle args = new Bundle();
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, msg);
        args.putSerializable(ContextVS.VOTE_KEY, vote);
        cancelVoteDialog.setArguments(args);
        return cancelVoteDialog;
    }
    
	public void saveCancelReceipt(View v, VoteVS vote) {
		Log.d(TAG + ".saveCancelReceipt(...)", "saveCancelReceipt");
        ContentValues values = new ContentValues();
        try {
            values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL, ObjectUtils.serializeObject(vote));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        values.put(ReceiptContentProvider.TYPE_COL, TypeVS.CANCEL_VOTE.toString());
        values.put(ReceiptContentProvider.STATE_COL, ReceiptContainer.State.ACTIVE.toString());
        values.put(ReceiptContentProvider.TIMESTAMP_CREATED_COL, System.currentTimeMillis());
        values.put(ReceiptContentProvider.TIMESTAMP_UPDATED_COL, System.currentTimeMillis());
        getActivity().getContentResolver().insert(ReceiptContentProvider.CONTENT_URI, values);
        saveCancellationButton.setEnabled(false);
        msgTextView.setText(getString(R.string.saved_cancel_vote_recepit_msg));
	}
    
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cancel_vote_dialog, container);
        final VoteVS vote = (VoteVS) getArguments().getSerializable(ContextVS.VOTE_KEY);
        msgTextView = (TextView) view.findViewById(R.id.msg);
        saveCancellationButton = (Button) view.findViewById(R.id.save_cancel_receipt_button);
        saveCancellationButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	saveCancelReceipt(v, vote);
            }  
        });
        if(vote.getId() > 0) saveCancellationButton.setVisibility(View.GONE);
        getDialog().setTitle(getArguments().getString(ContextVS.CAPTION_KEY));
        String message = getArguments().getString(ContextVS.MESSAGE_KEY);
        if(message != null && message.length() > MAX_MSG_LENGTH)
            message = message.substring(0, MAX_MSG_LENGTH) + "...";
        msgTextView.setText(message);
        return view;
    }

}