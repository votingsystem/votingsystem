package org.sistemavotacion.android.ui;

import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;

import java.io.File;
import java.io.FileOutputStream;

import org.sistemavotacion.android.R;
import org.sistemavotacion.android.service.SignService;
import org.sistemavotacion.modelo.VoteReceipt;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ReceiptOptionsDialog  extends DialogFragment {

	public static final String TAG = "ReceiptOptionsDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public ReceiptOptionsDialog() {
        // Empty constructor required for DialogFragment
    }
    
    private VoteReceipt receipt;
    private TextView msgTextView;
    private Button cancelVoteButton;
    private Button openReceiptButton;
	private SignService signService = null;
	private ReceiptOperationsListener listener = null;
	
    public static ReceiptOptionsDialog newInstance(String caption, 
    		String msg, VoteReceipt receipt, ReceiptOperationsListener listener) {
    	ReceiptOptionsDialog receiptOptionsDialog = new ReceiptOptionsDialog();
        Bundle args = new Bundle();
        receiptOptionsDialog.setVoteReceipt(receipt);
        receiptOptionsDialog.setListener(listener);
        args.putString("caption", caption);
        if(msg != null && msg.length() > MAX_MSG_LENGTH)
        	msg = msg.substring(0, MAX_MSG_LENGTH) + "...";
        args.putString("message", msg);
        receiptOptionsDialog.setArguments(args);
        return receiptOptionsDialog;
    }
    
    private void setVoteReceipt(VoteReceipt receipt) {
    	this.receipt = receipt;
    }
    
    private void setListener(ReceiptOperationsListener listener) {
    	this.listener = listener;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.receipt_options_dialog, container);
        msgTextView = (TextView) view.findViewById(R.id.msg);
        cancelVoteButton = (Button) view.findViewById(R.id.cancel_vote_button);
        cancelVoteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	cancelVoteButton.setEnabled(false);
            	listener.cancelVote(receipt);
            	getDialog().dismiss();
            }  
        });
        openReceiptButton = (Button) view.findViewById(R.id.open_receipt_button);
        openReceiptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	try {
            		/*File receiptFile =  new File(getActivity()
            				.getExternalFilesDir(null), "vote_receipt_" + 
            				receipt.getNotificationId() + SIGNED_PART_EXTENSION);*/
                    File receiptFile = File.createTempFile(
                    	"vote_recepit", SIGNED_PART_EXTENSION);
                    receipt.getSmimeMessage().writeTo(new FileOutputStream(receiptFile));
            		Log.d(TAG + ".onCreate(...) ", " - receiptFile path: " + receiptFile.getAbsolutePath() 
            				+ " - length: " + receiptFile.length() );
                	Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                	intent.setDataAndType(Uri.fromFile(receiptFile), "text/plain");
                	startActivity(intent);	
            	}catch(Exception ex) {
            		ex.printStackTrace();
            	}
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