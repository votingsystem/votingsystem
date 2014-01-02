package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.UserCertResponseActivity;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.NifUtils;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.Date;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.CSR_REQUEST_ID_KEY;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class UserCertRequestFragment extends Fragment implements CertPinDialogListener {

	public static final String TAG = "UserCertRequestFragment";

    private String email = null;
    private String phone = null;
    private String deviceId = null;
    private CertificationRequestVS certificationRequest;
    private EditText nifText;
    private EditText givennameText;
    private EditText surnameText;
    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean progressVisible;
    private SendDataTask sendDataTask;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateView(...)", "onCreateView");
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        View rootView = inflater.inflate(R.layout.user_cert_request_fragment, container, false);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        getActivity().setTitle(getString(R.string.request_certificate_form_lbl));
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //finish();
                Intent intent = new Intent(getActivity().getApplicationContext(),
                        NavigationDrawer.class);
                startActivity(intent);
            }
        });
        givennameText = (EditText)rootView.findViewById(R.id.given_name_edit);
        surnameText = (EditText)rootView.findViewById(R.id.surname_edit);
        nifText = (EditText)rootView.findViewById(R.id.nif_edit);
        nifText.setOnEditorActionListener(new OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
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
        Button requestButton = (Button) rootView.findViewById(R.id.request_button);
        requestButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                processNif();
            }
        });
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        progressMessage.setText(R.string.loading_data_msg);
        mainLayout.getForeground().setAlpha(0);
        progressVisible = false;
        // if set to true savedInstanceState will be allways null
        setRetainInstance(true);
        setHasOptionsMenu(true);
        return rootView;
    }


    @Override public void onStart() {
    	Log.d(TAG + ".onStart(...) ", "onStart");
    	super.onStart();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        setRetainInstance(true);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", "onStop");
    }


    @Override public void onResume() {
        super.onResume();
        Log.d(TAG + ".onResume() ", "onResume");
    }

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", "home");
	    		Intent intent = new Intent(getActivity().getApplicationContext(),
                        NavigationDrawer.class);
	    		startActivity(intent);
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void processNif() {
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
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

			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
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
    				R.string.cancel_button), null);
    		//builder.show();
    		Dialog dialog = builder.create();
            dialog.show();
    		TextView textView = ((TextView) dialog.findViewById(android.R.id.message));
            textView.setGravity(Gravity.CENTER);
      	}
    }

	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", "caption: " + caption + "  - showMessage: " + message);
		AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
		builder.setTitle(caption).setMessage(message).show();
	}

    private boolean validateForm () {
    	Log.d(TAG + ".validateForm", "validateForm");
    	if(NifUtils.validate(nifText.getText().toString()) == null) {
    		showMessage(getString(R.string.error_lbl), getString(R.string.nif_error));
    		return false;
    	}
    	if(TextUtils.isEmpty(givennameText.getText().toString())){
    		showMessage(getString(R.string.error_lbl), getString(R.string.givenname_missing_msg));
    		return false;
    	}
    	if(TextUtils.isEmpty(surnameText.getText().toString())){
    		showMessage(getString(R.string.error_lbl), getString(R.string.surname_missing_msg));
    		return false;
    	}
    	TelephonyManager telephonyManager = (TelephonyManager)getActivity().getSystemService(
                Context.TELEPHONY_SERVICE);
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
		Log.d(TAG + ".validateForm() ", "deviceId: " + deviceId);
    	return true;
    }


    private void showPinScreen(String message) {
        CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) ft.remove(prev);
        ft.addToBackStack(null);
        pinDialog.show(ft, CertPinDialog.TAG);
    }

    @Override public void setPin(final String pin) {
        Log.d(TAG + ".setPin()", "setPin");
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) ft.remove(prev);
        ft.commit();
        if(pin == null) return;
        if(sendDataTask != null) sendDataTask.cancel(true);
        sendDataTask = new SendDataTask(pin);
        sendDataTask.execute(contextVS.getAccessControl().getUserCSRServiceURL());
    }

    public void showProgress(boolean shown, boolean animate) {
        if (progressVisible == shown) return;
        progressVisible = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
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
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
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

        private String password;

        public SendDataTask(String password) {
            this.password = password;
        }

        protected void onPreExecute() {
            Log.d(TAG + ".SendDataTask.onPreExecute(...)", "onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            showProgress(true, true);
        }


        @Override protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".SendDataTask.doInBackground(...)", "url:" + urls[0]);
            byte[] csrBytes = null;
            try {
                String givenName = Normalizer.normalize(
                        givennameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
                givenName  = givenName.replaceAll("[^\\p{ASCII}]", "");
                String surname = Normalizer.normalize(
                        surnameText.getText().toString().toUpperCase(), Normalizer.Form.NFD);
                surname  = surname.replaceAll("[^\\p{ASCII}]", "");
                String nif = NifUtils.validate(nifText.getText().toString().toUpperCase());
                certificationRequest = CertificationRequestVS.getUserRequest(KEY_SIZE, SIG_NAME,
                        SIGNATURE_ALGORITHM, PROVIDER, nif, email, phone, deviceId, givenName, surname);
                csrBytes = certificationRequest.getCsrPEM();
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                X509Certificate[] dummyCerts = CertUtil.generateCertificate(
                        certificationRequest.getKeyPair(), new Date(System.currentTimeMillis()),
                        new Date(System.currentTimeMillis()), "CN=Dummy" + USER_CERT_ALIAS);
                keyStore.setKeyEntry(USER_CERT_ALIAS, certificationRequest.getPrivateKey(),
                        password.toCharArray(), dummyCerts);

                byte[] keyStoreBytes = KeyStoreUtil.getBytes(keyStore, password.toCharArray());
                FileOutputStream fos = getActivity().openFileOutput(KEY_STORE_FILE, Context.MODE_PRIVATE);
                fos.write(keyStoreBytes);
                fos.close();
                return HttpHelper.sendData(csrBytes, null, urls[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".SendDataTask.onPostExecute", "statusCode: " + responseVS.getStatusCode());
            //showProgress(false, true);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                        getActivity().getApplicationContext());
                SharedPreferences.Editor editor = settings.edit();
                Long requestId = Long.valueOf(responseVS.getMessage());
                editor.putLong(CSR_REQUEST_ID_KEY, requestId);
                editor.commit();
                contextVS.setState(State.WITH_CSR);
                Intent intent = new Intent(getActivity().getApplicationContext(),
                        UserCertResponseActivity.class);
                startActivity(intent);
            } else {
                AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.alert_exception_caption).setMessage(
                        responseVS.getMessage()).setPositiveButton("OK", null).show();
            }
        }


    }
}