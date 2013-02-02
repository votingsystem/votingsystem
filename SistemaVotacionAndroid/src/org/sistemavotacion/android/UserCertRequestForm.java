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

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.PREFS_ID_SOLICTUD_CSR;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.SendDataTask;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.StringUtils;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class UserCertRequestForm extends SherlockFragmentActivity 
		implements CertPinDialogListener {

	public static final String TAG = "UserCertRequestForm";
	
    private ProgressDialog progressDialog = null;
    private String password = null;
    private String email = null;
    private String telefono = null;
    private String deviceId = null;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private EditText nifText;
	
    DataListener<String> envioCsrListener =new DataListener<String>() {

    	@Override
		public void updateData(int statusCode, String response) {
			Log.d(TAG + ".envioCsrListener.updateData() ", response);
	        if (progressDialog != null && progressDialog.isShowing()) {
	            progressDialog.dismiss();
	        }
	        if(Respuesta.SC_OK == statusCode) {
	        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		        SharedPreferences.Editor editor = settings.edit();
		        Long idSolictud = Long.valueOf(response);
		        editor.putLong(PREFS_ID_SOLICTUD_CSR, idSolictud);
		        editor.commit();
		        Aplicacion.setEstado(Aplicacion.Estado.CON_CSR);
	        	Intent intent = new Intent(getApplicationContext(), 
	        			UserCertResponseForm.class);
	        	startActivity(intent);
	        } else {
	        	setException(response);
	        }
		}
	
		@Override
		public void setException(String exceptionMsg) {
			Log.d(TAG + ".envioCsrListener.setException() ", " - exceptionMsg: " + exceptionMsg);	
	        if (progressDialog != null && progressDialog.isShowing()) {
	            progressDialog.dismiss();
	        }
			AlertDialog.Builder builder= new AlertDialog.Builder(UserCertRequestForm.this);
			builder.setTitle(R.string.alert_exception_caption).setMessage(exceptionMsg)
				.setPositiveButton("OK", null).show();
		}
    };
    
    DialogInterface.OnClickListener requestCertClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			Log.d(TAG + ".cancelRequestCertClickListener.onclick(...) ", " - which:" + which);
			if(which == DialogInterface.BUTTON_NEGATIVE) {
				finish();	
			}
		}};
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        setTheme(Aplicacion.THEME);
    	super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate");
        setContentView(R.layout.user_cert_request_form); 
        setTitle(getApplicationContext().getString(R.string.formulario_solicitud_certificado_label));
        
        Button cancelarButton = (Button) findViewById(R.id.cancelar_button);
        cancelarButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { 
            	//finish(); 
            	Intent intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
            	startActivity(intent);
            }
        });      
        nifText = (EditText)findViewById(R.id.nif_edit);
        
        nifText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Log.d(TAG + ".onKey(...)", " - keyCode: " + keyCode);
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
        if(getIntent().getBooleanExtra(Aplicacion.NEW_CERT_KEY, false)) {
            Log.d(TAG + ".onCreate(...)", " - Nuevo certificado !!!");
        	AlertDialog.Builder builder= new AlertDialog.Builder(UserCertRequestForm.this);
    		builder.setTitle(getString(R.string.
    				menu_solicitar_certificado));
    		builder.setMessage(Html.fromHtml(
    				getString(R.string.request_cert_again_msg)));
    		builder.setPositiveButton(getString(
    				R.string.ok_button), requestCertClickListener);
    		builder.setNegativeButton(getString(
    				R.string.cancelar_button), requestCertClickListener);
    		builder.show();
        }
    }
    
    private void processNif() {
    	InputMethodManager imm = (InputMethodManager)getSystemService(
  		      Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(nifText.getWindowToken(), 0);
      	if (validarFormulario ()) {
          	showPinScreen(getApplicationContext().getString(
          			R.string.keyguard_password_enter_first_pin_code));
      	}
    }
    
    private ProgressDialog getProgressDialog(String dialogMessage) {
        if (progressDialog == null) 
        	progressDialog = new ProgressDialog(UserCertRequestForm.this);
    	progressDialog.setMessage(dialogMessage);
    	progressDialog.setIndeterminate(true);
    	progressDialog.setCancelable(false);
        return progressDialog;
    }
    
    private void sendCsrRequest() {
        progressDialog = getProgressDialog(getApplicationContext().getString(R.string.request_cert_msg));
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	progressDialog.show();
    	    }
    	});   
        String csr = null;
    	try {
			pkcs10WrapperClient = PKCS10WrapperClient.buildCSRUsuario (KEY_SIZE, SIG_NAME, 
			        SIGNATURE_ALGORITHM, PROVIDER, nifText.getText().toString(), email, telefono, deviceId);
	        String privateKeyPEMString = pkcs10WrapperClient.getPrivateKeyPEMString();
	        csr = new String(pkcs10WrapperClient.getPEMEncodedRequestCSR());
	        /*X509Certificate cert = CertUtil.generateV1RootCert(pkcs10WrapperClient.getKeyPair(), 
	        		System.currentTimeMillis(), 
	        		Long.valueOf(System.currentTimeMillis()).intValue() + 365 * 24 * 60 * 60 * 1000,
	        		"CN=" + ALIAS_CERT_USUARIO);*/
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
		} catch (Exception e) {
			Log.e(TAG + "solicitarButton.onClick(...)", " e.getMessage(): " + e.getMessage());
			manejarExcepcion(e.getMessage());
		}
    	new SendDataTask(envioCsrListener, csr).execute(ServerPaths.getURLSolicitudCSRUsuario(
    			Aplicacion.CONTROL_ACCESO_URL));
    }
    
    private boolean validarFormulario () {
    	Log.d(TAG + ".validarFormulario", "");
    	if(StringUtils.validarNIF(nifText.getText().toString()) == null) {
    		Toast.makeText(UserCertRequestForm.this, 
            		R.string.nif_error, Toast.LENGTH_LONG).show();
    		return false;
    	}
    	TelephonyManager telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
    	//telefono = telephonyManager.getLine1Number();
    	//IMSI
    	telefono = telephonyManager.getSubscriberId();
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
		android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("pinDialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    pinDialog.show(ft, "pinDialog");
    }

	public void manejarExcepcion(String exMessage) {
		Log.d(TAG + ".manejarExcepcion(...) ", "Message:" + exMessage);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(getApplicationContext().getString(
				R.string.alert_exception_caption)).setMessage(exMessage)
				.setPositiveButton("OK", null).show();
	}

	@Override
	public void setPin(String pin) {
		Log.d(TAG + ".setPin(...) ", " --- pin");
		password = pin;
		if(password == null) return;
		android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("pinDialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
		sendCsrRequest();
	}
		
}