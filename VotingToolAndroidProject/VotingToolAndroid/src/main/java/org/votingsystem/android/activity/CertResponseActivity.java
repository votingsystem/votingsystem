package org.votingsystem.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.CertRequestFormFragment;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.ProgressDialogFragment;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.CSR_REQUEST_ID_KEY;
import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertResponseActivity extends ActionBarActivity {
	
	public static final String TAG = CertResponseActivity.class.getSimpleName();

	private String CSR_SIGNED = "csrSigned";
	private String csrSigned = null;
	private String SCREEN_MESSAGE = "screenMessage";
	private String screenMessage = null;
	private AtomicBoolean isCertStateChecked = new AtomicBoolean(false);
	private String CERT_CHECKED = "isCertStateChecked";
    private Button goAppButton;
    private Button insertPinButton;
    private Button requestCertButton;
    private AppContextVS contextVS;
    private String broadCastId = CertResponseActivity.class.getSimpleName();;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        if(intent.getStringExtra(PIN_KEY) != null) updateKeyStore(intent.getStringExtra(PIN_KEY));
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.cert_request_form_response);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        contextVS = (AppContextVS) getApplicationContext();
        LOGD(TAG + ".onCreate", "state: " + contextVS.getState() +
                " - savedInstanceState: " + savedInstanceState);
        getSupportActionBar().setTitle(getString(R.string.voting_system_lbl));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        goAppButton = (Button) findViewById(R.id.go_app_button);
        goAppButton.setVisibility(View.GONE);
        goAppButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = new Intent(getBaseContext(), EventVSMainActivity.class);
            	startActivity(intent);
            }
        });
        insertPinButton = (Button) findViewById(R.id.insert_pin_button);
        insertPinButton.setVisibility(View.GONE);
        insertPinButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            PinDialogFragment.showPinScreenWithoutHashValidation(
                    getSupportFragmentManager(), false, broadCastId,
                    getString(R.string.enter_pin_import_cert_msg), null);
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
        LOGD(TAG + ".onCreate() ", "isCertStateChecked: " + isCertStateChecked);
        checkCertState();
    }

    private void updateKeyStore (String pin) {
        LOGD(TAG + ".updateKeyStore", "");
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
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                            getString(R.string.pin_error_msg), getSupportFragmentManager());
                    return;
                }
                PrivateKey privateKey = certificationRequest.getPrivateKey();
                Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                        csrSigned.getBytes());
                X509Certificate x509Cert = certificates.iterator().next();
                JSONObject deviceData = CertUtils.getCertExtensionData(x509Cert, ContextVS.DEVICEVS_OID);
                PrefUtils.putApplicationId(deviceData.getString("deviceId"), this);
                UserVS user = UserVS.getUserVS(x509Cert);
                LOGD(TAG + ".updateKeyStore", "user: " + user.getNif() +
                        " - certificates.size(): " + certificates.size());
                X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                certificates.toArray(certsArray);

                String walletBase64 = PrefUtils.getWallet(contextVS);
                JSONArray wallet = null;
                if(walletBase64 != null) {
                    byte[] walletBytes = contextVS.decryptMessage(walletBase64.getBytes());
                    wallet = new JSONArray(new String(walletBytes, "UTF-8"));
                }
                keyStore.setKeyEntry(USER_CERT_ALIAS, privateKey, null, certsArray);
                PrefUtils.putAppCertState(contextVS.getAccessControl().getServerURL(),
                        State.WITH_CERTIFICATE, user.getNif(), contextVS);
                PrefUtils.putPin(Integer.valueOf(pin), contextVS);
                if(wallet != null) {
                    Wallet.saveWallet(wallet, pin, contextVS);
                }
                setMessage(getString(R.string.request_cert_result_activity_ok));
                PrefUtils.putSessionUserVS(user, this);
                insertPinButton.setVisibility(View.GONE);
                requestCertButton.setVisibility(View.GONE);
            } catch (Exception ex) {
                ex.printStackTrace();
                MessageDialogFragment.showDialog(ResponseVS.getExceptionResponse(ex, this),
                        getSupportFragmentManager());
            }
        }
        goAppButton.setVisibility(View.VISIBLE);
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
	        	LOGD(TAG + ".checkCertState() ", "csrRequestId: " + csrRequestId);
                new GetDataTask(null).execute(
                        contextVS.getAccessControl().getUserCSRServiceURL(csrRequestId));
  	  	}
    }

	@Override public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(CERT_CHECKED, isCertStateChecked.get());
        outState.putString(CSR_SIGNED, csrSigned);
        outState.putString(SCREEN_MESSAGE, screenMessage);
	}
	
	private void setCsrSigned (String csrSigned) {
		this.csrSigned = csrSigned;
	}
    
	@Override public void onRestoreInstanceState(Bundle savedInstanceState) {
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
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
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
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }
    
    private void setMessage(String message) {
		LOGD(TAG + ".setMessage", "message: " + message);
		this.screenMessage = message;
    	TextView contentTextView = (TextView) findViewById(R.id.text);
    	contentTextView.setText(Html.fromHtml(message));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public class GetDataTask extends AsyncTask<String, Void, ResponseVS> {

        public final String TAG = GetDataTask.class.getSimpleName();

        private ContentTypeVS contentType = null;

        public GetDataTask(ContentTypeVS contentType) {
            this.contentType = contentType;
        }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.connecting_caption),
                    getString(R.string.cert_state_msg), getSupportFragmentManager());
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            LOGD(TAG + ".doInBackground", "url: " + urls[0]);
            return  HttpHelper.getData(urls[0], contentType);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            LOGD(TAG + "GetDataTask.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
            ProgressDialogFragment.hide(getSupportFragmentManager());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                setCsrSigned(responseVS.getMessage());
                setMessage(getString(R.string.cert_downloaded_msg));
                insertPinButton.setVisibility(View.VISIBLE);
                isCertStateChecked.set(true);
            } else if(ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                String certificationAddresses = contextVS.getAccessControl().getCertificationCentersURL();
                setMessage(getString(R.string.request_cert_result_activity, certificationAddresses));
            } else MessageDialogFragment.showDialog(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.request_user_cert_error_msg), getSupportFragmentManager());
            goAppButton.setVisibility(View.VISIBLE);
        }
    }

}