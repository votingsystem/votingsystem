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

package org.sistemavotacion.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.task.SendDataTask;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.StringUtils;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.Date;
import java.util.UUID;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.PREFS_ID_SOLICTUD_CSR;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;

public class UserCertRequestForm extends FragmentActivity 
		implements CertPinDialogListener {

	public static final String TAG = "UserCertRequestForm";
	
    private ProgressDialog progressDialog = null;
    private String password = null;
    private String email = null;
    private String telefono = null;
    private String deviceId = null;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private EditText nifText;
    private EditText givennameText;
    private EditText surnameText;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate - ");
        setContentView(R.layout.user_cert_request_form); 
		try {//android api 11 I don't have this method
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".setTitle(...)", " --- android api 11 doesn't have method 'setLogo'");
		}  
        setTitle(getString(R.string.formulario_solicitud_certificado_label));
        
        Button cancelarButton = (Button) findViewById(R.id.cancelar_button);
        cancelarButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { 
            	//finish(); 
            	Intent intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
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
	    		Intent intent = new Intent(this, FragmentTabsPager.class);   
	    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
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
      	if (validarFormulario ()) {
      		
    		String givenName = Normalizer.normalize(
    				givennameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
    		givenName  = givenName.replaceAll("[^\\p{ASCII}]", "");
    		String surname = Normalizer.normalize(
    				 surnameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
    		surname  = surname.replaceAll("[^\\p{ASCII}]", "");
    		String nif = StringUtils.validarNIF(nifText.getText().toString().toUpperCase());
      		
      		
      		
			AlertDialog.Builder builder= new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.
    				formulario_solicitud_certificado_label));
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
    
    private void showProgressDialog(String dialogMessage) {
        if (progressDialog == null) 
        	progressDialog = new ProgressDialog(UserCertRequestForm.this);
    	progressDialog.setMessage(dialogMessage);
    	progressDialog.setIndeterminate(true);
    	progressDialog.setCancelable(false);
        progressDialog.show();
    }
    
    private void sendCsrRequest() {
        showProgressDialog(getString(R.string.request_cert_msg));
        byte[] csrBytes = null;
    	try {
    		String givenName = Normalizer.normalize(
    				givennameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
    		givenName  = givenName.replaceAll("[^\\p{ASCII}]", "");
    		String surname = Normalizer.normalize(
    				 surnameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
    		surname  = surname.replaceAll("[^\\p{ASCII}]", "");
    		String nif = StringUtils.validarNIF(nifText.getText().toString().toUpperCase());
			pkcs10WrapperClient = PKCS10WrapperClient.buildCSRUsuario (KEY_SIZE, SIG_NAME, 
			        SIGNATURE_ALGORITHM, PROVIDER, nif, email, telefono, deviceId, givenName, surname);
			csrBytes = pkcs10WrapperClient.getPEMEncodedRequestCSR();
	        X509Certificate[] arrayCerts = CertUtil.generateCertificate(pkcs10WrapperClient.getKeyPair(), 
	        		new Date(System.currentTimeMillis()), 
	        		new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000),
	        		"CN=" + ALIAS_CERT_USUARIO);
	        KeyStore keyStore = KeyStore.getInstance("PKCS12");
	        keyStore.load(null, null);
	        keyStore.setKeyEntry(ALIAS_CERT_USUARIO, pkcs10WrapperClient.getPrivateKey(), 
	        		password.toCharArray(), arrayCerts);
	        byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
	        FileOutputStream fos = openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
	        fos.write(keyStoreBytes);
	        fos.close();
	        Aplicacion.setEstado(Aplicacion.Estado.CON_CSR);
		} catch (Exception ex) {
			ex.printStackTrace();
			showMessage(getString(R.string.error_lbl), ex.getMessage());
		}
    	try {
    		SendDataTask sendDataTask = (SendDataTask) new SendDataTask(csrBytes, null).
    				execute(ServerPaths.getURLSolicitudCSRUsuario(Aplicacion.CONTROL_ACCESO_URL));
    		Respuesta respuesta = sendDataTask.get();
			Log.d(TAG + ".sendCsrRequest(...)", " - sendCsrRequest - sendDataTask - statuscode: " + respuesta.getCodigoEstado());
	        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
	        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		        SharedPreferences.Editor editor = settings.edit();
		        Long idSolictud = Long.valueOf(respuesta.getMensaje());
		        editor.putLong(PREFS_ID_SOLICTUD_CSR, idSolictud);
		        editor.commit();
		        Aplicacion.setEstado(Aplicacion.Estado.CON_CSR);
	        	Intent intent = new Intent(getApplicationContext(), 
	        			UserCertResponseForm.class);
	        	startActivity(intent);
	        } else {
	            if (progressDialog != null) progressDialog.dismiss();
				AlertDialog.Builder builder= new AlertDialog.Builder(UserCertRequestForm.this);
				builder.setTitle(R.string.alert_exception_caption).setMessage(respuesta.getMensaje())
					.setPositiveButton("OK", null).show();
	        }
    	} catch(Exception ex) {
    		ex.printStackTrace();
			showMessage(getString(R.string.error_lbl), ex.getMessage());
    	}

    }
    
	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", " - caption: " 
				+ caption + "  - showMessage: " + message);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(message).show();
	}
    
    private boolean validarFormulario () {
    	Log.d(TAG + ".validarFormulario", " - validarFormulario");
    	if(StringUtils.validarNIF(nifText.getText().toString()) == null) {
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
    	telefono = telephonyManager.getLine1Number();
    	//IMSI
    	//telefono = telephonyManager.getSubscriberId();
    	deviceId = telephonyManager.getDeviceId();
    	if(deviceId == null || "".equals(deviceId.trim())) {
    		deviceId = android.os.Build.SERIAL;
    		if(deviceId == null || "".equals(deviceId.trim())) {
    			deviceId = UUID.randomUUID().toString();
    		}
    	}
		Log.d(TAG + ".validarFormulario() ", " - deviceId: " + deviceId);
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
		sendCsrRequest();
	}

}