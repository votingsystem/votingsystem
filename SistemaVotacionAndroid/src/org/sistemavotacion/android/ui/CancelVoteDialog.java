package org.sistemavotacion.android.ui;

import org.sistemavotacion.android.R;
import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.modelo.VoteReceipt;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class CancelVoteDialog  extends DialogFragment {

	public static final String TAG = "CancelVoteDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public CancelVoteDialog() {
        // Empty constructor required for DialogFragment
    }
    
    private VoteReceipt receipt;
    private TextView msgTextView;
    private Button cancelVoteButton;

    public static CancelVoteDialog newInstance(String caption, 
    		String msg, VoteReceipt receipt) {
    	CancelVoteDialog cancelVoteDialog = new CancelVoteDialog();
        Bundle args = new Bundle();
        cancelVoteDialog.setVoteReceipt(receipt);
        args.putString("caption", caption);
        if(msg != null && msg.length() > MAX_MSG_LENGTH)
        	msg = msg.substring(0, MAX_MSG_LENGTH) + "...";
        args.putString("message", msg);
        cancelVoteDialog.setArguments(args);
        return cancelVoteDialog;
    }
    
    private void setVoteReceipt(VoteReceipt receipt) {
    	this.receipt = receipt;
    }
    
	public void saveCancelReceipt(View v) {
		Log.d(TAG + ".saveCancelReceipt(...)", " - saveVote");
    	VoteReceiptDBHelper db = new VoteReceiptDBHelper(getActivity());
		try {
			db.updateVoteReceipt(receipt);
			cancelVoteButton.setEnabled(false);
			msgTextView.setText(getString(R.string.saved_cancel_vote_recepit_msg));
		} catch (Exception ex) {
			msgTextView.setText(ex.getMessage());
			Log.e(TAG + ".saveCancelReceipt(...) ", ex.getMessage(), ex);
		}
	}
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cancel_vote_dialog, container);
        msgTextView = (TextView) view.findViewById(R.id.msg);
        cancelVoteButton = (Button) view.findViewById(R.id.save_cancel_receipt_button);
        cancelVoteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	saveCancelReceipt(v);
            }  
        });
         
        if(getArguments().getString("caption") != null) {
        	getDialog().setTitle(getArguments().getString("caption"));
        }
        if(getArguments().getString("message") != null) {
        	msgTextView.setText(getArguments().getString("message"));
        }
        return view;
    }
}