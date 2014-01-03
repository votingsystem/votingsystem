package org.votingsystem.android.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.VoteVS;

public class VotingResultDialog  extends DialogFragment {

	public static final String TAG = "VotingResultDialog";
	
	public static final int MAX_MSG_LENGTH = 400;
    
    private VoteVS receipt;

    public static VotingResultDialog newInstance(String caption, 
    		VoteVS receipt) {
    	VotingResultDialog votingResultDialog = new VotingResultDialog();
        Bundle args = new Bundle();
        votingResultDialog.setReceipt(receipt);
        args.putString(ContextVS.CAPTION_KEY, caption);
        votingResultDialog.setArguments(args);
        return votingResultDialog;
    }
    
    private void setReceipt(VoteVS receipt) {
    	this.receipt = receipt;
    }
    
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.voting_result_dialog, container);
        TextView msgTextView = (TextView) view.findViewById(R.id.msg);
        if(getArguments().getString(ContextVS.CAPTION_KEY) != null) {
        	getDialog().setTitle(getArguments().getString(ContextVS.CAPTION_KEY));
        }

        if(receipt != null) {
            String msg = receipt.getMessage(getActivity().getApplicationContext());
            if(msg != null && msg.length() > MAX_MSG_LENGTH)
                msg = msg.substring(0, MAX_MSG_LENGTH) + "...";
            msgTextView.setText(msg);
        }
        return view;
    }
}