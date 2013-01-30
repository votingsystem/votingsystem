package org.sistemavotacion.android.ui;

import java.io.File;

import org.sistemavotacion.android.FragmentTabsPager;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.util.StringUtils;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class VotingResultDialog  extends DialogFragment {

	public static final String TAG = "VotingResultDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public VotingResultDialog() {
        // Empty constructor required for DialogFragment
    }
    
    VoteReceipt reciboVoto;

    public static VotingResultDialog newInstance(String caption, 
    		VoteReceipt reciboVoto) {
    	VotingResultDialog votingResultDialog = new VotingResultDialog();
        Bundle args = new Bundle();
        votingResultDialog.setReciboVoto(reciboVoto);
        args.putString("caption", caption);
        String msg = reciboVoto.getMensaje();
        if(msg != null && msg.length() > MAX_MSG_LENGTH)
        	msg = msg.substring(0, MAX_MSG_LENGTH) + "...";
        args.putString("message", msg);
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
        Button anularVotoButton = (Button) view.findViewById(R.id.anular_voto_button);
        anularVotoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {  
            	/*Intent intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
            	startActivity(intent);*/
            }
        });  
        
        
        
        Button guardarReciboButton = (Button) view.findViewById(R.id.guardar_recibo_button);
        guardarReciboButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { 
    			Log.d(TAG + ".guardarReciboButton ", " - Files dir path: " + 
    					getActivity().getApplicationContext().getFilesDir().getAbsolutePath());
    			String receiptFileName = StringUtils.getCadenaNormalizada(reciboVoto.getEventoURL()) ;
    					
            	File file = new File(getActivity().getApplicationContext().getFilesDir(), receiptFileName);
    			Log.d(TAG + ".guardarReciboButton ", " - Files path: " + file.getAbsolutePath());
    			try {
    				reciboVoto.writoToFile(file);
    				AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
    				builder.setTitle(getActivity().getString(R.string.operacion_ok_msg)).
    					setMessage(getActivity().getString(R.string.receipt_file_saved_msg)).show();
    			} catch(Exception ex) {
    				AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
    				builder.setTitle(getActivity().getString(R.string.error_lbl)).
    					setMessage(ex.getMessage()).show();
    			}
    			
            	/*Intent intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
            	startActivity(intent);*/
            }
        });  
        if(getArguments().getString("caption") != null) {
        	getDialog().setTitle(getArguments().getString("caption"));
        }
        if(getArguments().getString("message") != null) {
        	msgTextView.setText(getArguments().getString("message"));
        }
        if(reciboVoto == null) {
        	guardarReciboButton.setVisibility(View.GONE);
        }
        return view;
    }
}