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
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.PREFS_ID_SOLICTUD_CSR;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class UserCertResponseForm extends FragmentActivity 
	implements CertPinDialogListener {
	
	public static final String TAG = "UserCertResponseForm";
	
	
	private ProgressDialog progressDialog = null;
	private String CSR_SIGNED = "csrSigned";
	private String csrFirmado = null;
	private String SCREEN_MESSAGE = "screenMessage";
	private String screenMessage = null;
	private boolean isCertStateChecked = false;
	private String CERT_CHECKED = "isCertStateChecked";
    private Button goAppButton;
    private Button insertPinButton;
    private Button requestCertButton;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate");
        setContentView(R.layout.user_cert_response_screen);  
		try {//android api 11 I don't have this method
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".setTitle(...)", " --- android api 11 doesn't have method 'setLogo'");
		}  
        goAppButton = (Button) findViewById(R.id.go_app_button);
        goAppButton.setVisibility(View.GONE);
        goAppButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = new Intent(getApplicationContext(), FragmentTabsPager.class);
            	startActivity(intent);
            }
        });
        insertPinButton = (Button) findViewById(R.id.insert_pin_button);
        insertPinButton.setVisibility(View.GONE);
        insertPinButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	showPinScreen(getString(R.string.enter_pin_import_cert_msg));
            }
        });
        requestCertButton = (Button) findViewById(R.id.request_cert_button);
        requestCertButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = null;
          	  	switch(Aplicacion.INSTANCIA.getEstado()) {
			    	case SIN_CSR:
			    		intent = new Intent(getApplicationContext(), Aplicacion.class);
			    		break;
			    	case CON_CSR:
			    	case CON_CERTIFICADO:
			    		intent = new Intent(getApplicationContext(), UserCertRequestForm.class);
			    		break;
          	  	}
          	  	if(intent != null) {
	          	  	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            	startActivity(intent);
          	  	}
            }
        });
        if(savedInstanceState != null) 
        	isCertStateChecked = savedInstanceState.getBoolean(CERT_CHECKED, false);
        Log.d(TAG + ".onCreate() ", " --- savedInstanceState: " + isCertStateChecked);	
        checkCertState();
    }
    
    private void checkCertState () {
  	  	switch(Aplicacion.INSTANCIA.getEstado()) {
	    	case SIN_CSR:
	    		Intent intent = new Intent(getApplicationContext(), Aplicacion.class);
	    		startActivity(intent);
	    		break;
	    	case CON_CSR:
	    		if(isCertStateChecked) break;
	        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	        	Long idSolicitudCSR = settings.getLong(PREFS_ID_SOLICTUD_CSR, -1);
	        	/*if(idSolicitudCSR < 0) {
	    			setMessage(getString(R.string.app_unstable_msg));
	        		return;
	        	} */
	        	Log.d(TAG + ".checkCertState() ", "- idSolicitudCSR: " + idSolicitudCSR);	
	            progressDialog = ProgressDialog.show(
	            		UserCertResponseForm.this,
	            		getString(R.string.connecting_caption),
	            		getString(R.string.cert_state_msg), true,
	    	            true, new DialogInterface.OnCancelListener() {
	    	                @Override
	    	                public void onCancel(DialogInterface dialog) { 
	    	                	/*if (getDataTask != null) {
	    	                		getDataTask.cancel(true);
	    	                	}*/
	    	                }
	                });
	            try {
	            	GetDataTask getDataTask = (GetDataTask)new GetDataTask(null).
	            			execute(ServerPaths.getURLSolicitudCertificadoUsuario(
		        			CONTROL_ACCESO_URL, String.valueOf(idSolicitudCSR)));
	            	Respuesta respuesta = getDataTask.get();
	            	if (progressDialog != null && progressDialog.isShowing()) {
			            progressDialog.dismiss();
			        }
	            	Log.d(TAG + ".checkCertState() ", "- getDataTask - statusCode: " + respuesta.getCodigoEstado());	
	            	if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
			        	setCsrFirmado(respuesta.getMensaje());
			        	setMessage(getString(R.string.cert_downloaded_msg));
			            insertPinButton.setVisibility(View.VISIBLE);
				        setCertStateChecked(true);
			        } else if(Respuesta.SC_NOT_FOUND == respuesta.getCodigoEstado()) {
			        	String certificationAddresses = ServerPaths.
			        			getURLCertificationAddresses(Aplicacion.CONTROL_ACCESO_URL);
			        	setMessage(getString(R.string.
			        			resultado_solicitud_certificado_activity, 
			        			certificationAddresses));
			        } else showException(getString(
			        		R.string.request_user_cert_error_msg));
			        goAppButton.setVisibility(View.VISIBLE);
	            } catch(Exception ex) {
	            	ex.printStackTrace();
	            	showException(ex.getMessage());
	            }
  	  	}
    	
    }
    
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d(TAG + ".onSaveInstanceState(...) ", " ------ onSaveInstanceState -------");
		savedInstanceState.putBoolean(CERT_CHECKED, isCertStateChecked);
		savedInstanceState.putString(CSR_SIGNED, csrFirmado);
		savedInstanceState.putString(SCREEN_MESSAGE, screenMessage);
	}
	
	private void setCertStateChecked(boolean isChecked) {
		isCertStateChecked = isChecked;
	}
	
	private void setCsrFirmado (String csrFirmado) {
		this.csrFirmado = csrFirmado;
	}
    
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {		
		Log.d(TAG + ".onRestoreInstanceState(...) ", " ------ onRestoreInstanceState -------");
		setMessage(savedInstanceState.getString(SCREEN_MESSAGE));
		csrFirmado = savedInstanceState.getString(CSR_SIGNED);
		isCertStateChecked = savedInstanceState.getBoolean(CERT_CHECKED, false);
		if(isCertStateChecked) {
			if(csrFirmado != null)
				insertPinButton.setVisibility(View.VISIBLE);
			else goAppButton.setVisibility(View.VISIBLE);
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
	
    private void showPinScreen(String message) {
    	CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag("pinDialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    pinDialog.show(ft, "pinDialog");
    }
    
    private boolean actualizarKeyStore (char[] password) {
    	Log.d(TAG + ".actualizarKeyStore(...)", "");
    	if (csrFirmado == null) {
    		Log.d(TAG + ".actualizarKeyStore(...)", " - csrFirmado: " + csrFirmado);
    		return false;
    	}
    	try {
    		FileInputStream fis = openFileInput(KEY_STORE_FILE);
			byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey privateKey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
	        Collection<X509Certificate> certificados = 
	        		CertUtil.fromPEMToX509CertCollection(csrFirmado.getBytes());
	        Log.d(TAG + ".actualizarKeyStore(...)", " - certificados.size(): " + certificados.size());
	        for(X509Certificate cert:certificados) {
	        	Log.d(TAG + ".actualizarKeyStore(...)", "************ cert.toString(): " + cert.toString());
	        }
	        X509Certificate[] arrayCerts = new X509Certificate[certificados.size()];
	        certificados.toArray(arrayCerts);
	        keyStore.setKeyEntry(ALIAS_CERT_USUARIO, privateKey, password, arrayCerts);
	        keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password);
	        FileOutputStream fos = openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
	        fos.write(keyStoreBytes);
	        fos.close();
        	Aplicacion.setEstado(Aplicacion.Estado.CON_CERTIFICADO);
    		return true;
		} catch (Exception ex) {
			Log.e(TAG, " - ex.getMessage(): " + ex.getMessage());
			showException(getString(R.string.pin_error_msg));
    		return false;
		}
    }
    
    private void setMessage(String message) {
		Log.d(TAG + ".setMessage(...) ", " - message: " + message);
		this.screenMessage = message;
    	TextView contenidoTextView = (TextView) findViewById(R.id.text);
    	contenidoTextView.setText(Html.fromHtml(message));
        contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
    
	public void showException(String exMessage) {
		Log.d(TAG + ".showException(...) ", " - exMessage: " + exMessage);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(getString(
				R.string.alert_exception_caption)).setMessage(exMessage)
		.setPositiveButton(getString(
				R.string.ok_button), null).show();
	}

	@Override
	public void setPin(String pin) {
		if(pin != null) {
			if(actualizarKeyStore(pin.toCharArray())) {
				setMessage(getString(
	    				R.string.resultado_solicitud_certificado_activity_ok));
			    insertPinButton.setVisibility(View.GONE);
			    requestCertButton.setVisibility(View.GONE);
			} else {
				setMessage(getString(
						R.string.cert_install_error_msg));
			}
		} else {
			setMessage(getString(
					R.string.cert_install_error_msg));
		} 
		goAppButton.setVisibility(View.VISIBLE);
	}
	
}