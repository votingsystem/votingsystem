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

package org.votingsystem.android.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import org.votingsystem.android.R;

public class CertPinDialog extends DialogFragment implements OnKeyListener {

	public static final String TAG = "CertPinDialog";
	
    public CertPinDialog() {
        // Empty constructor required for DialogFragment
    }
    
    private TextView msgTextView;
    private EditText userPinEditText;
    //orientation changes
    private static CertPinDialogListener listener;
    private static String firstPin = null;
    private static Boolean withPasswordConfirm = null;

    public static CertPinDialog newInstance(String msg, 
    		CertPinDialogListener dialogListener, boolean isWithPasswordConfirm) {
    	CertPinDialog dialog = new CertPinDialog();
        Bundle args = new Bundle();
        args.putString("message", msg);
        dialog.setArguments(args);
        listener = dialogListener;
        withPasswordConfirm = isWithPasswordConfirm;
        return dialog;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	Log.d(TAG + ".onCreateView(...) ", " - onCreateView -");
        View view = inflater.inflate(R.layout.cert_pin_dialog, container, false);
        msgTextView = (TextView) view.findViewById(R.id.msg);
        userPinEditText = (EditText)view.findViewById(R.id.user_pin);;
        getDialog().setTitle(getString(R.string.pin_dialog_Caption));
        if(getArguments().getString("message") == null || 
        		"".equals(getArguments().getString("message"))) {
        	msgTextView.setVisibility(View.GONE);
        } else {
        	msgTextView.setVisibility(View.VISIBLE);
        	msgTextView.setText(getArguments().getString("message"));
        }
        getDialog().setOnKeyListener(this);
        setRetainInstance(true);
        /*int width = getResources().getDimensionPixelSize(R.dimen.cert_pin_dialog_width);
        int height = getResources().getDimensionPixelSize(R.dimen.cert_pin_dialog_height);        
        getDialog().getWindow().setLayout(width, height);*/
        firstPin = null;
        return view;
    }
  
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	//super.onConfigurationChanged(newConfig);
    	Log.d(TAG + ".onConfigurationChanged(...) ", " - onConfigurationChanged");
    }
    
    private void setPin(final String pin) {
    	if(withPasswordConfirm) {
    		if(firstPin == null) {
    			firstPin = pin;
    			msgTextView.setText(getString(R.string.repeat_password));
    			userPinEditText.setText("");
    			return;
    		} else {
    			if (!firstPin.equals(pin)) {
    				firstPin = null;
        			userPinEditText.setText("");
    				msgTextView.setText(getString(R.string.password_mismatch));
    				return;
    			} 
    		}
    	}
        InputMethodManager imm = (InputMethodManager)getActivity().
        		getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getDialog().
        		getCurrentFocus().getWindowToken(), 0);
        //onDestroyView();
        getDialog().dismiss();
        listener.setPin(pin);
    }


	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		//OnKey is fire twice: the first time for key down, and the second time for key up, 
		//so you have to filter:
		if (event.getAction()!=KeyEvent.ACTION_DOWN)
            return true;
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
}