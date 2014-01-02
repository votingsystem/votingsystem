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

package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.UserCertRequestFormFragment;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.CSR_REQUEST_ID_KEY;
import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;


public class UserCertResponseActivity extends ActionBarActivity {
	
	public static final String TAG = "UserCertResponseActivity";
	
	
	private ProgressDialog progressDialog = null;
	private String CSR_SIGNED = "csrSigned";
	private String csrSigned = null;
	private String SCREEN_MESSAGE = "screenMessage";
	private String screenMessage = null;
	private AtomicBoolean isCertStateChecked = new AtomicBoolean(false);
	private String CERT_CHECKED = "isCertStateChecked";
    private Button goAppButton;
    private Button insertPinButton;
    private Button requestCertButton;
    private ContextVS contextVS;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            if(pin != null) updateKeyStore(pin);
        }
    };

    private void updateKeyStore (String pin) {
        Log.d(TAG + ".updateKeyStore(...)", "");
        if (csrSigned == null) {
            setMessage(getString(R.string.cert_install_error_msg));
        } else {
            try {
                FileInputStream fis = openFileInput(KEY_STORE_FILE);
                byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
                        keyStoreBytes, pin.toCharArray());
                PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS,
                        pin.toCharArray());
                Collection<X509Certificate> certificates =
                        CertUtil.fromPEMToX509CertCollection(csrSigned.getBytes());
                Log.d(TAG + ".updateKeyStore(...)", "certificates.size(): " + certificates.size());
                X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
                certificates.toArray(arrayCerts);
                keyStore.setKeyEntry(USER_CERT_ALIAS, privateKey, pin.toCharArray(), arrayCerts);
                keyStoreBytes = KeyStoreUtil.getBytes(keyStore, pin.toCharArray());
                FileOutputStream fos = openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
                fos.write(keyStoreBytes);
                fos.close();
                contextVS.setState(ContextVS.State.WITH_CERTIFICATE);
                setMessage(getString(R.string.request_cert_result_activity_ok));
                insertPinButton.setVisibility(View.GONE);
                requestCertButton.setVisibility(View.GONE);
            } catch (Exception ex) {
                ex.printStackTrace();
                showException(getString(R.string.pin_error_msg));
            }
        }
        goAppButton.setVisibility(View.VISIBLE);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.user_cert_response_activity);
        contextVS = ContextVS.getInstance(getBaseContext());
        Log.d(TAG + ".onCreate(...) ", "state: " + contextVS.getState() +
                " - savedInstanceState: " + savedInstanceState);
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
			    		intent = new Intent(getBaseContext(), CertRequestActivity.class);
			    		break;
			    	case WITH_CSR:
			    	case WITH_CERTIFICATE:
			    		intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                        intent.putExtra(ContextVS.FRAGMENT_KEY,
                                UserCertRequestFormFragment.class.getName());
			    		break;
          	  	}
          	  	if(intent != null) {
	          	  	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            	startActivity(intent);
          	  	}
            }
        });
        if(savedInstanceState != null) 
        	isCertStateChecked.set(savedInstanceState.getBoolean(CERT_CHECKED, false));
        Log.d(TAG + ".onCreate() ", "isCertStateChecked: " + isCertStateChecked);
        checkCertState();
    }
    
    private void checkCertState () {
  	  	switch(contextVS.getState()) {
	    	case WITHOUT_CSR:
	    		Intent intent = new Intent(getBaseContext(), CertRequestActivity.class);
	    		startActivity(intent);
	    		break;
	    	case WITH_CSR:
	    		if(isCertStateChecked.get()) break;
	        	SharedPreferences settings = getSharedPreferences(
                        VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
	        	Long csrRequestId = settings.getLong(CSR_REQUEST_ID_KEY, -1);
	        	Log.d(TAG + ".checkCertState() ", "csrRequestId: " + csrRequestId);
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

	@Override public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(CERT_CHECKED, isCertStateChecked.get());
        outState.putString(CSR_SIGNED, csrSigned);
        outState.putString(SCREEN_MESSAGE, screenMessage);
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState: " + outState);
	}
	
	private void setCsrSigned (String csrSigned) {
		this.csrSigned = csrSigned;
	}
    
	@Override public void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.d(TAG + ".onRestoreInstanceState(...) ", "savedInstanceState: " + savedInstanceState);
		setMessage(savedInstanceState.getString(SCREEN_MESSAGE));
		csrSigned = savedInstanceState.getString(CSR_SIGNED);
		isCertStateChecked.set(savedInstanceState.getBoolean(CERT_CHECKED, false));
		if(isCertStateChecked.get()) {
			if(csrSigned != null)
				insertPinButton.setVisibility(View.VISIBLE);
			else goAppButton.setVisibility(View.VISIBLE);
		}
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
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

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
        Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, false, this.getClass().getName());
        pinDialog.show(getSupportFragmentManager(), PinDialogFragment.TAG);
    }
    
    private void setMessage(String message) {
		Log.d(TAG + ".setMessage(...) ", "message: " + message);
		this.screenMessage = message;
    	TextView contentTextView = (TextView) findViewById(R.id.text);
    	contentTextView.setText(Html.fromHtml(message));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
    
	public void showException(String exMessage) {
		Log.d(TAG + ".showException(...) ", "exMessage: " + exMessage);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_exception_caption)).setMessage(exMessage)
		.setPositiveButton(getString(R.string.ok_button), null).show();
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

        @Override protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".doInBackground", " - url: " + urls[0]);
            return  HttpHelper.getData(urls[0], contentType);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + "GetDataTask.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                setCsrSigned(responseVS.getMessage());
                setMessage(getString(R.string.cert_downloaded_msg));
                insertPinButton.setVisibility(View.VISIBLE);
                isCertStateChecked.set(true);
            } else if(ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                String certificationAddresses = contextVS.getAccessControl().getCertificationCentersURL();
                setMessage(getString(R.string.request_cert_result_activity, certificationAddresses));
            } else showException(getString(
                    R.string.request_user_cert_error_msg));
            goAppButton.setVisibility(View.VISIBLE);
        }
    }

}