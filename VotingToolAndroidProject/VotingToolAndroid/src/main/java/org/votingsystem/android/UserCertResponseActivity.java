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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;

import static org.votingsystem.model.ContextVS.*;


public class UserCertResponseActivity extends ActionBarActivity
	implements CertPinDialogListener {
	
	public static final String TAG = "UserCertResponseActivity";
	
	
	private ProgressDialog progressDialog = null;
	private String CSR_SIGNED = "csrSigned";
	private String csrSigned = null;
	private String SCREEN_MESSAGE = "screenMessage";
	private String screenMessage = null;
	private boolean isCertStateChecked = false;
	private String CERT_CHECKED = "isCertStateChecked";
    private Button goAppButton;
    private Button insertPinButton;
    private Button requestCertButton;
    private ContextVS contextVS;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate");
        setContentView(R.layout.user_cert_response_activity);
        contextVS = ContextVS.getInstance(getBaseContext());
        getSupportActionBar().setTitle(getString(R.string.voting_system_lbl));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        goAppButton = (Button) findViewById(R.id.go_app_button);
        goAppButton.setVisibility(View.GONE);
        goAppButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
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
          	  	switch(contextVS.getState()) {
			    	case WITHOUT_CSR:
			    		intent = new Intent(getBaseContext(), MainActivity.class);
			    		break;
			    	case WITH_CSR:
			    	case WITH_CERTIFICATE:
			    		intent = new Intent(getBaseContext(), UserCertRequestActivity.class);
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
  	  	switch(contextVS.getState()) {
	    	case WITHOUT_CSR:
	    		Intent intent = new Intent(getBaseContext(), MainActivity.class);
	    		startActivity(intent);
	    		break;
	    	case WITH_CSR:
	    		if(isCertStateChecked) break;
	        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	        	Long csrRequestId = settings.getLong(CSR_REQUEST_ID_KEY, -1);
	        	Log.d(TAG + ".checkCertState() ", "- csrRequestId: " + csrRequestId);
                GetDataTask getDataTask = new GetDataTask(null);
                getDataTask.execute(contextVS.getAccessControl().getUserCSRServiceURL(csrRequestId));
  	  	}
    }

    private void showProgressDialog(String title, String dialogMessage) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(dialogMessage);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d(TAG + ".onSaveInstanceState(...) ", " ------ onSaveInstanceState -------");
		savedInstanceState.putBoolean(CERT_CHECKED, isCertStateChecked);
		savedInstanceState.putString(CSR_SIGNED, csrSigned);
		savedInstanceState.putString(SCREEN_MESSAGE, screenMessage);
	}
	
	private void setCertStateChecked(boolean isChecked) {
		isCertStateChecked = isChecked;
	}
	
	private void setCsrSigned (String csrSigned) {
		this.csrSigned = csrSigned;
	}
    
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {		
		Log.d(TAG + ".onRestoreInstanceState(...) ", " ------ onRestoreInstanceState -------");
		setMessage(savedInstanceState.getString(SCREEN_MESSAGE));
		csrSigned = savedInstanceState.getString(CSR_SIGNED);
		isCertStateChecked = savedInstanceState.getBoolean(CERT_CHECKED, false);
		if(isCertStateChecked) {
			if(csrSigned != null)
				insertPinButton.setVisibility(View.VISIBLE);
			else goAppButton.setVisibility(View.VISIBLE);
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
    
    private boolean updateKeyStore (char[] password) {
    	Log.d(TAG + ".updateKeyStore(...)", "");
    	if (csrSigned == null) {
    		Log.d(TAG + ".updateKeyStore(...)", " - csrSigned: " + csrSigned);
    		return false;
    	}
    	try {
    		FileInputStream fis = openFileInput(KEY_STORE_FILE);
			byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
	        Collection<X509Certificate> certificates =
                    CertUtil.fromPEMToX509CertCollection(csrSigned.getBytes());
	        Log.d(TAG + ".updateKeyStore(...)", " - certificates.size(): " + certificates.size());
	        for(X509Certificate cert:certificates) {
	        	Log.d(TAG + ".updateKeyStore(...)", "************ cert.toString(): " + cert.toString());
	        }
	        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
	        certificates.toArray(arrayCerts);
	        keyStore.setKeyEntry(USER_CERT_ALIAS, privateKey, password, arrayCerts);
	        keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password);
	        FileOutputStream fos = openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
	        fos.write(keyStoreBytes);
	        fos.close();
            contextVS.setState(ContextVS.State.WITH_CERTIFICATE);
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
    	TextView contentTextView = (TextView) findViewById(R.id.text);
    	contentTextView.setText(Html.fromHtml(message));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
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
			if(updateKeyStore(pin.toCharArray())) {
				setMessage(getString(
	    				R.string.request_cert_result_activity_ok));
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

    public class GetDataTask extends AsyncTask<String, Void, ResponseVS> {

        public static final String TAG = "GetDataTask";


        private ContentTypeVS contentType = null;

        public GetDataTask(ContentTypeVS contentType) {
            this.contentType = contentType;
        }

        @Override protected void onPreExecute() {
            showProgressDialog(getString(R.string.connecting_caption), getString(R.string.cert_state_msg));
        }

        @Override
        protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".doInBackground", " - url: " + urls[0]);
            return  HttpHelper.getData(urls[0], contentType);
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + "GetDataTask.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                setCsrSigned(responseVS.getMessage());
                setMessage(getString(R.string.cert_downloaded_msg));
                insertPinButton.setVisibility(View.VISIBLE);
                setCertStateChecked(true);
            } else if(ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                String certificationAddresses = contextVS.getAccessControl().getCertificationCentersURL();
                setMessage(getString(R.string.request_cert_result_activity, certificationAddresses));
            } else showException(getString(
                    R.string.request_user_cert_error_msg));
            goAppButton.setVisibility(View.VISIBLE);
        }
    }

}