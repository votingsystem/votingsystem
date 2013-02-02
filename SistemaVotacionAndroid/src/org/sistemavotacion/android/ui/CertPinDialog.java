/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sistemavotacion.android.ui;

import org.sistemavotacion.android.R;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class CertPinDialog  extends DialogFragment {

	public static final String TAG = "CertPinDialog";

    private CertPinDialogListener dialogListener;
	
    public CertPinDialog() {
        // Empty constructor required for DialogFragment
    }
    
    private TextView msgTextView;
    private EditText userPinEditText;
    private String firstPin = null;
    private Boolean withPasswordConfirm = null;

    public static CertPinDialog newInstance(String msg, 
    		CertPinDialogListener dialogListener, boolean withPasswordConfirm) {
    	CertPinDialog dialog = new CertPinDialog();
        Bundle args = new Bundle();
        args.putString("message", msg);
        dialog.setArguments(args);
        dialog.setListener(dialogListener);
        dialog.setWithPasswordConfirm(withPasswordConfirm);
        return dialog;
    }

    private void setListener(CertPinDialogListener dialogListener) {
    	this.dialogListener = dialogListener;
    }
    
    private void setWithPasswordConfirm(Boolean withPasswordConfirm) {
    	this.withPasswordConfirm = withPasswordConfirm;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cert_pin_dialog, container, false);
        msgTextView = (TextView) view.findViewById(R.id.msg);
        userPinEditText = (EditText)view.findViewById(R.id.user_pin);
        getDialog().setTitle(getString(R.string.pin_dialog_Caption));
        if(getArguments().getString("message") == null || 
        		"".equals(getArguments().getString("message"))) {
        	msgTextView.setVisibility(View.GONE);
        } else {
        	msgTextView.setVisibility(View.VISIBLE);
        	msgTextView.setText(getArguments().getString("message"));
        }
        getDialog().setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				//OnKey is fire twice: the first time for key down, and the second time for key up, 
				//so you have to filter:
				if (event.getAction()!=KeyEvent.ACTION_DOWN)
                    return true;
				Log.d(TAG + ".onKey(...) ", " - keyCode: " + keyCode);
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                	Log.d(TAG + ".onKey(...) ", " - KEYCODE_BACK KEYCODE_BACK ");
                    dialog.dismiss();
                }
                //if (keyCode == KeyEvent.KEYCODE_DEL) { } 
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                	Log.d(TAG + ".onKey(...) ", " - KEYCODE_ENTER");
                	String pin = userPinEditText.getText().toString();
                	if(pin != null && pin.length() ==4) {
                		setPin(pin);
                	} 
                }
                //True if the listener has consumed the event, false otherwise.
				return false;
			}

        });
        /*int width = getResources().getDimensionPixelSize(R.dimen.cert_pin_dialog_width);
        int height = getResources().getDimensionPixelSize(R.dimen.cert_pin_dialog_height);        
        getDialog().getWindow().setLayout(width, height);*/
        firstPin = null;
        return view;
    }
    
    private void setPin(String pin) {
    	if(withPasswordConfirm) {
    		if(firstPin == null) {
    			firstPin = pin;
    			msgTextView.setText(getActivity().getString(R.string.repeat_password));
    			userPinEditText.setText("");
    			return;
    		} else {
    			if (!firstPin.equals(pin)) {
    				firstPin = null;
        			userPinEditText.setText("");
    				msgTextView.setText(getActivity().getString(R.string.password_mismatch));
    				return;
    			} 
    		}
    	}
		this.dismiss();
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
  		      getActivity().INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(userPinEditText.getWindowToken(), 0);
		dialogListener.setPin(pin);
    }
}