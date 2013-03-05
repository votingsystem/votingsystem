package org.sistemavotacion.android.ui;

import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;

import java.io.File;
import java.io.FileOutputStream;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.UserCertRequestForm;
import org.sistemavotacion.android.UserCertResponseForm;
import org.sistemavotacion.android.service.SignService;
import org.sistemavotacion.modelo.VoteReceipt;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class CertNotFoundDialog  extends DialogFragment {

	public static final String TAG = "ReceiptOptionsDialog";
	
	public static final int MAX_MSG_LENGTH = 400;

    public CertNotFoundDialog() {
        // Empty constructor required for DialogFragment
    }

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cert_not_found_dialog, container);
        TextView msgTextView = (TextView) view.findViewById(R.id.msg);
        /*Button cancelVoteButton = (Button) view.findViewById(R.id.cancel_button);
        cancelVoteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	onDestroyView();
            }  
        });*/
        Button openReceiptButton = (Button) view.findViewById(R.id.get_cert_button);
        openReceiptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	Intent intent = null;
          	  	switch(Aplicacion.INSTANCIA.getEstado()) {
          	  		case CON_CSR:
          	  			intent = new Intent(getActivity(), UserCertResponseForm.class);
          	  			break;
          	  		case SIN_CSR:
          	  			intent = new Intent(getActivity(), UserCertRequestForm.class);
          	  			break;
          	  	}
          	  	if(intent != null) startActivity(intent);
            }  
        });
        getDialog().setTitle(getString(R.string.cert_not_found_caption));
        msgTextView.setText(Html.fromHtml(getString(
				R.string.certificado_no_encontrado_msg)));
        return view;
    }
    
}