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

package org.votingsystem.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ContextVSImpl;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.util.NifUtils;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.Date;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.*;

public class UserCertRequestActivity extends ActionBarActivity implements CertPinDialogListener {

	public static final String TAG = "UserCertRequestActivity";

    private String password = null;
    private String email = null;
    private String phone = null;
    private String deviceId = null;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private EditText nifText;
    private EditText givennameText;
    private EditText surnameText;
    private ContextVSImpl contextVS;

    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;
    private SendDataTask sendDataTask;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate - ");
        setContentView(R.layout.user_cert_request_activity);
        contextVS = ContextVSImpl.getInstance(getBaseContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.request_certificate_form_lbl));
        
        Button cancelarButton = (Button) findViewById(R.id.cancelar_button);
        cancelarButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { 
            	//finish(); 
            	Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
            	startActivity(intent);
            }
        });

        givennameText = (EditText)findViewById(R.id.given_name_edit);
        surnameText = (EditText)findViewById(R.id.surname_edit);


        nifText = (EditText)findViewById(R.id.nif_edit);
        nifText.setOnEditorActionListener(new OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
		            InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		            return true;
		        }
		        return false;
			}});

        nifText.setOnKeyListener(new OnKeyListener() {
        	// android:imeOptions="actionDone" doesn't work
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				//Log.d(TAG + ".onKey(...)", " - keyCode: " + keyCode);
				if (event != null && keyCode == KeyEvent.KEYCODE_ENTER) {
					processNif();
					return true;
				} else return false;
			}

        });
        Button solicitarButton = (Button) findViewById(R.id.solicitar_button);
        solicitarButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	processNif();
            }
        });

        mainLayout = (FrameLayout) findViewById(R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        progressMessage = (TextView)findViewById(R.id.progressMessage);
        mainLayout.getForeground().setAlpha(0);
        isProgressShown = false;
        isDestroyed = false;
        progressMessage.setText(R.string.loading_data_msg);
    }

    @Override public void onStart() {
    	Log.d(TAG + ".onStart(...) ", " --- onStart --- ");
    	super.onStart();
    }

    @Override //android:configChanges="orientation"
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG + ".onConfigurationChanged(...) ", " --- onConfigurationChanged --- ");
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	Log.d(TAG + ".onConfigurationChanged(...) ", " - ORIENTATION_LANDSCAPE - ");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        	Log.d(TAG + ".onConfigurationChanged(...) ", " - ORIENTATION_PORTRAIT - ");
        }
      }

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
	    		Intent intent = new Intent(this, NavigationDrawer.class);
	    		startActivity(intent);
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void processNif() {
    	InputMethodManager imm = (InputMethodManager)getSystemService(
  		      Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(nifText.getWindowToken(), 0);
      	if (validateForm ()) {

    		String givenName = Normalizer.normalize(
    				givennameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
    		givenName  = givenName.replaceAll("[^\\p{ASCII}]", "");
    		String surname = Normalizer.normalize(
    				 surnameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
    		surname  = surname.replaceAll("[^\\p{ASCII}]", "");
    		String nif = NifUtils.validate(nifText.getText().toString().toUpperCase());

			AlertDialog.Builder builder= new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.
    				request_certificate_form_lbl));
    		builder.setMessage(Html.fromHtml(
    				getString(R.string.cert_data_confirm_msg, givenName, surname, nif)));
    		builder.setPositiveButton(getString(
    				R.string.continue_label), new DialogInterface.OnClickListener() {
    		            public void onClick(DialogInterface dialog, int whichButton) {
    		              	showPinScreen(getString(
    		              			R.string.keyguard_password_enter_first_pin_code));
    		            }
    					});
    		builder.setNegativeButton(getString(
    				R.string.cancelar_button), null);
    		//builder.show();
    		Dialog dialog = builder.create();
            dialog.show();
    		TextView textView = ((TextView) dialog.findViewById(android.R.id.message));
            textView.setGravity(Gravity.CENTER);
      	}
    }

	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", " - caption: " 
				+ caption + "  - showMessage: " + message);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(message).show();
	}
    
    private boolean validateForm () {
    	Log.d(TAG + ".validateForm", " - validateForm");
    	if(NifUtils.validate(nifText.getText().toString()) == null) {
    		showMessage(getString(R.string.error_lbl), getString(R.string.nif_error));
    		return false;
    	}
    	if("".equals(givennameText.getText().toString().trim()) ){
    		showMessage(getString(R.string.error_lbl), getString(R.string.givenname_missing_msg));
    		return false;
    	} 
    	if("".equals(surnameText.getText().toString().trim()) ){
    		showMessage(getString(R.string.error_lbl), getString(R.string.surname_missing_msg));
    		return false;
    	}
    	TelephonyManager telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
    	phone = telephonyManager.getLine1Number();
    	//IMSI
    	//phone = telephonyManager.getSubscriberId();
    	deviceId = telephonyManager.getDeviceId();
    	if(deviceId == null || "".equals(deviceId.trim())) {
    		deviceId = android.os.Build.SERIAL;
    		if(deviceId == null || "".equals(deviceId.trim())) {
    			deviceId = UUID.randomUUID().toString();
    		}
    	}
		Log.d(TAG + ".validateForm() ", " - deviceId: " + deviceId);
    	return true;
    }
    
    private void showPinScreen(String message) {
    	CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, true);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag("pinDialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    pinDialog.show(ft, "pinDialog");
    }
	
	@Override public void setPin(String pin) {
		Log.d(TAG + ".setPin(...) ", " --- pin");
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag("pinDialog");
	    if (prev != null) ft.remove(prev);
	    ft.addToBackStack(null);
		password = pin;
		if(password == null) return;
        if(sendDataTask != null) sendDataTask.cancel(true);
        sendDataTask = new SendDataTask();
        sendDataTask.execute(contextVS.getAccessControlVS().getUserCSRServiceURL());
	}

    public void showProgress(boolean shown, boolean animate) {
        if (isProgressShown == shown) {
            return;
        }
        isProgressShown = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha( 0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {return false;}
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) { return true; }
            });
        }

    }

    public class SendDataTask extends AsyncTask<String, Void, ResponseVS> {

        public static final String TAG = "SendDataTask";


        public SendDataTask() { }

        protected void onPreExecute() {
            Log.d(TAG + ".SendDataTask.onPreExecute(...)", " --- onPreExecute");
            getWindow().getDecorView().findViewById(
                    android.R.id.content).invalidate();
            showProgress(true, true);
        }


        @Override
        protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".SendDataTask.doInBackground(...)", " - url:" + urls[0]);
            byte[] csrBytes = null;
            try {
                String givenName = Normalizer.normalize(
                        givennameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
                givenName  = givenName.replaceAll("[^\\p{ASCII}]", "");
                String surname = Normalizer.normalize(
                        surnameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
                surname  = surname.replaceAll("[^\\p{ASCII}]", "");
                String nif = NifUtils.validate(nifText.getText().toString().toUpperCase());
                pkcs10WrapperClient = PKCS10WrapperClient.buildCSRUsuario (KEY_SIZE, SIG_NAME,
                        SIGNATURE_ALGORITHM, PROVIDER, nif, email, phone, deviceId, givenName, surname);
                csrBytes = pkcs10WrapperClient.getCsrPEM();
                X509Certificate[] arrayCerts = CertUtil.generateCertificate(pkcs10WrapperClient.getKeyPair(),
                        new Date(System.currentTimeMillis()),
                        new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000),
                        "CN=" + USER_CERT_ALIAS);
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                keyStore.setKeyEntry(USER_CERT_ALIAS, pkcs10WrapperClient.getPrivateKey(),
                        password.toCharArray(), arrayCerts);
                byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
                FileOutputStream fos = openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
                fos.write(keyStoreBytes);
                fos.close();
                return HttpHelper.sendData(csrBytes, null, urls[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR,
                        ex.getMessage());
            }
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".SendDataTask.onPostExecute", " - statusCode: " +
                    responseVS.getStatusCode());
            showProgress(false, true);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = settings.edit();
                Long idSolictud = Long.valueOf(responseVS.getMessage());
                editor.putLong(CSR_REQUEST_ID_KEY, idSolictud);
                editor.commit();
                contextVS.setState(ContextVS.State.WITH_CSR);
                Intent intent = new Intent(getBaseContext(),
                        UserCertResponseActivity.class);
                startActivity(intent);
            } else {
                AlertDialog.Builder builder= new AlertDialog.Builder(UserCertRequestActivity.this);
                builder.setTitle(R.string.alert_exception_caption).setMessage(responseVS.getMessage())
                        .setPositiveButton("OK", null).show();
            }
        }


    }
}