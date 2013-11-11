package org.votingsystem.android.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.model.VoteReceipt;

public class VotingResultDialog  extends DialogFragment {

	public static final String TAG = "VotingResultDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public VotingResultDialog() {
        // Empty constructor required for DialogFragment
    }
    
    private VoteReceipt reciboVoto;

    public static VotingResultDialog newInstance(String caption, 
    		VoteReceipt reciboVoto) {
    	VotingResultDialog votingResultDialog = new VotingResultDialog();
        Bundle args = new Bundle();
        votingResultDialog.setReciboVoto(reciboVoto);
        args.putString("caption", caption);
        votingResultDialog.setArguments(args);
        return votingResultDialog;
    }
    
    private void setReciboVoto(VoteReceipt reciboVoto) {
    	this.reciboVoto = reciboVoto;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.voting_result_dialog, container);
        TextView msgTextView = (TextView) view.findViewById(R.id.msg);

        if(getArguments().getString("caption") != null) {
        	getDialog().setTitle(getArguments().getString("caption"));
        }

        if(reciboVoto != null) {
            String msg = reciboVoto.getMensaje(getActivity().getBaseContext());
            if(msg != null && msg.length() > MAX_MSG_LENGTH)
                msg = msg.substring(0, MAX_MSG_LENGTH) + "...";
            msgTextView.setText(msg);
        }
        return view;
    }
}