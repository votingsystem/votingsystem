package org.sistemavotacion.android.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.mail.MessagingException;

import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.mail.smime.SMIMEException;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.UserCertRequestForm;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.text.Html;
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
    
    private static VoteReceipt receipt;
	private static ReceiptOperationsListener listener = null;
	
    public static ReceiptOptionsDialog newInstance(String caption, 
    		String msg, VoteReceipt voteReceipt, ReceiptOperationsListener operationsListener) {
    	ReceiptOptionsDialog receiptOptionsDialog = new ReceiptOptionsDialog();
        Bundle args = new Bundle();
        receipt = voteReceipt;
        listener = operationsListener;
        args.putString("caption", caption);
        if(msg != null && msg.length() > MAX_MSG_LENGTH)
        	msg = msg.substring(0, MAX_MSG_LENGTH) + "...";
        args.putString("message", msg);
        receiptOptionsDialog.setArguments(args);
        return receiptOptionsDialog;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.receipt_options_dialog, container);
        TextView msgTextView = (TextView) view.findViewById(R.id.msg);
        final Button cancelVoteButton = (Button) view.findViewById(R.id.cancel_vote_button);
        cancelVoteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	cancelVoteButton.setEnabled(false);
            	listener.cancelVote(receipt);
            }  
        });
        Button openReceiptButton = (Button) view.findViewById(R.id.open_receipt_button);
        openReceiptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	openReceipt();
            }  
        });
        if(receipt != null && receipt.isCanceled()) {
        	cancelVoteButton.setVisibility(View.GONE);
        	if(receipt.getCancelVoteReceipt() != null) {
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
	    		            	listener.removeReceipt(receipt);
	    		            	getDialog().dismiss();
	    		            }
	    					});
	    		builder.setNegativeButton(getString(
	    				R.string.cancelar_button), null);
	    		builder.show();
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
    
    private void openReceipt() {
       	try {
    		/*File receiptFile =  new File(getActivity()
    				.getExternalFilesDir(null), "vote_receipt_" + 
    				receipt.getNotificationId() + SIGNED_PART_EXTENSION);*/
    		String fileName = "receipt_" + receipt.getId() + Aplicacion.SIGNED_PART_EXTENSION;
       		File receiptFile = getTemporaryFile(getActivity(), fileName);
       		
    		if(receipt.getCancelVoteReceipt() != null) {
    			receipt.getCancelVoteReceipt().writeTo(
        				new FileOutputStream(receiptFile));
    		} else receipt.getSmimeMessage().writeTo(
    				new FileOutputStream(receiptFile));
    		Log.d(TAG + ".openReceipt - ", " - receiptFile path: " + receiptFile.getAbsolutePath() 
    				+ " - length: " + receiptFile.length() );
        	Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        	intent.setDataAndType(Uri.fromFile(receiptFile), "text/plain");
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