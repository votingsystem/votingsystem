package org.votingsystem.android.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.CertRequestFormFragment;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.CSR_REQUEST_ID_KEY;
import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.USER_DATA_FILE_NAME;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertResponseActivity extends FragmentActivity {
	
	public static final String TAG = CertResponseActivity.class.getSimpleName();
	
	
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
    private AppContextVS appContextVS;
    private String broadCastId;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            if(intent.getStringExtra(PIN_KEY) != null) updateKeyStore(intent.getStringExtra(PIN_KEY));
        }
    };

    private void updateKeyStore (String pin) {
        Log.d(TAG + ".updateKeyStore(...)", "");
        if (csrSigned == null) {
            setMessage(getString(R.string.cert_install_error_msg));
        } else {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                CertificationRequestVS certificationRequest = (CertificationRequestVS)
                        ObjectUtils.deSerializeObject(PrefUtils.getCsrRequest(this).getBytes());

                String passwordHash = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
                if(!passwordHash.equals(certificationRequest.getHashPin())) {
                    showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                            getString(R.string.pin_error_msg));
                    return;
                }
                PrivateKey privateKey = certificationRequest.getPrivateKey();
                Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                        csrSigned.getBytes());
                X509Certificate userCert = certificates.iterator().next();
                UserVS user = UserVS.getUserVS(userCert);
                Log.d(TAG + ".updateKeyStore(...)", "user: " + user.getNif() +
                        " - certificates.size(): " + certificates.size());
                X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                certificates.toArray(certsArray);
                keyStore.setKeyEntry(USER_CERT_ALIAS, privateKey, null, certsArray);

                byte[] userDataBytes = ObjectUtils.serializeObject(user);
                FileOutputStream outputStream = openFileOutput(USER_DATA_FILE_NAME,
                        Context.MODE_PRIVATE);
                outputStream.write(userDataBytes);
                outputStream.close();
                PrefUtils.putAppCertState(appContextVS, appContextVS.getAccessControl().getServerURL(),
                        State.WITH_CERTIFICATE, user.getNif());
                PrefUtils.setPin(appContextVS, pin);
                setMessage(getString(R.string.request_cert_result_activity_ok));
                PrefUtils.putSessionUserVS(this, user);
                insertPinButton.setVisibility(View.GONE);
                requestCertButton.setVisibility(View.GONE);
            } catch (Exception ex) {
                ex.printStackTrace();
                showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl), getString(R.string.pin_error_msg));
            }
        }
        goAppButton.setVisibility(View.VISIBLE);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.cert_request_form_response);
        appContextVS = (AppContextVS) getApplicationContext();
        broadCastId = CertResponseActivity.class.getSimpleName();
        Log.d(TAG + ".onCreate(...) ", "state: " + appContextVS.getState() +
                " - savedInstanceState: " + savedInstanceState);
        getActionBar().setTitle(getString(R.string.voting_system_lbl));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        goAppButton = (Button) findViewById(R.id.go_app_button);
        goAppButton.setVisibility(View.GONE);
        goAppButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = new Intent(getBaseContext(), EventsVSActivity.class);
            	startActivity(intent);
            }
        });
        insertPinButton = (Button) findViewById(R.id.insert_pin_button);
        insertPinButton.setVisibility(View.GONE);
        insertPinButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                PinDialogFragment.showPinScreenWithoutHashValidation(
                        getSupportFragmentManager(), broadCastId,
                        getString(R.string.enter_pin_import_cert_msg));
            }
        });
        requestCertButton = (Button) findViewById(R.id.request_cert_button);
        requestCertButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = null;
          	  	switch(appContextVS.getState()) {
			    	case WITHOUT_CSR:
			    		intent = new Intent(getBaseContext(), CertRequestActivity.class);
			    		break;
			    	case WITH_CSR:
			    	case WITH_CERTIFICATE:
			    		intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                        intent.putExtra(FRAGMENT_KEY, CertRequestFormFragment.class.getName());
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
  	  	switch(appContextVS.getState()) {
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
                getDataTask.execute(appContextVS.getAccessControl().getUserCSRServiceURL(csrRequestId));
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
                super.onBackPressed();
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }
    
    private void setMessage(String message) {
		Log.d(TAG + ".setMessage(...) ", "message: " + message);
		this.screenMessage = message;
    	TextView contentTextView = (TextView) findViewById(R.id.text);
    	contentTextView.setText(Html.fromHtml(message));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showMessage(Integer statusCode,String caption,String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
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
                String certificationAddresses = appContextVS.getAccessControl().getCertificationCentersURL();
                setMessage(getString(R.string.request_cert_result_activity, certificationAddresses));
            } else showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.request_user_cert_error_msg));
            goAppButton.setVisibility(View.VISIBLE);
        }
    }

}