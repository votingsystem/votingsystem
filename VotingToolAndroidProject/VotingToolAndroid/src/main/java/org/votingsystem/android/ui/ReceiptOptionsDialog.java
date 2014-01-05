package org.votingsystem.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.VoteVS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;

public class ReceiptOptionsDialog extends DialogFragment {

	public static final String TAG = "ReceiptOptionsDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public enum Operation {CANCEL_VOTE, REMOVE_RECEIPT}

    public ReceiptOptionsDialog() { }
    
    private VoteVS vote;
	
    public static ReceiptOptionsDialog newInstance(String caption, String message,
               Serializable receipt, String caller) {
    	ReceiptOptionsDialog receiptOptionsDialog = new ReceiptOptionsDialog();
        Bundle args = new Bundle();
        args.putString(ContextVS.CAPTION_KEY, caption);
        if(message != null && message.length() > MAX_MSG_LENGTH)
            message = message.substring(0, MAX_MSG_LENGTH) + "...";
        args.putString(ContextVS.MESSAGE_KEY, message);
        args.putString(ContextVS.CALLER_KEY, caller);
        args.putSerializable(ContextVS.RECEIPT_KEY, receipt);
        receiptOptionsDialog.setArguments(args);
        return receiptOptionsDialog;
    }
    
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.receipt_options_dialog, container);
        TextView msgTextView = (TextView) view.findViewById(R.id.msg);
        final Button cancelVoteButton = (Button) view.findViewById(R.id.cancel_vote_button);
        final String dialogCaller = getArguments().getString(ContextVS.CALLER_KEY);
        vote = (VoteVS)getArguments().getSerializable(ContextVS.RECEIPT_KEY);
        cancelVoteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	cancelVoteButton.setEnabled(false);
                Intent intent = new Intent(dialogCaller);
                intent.putExtra(ContextVS.OPERATION_KEY, Operation.CANCEL_VOTE);
                LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                        sendBroadcast(intent);
            }  
        });
        Button openReceiptButton = (Button) view.findViewById(R.id.open_receipt_button);
        openReceiptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	openReceipt();
            }  
        });
        if(vote != null && vote.getCancelVoteReceipt() != null) {
        	cancelVoteButton.setVisibility(View.GONE);
        	if(vote.getCancelVoteReceipt() != null) {
        		openReceiptButton.setText(getActivity().
            			getString(R.string.open_cancel_vote_receipt_lbl));
        	} else {
        		openReceiptButton.setVisibility(View.GONE);
        	}
        } 
        final Button removeReceiptButton = (Button) view.findViewById(R.id.remove_receipt_button);
        removeReceiptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	removeReceiptButton.setEnabled(false);
				AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
	    		builder.setTitle(getString(R.string.remove_receipt_lbl));
	    		builder.setMessage(getString(R.string.remove_receipt_msg));
	    		builder.setPositiveButton(getString(
	    				R.string.ok_button), new DialogInterface.OnClickListener() {
	    		            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(dialogCaller);
                                intent.putExtra(ContextVS.OPERATION_KEY, Operation.REMOVE_RECEIPT);
                                LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                                        sendBroadcast(intent);
	    		            	dismiss();
	    		            }
	    					});
	    		builder.setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
		            		public void onClick(DialogInterface dialog, int whichButton) {
		            			removeReceiptButton.setEnabled(true);
		            		}
						});
	    		builder.show();
            }
        });
        if(getArguments().getString(ContextVS.CAPTION_KEY) != null) {
        	getDialog().setTitle(getArguments().getString(ContextVS.CAPTION_KEY));
        }
        if(getArguments().getString(ContextVS.MESSAGE_KEY) != null) {
        	msgTextView.setText(getArguments().getString(ContextVS.MESSAGE_KEY));
        }
        return view;
    }
    
    private void openReceipt() {
       	try {
    		/*File receiptFile =  new File(getActivity()
    				.getExternalFilesDir(null), "vote_receipt_" + 
    				receipt.getNotificationId() + ContentTypeVS.SIGNED.getExtension());*/
    		String fileName = "receipt_" + vote.getId() + ContentTypeVS.SIGNED.getExtension();
       		File receiptFile = getTemporaryFile(getActivity().getApplicationContext(), fileName);
       		
    		if(vote.getCancelVoteReceipt() != null) {
                vote.getCancelVoteReceipt().writeTo(new FileOutputStream(receiptFile));
    		} else vote.getVoteReceipt().writeTo(new FileOutputStream(receiptFile));
    		Log.d(TAG + ".openReceipt - ", " - receiptFile path: " + receiptFile.getAbsolutePath() 
    				+ " - length: " + receiptFile.length() );
        	Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        	intent.setDataAndType(Uri.fromFile(receiptFile), ContentTypeVS.TEXT.getName());
        	startActivity(intent);	
    	}catch(Exception ex) {
    		ex.printStackTrace();
    	}
    }
    
    private File getTemporaryFile(Context context, String fileName){
  	  final File path = new File( Environment.getExternalStorageDirectory(), context.getPackageName() );
  	  if(!path.exists()){
  	    path.mkdir();
  	  }
  	  return new File(path, fileName);
	}
    
}