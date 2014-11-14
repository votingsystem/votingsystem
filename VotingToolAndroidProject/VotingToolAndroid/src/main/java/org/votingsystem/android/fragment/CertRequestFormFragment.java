package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertResponseActivity;
import org.votingsystem.android.activity.EventsVSActivity;
import org.votingsystem.android.service.UserCertRequestService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.ResponseVS;

import java.text.Normalizer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.CALLER_KEY;
import static org.votingsystem.model.ContextVS.DEVICE_ID_KEY;
import static org.votingsystem.model.ContextVS.EMAIL_KEY;
import static org.votingsystem.model.ContextVS.NAME_KEY;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.PHONE_KEY;
import static org.votingsystem.model.ContextVS.PIN_KEY;
import static org.votingsystem.model.ContextVS.SURNAME_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertRequestFormFragment extends Fragment {

	public static final String TAG = CertRequestFormFragment.class.getSimpleName();

    private String broadCastId = CertRequestFormFragment.class.getSimpleName();
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String givenname = null;
    private String surname = null;
    private String email = null;
    private String nif = null;
    private String phone = null;
    private String deviceId = null;
    private EditText nifText;
    private EditText givennameText;
    private EditText surnameText;
    private EditText phoneText;
    private EditText mailText;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            String pin = intent.getStringExtra(PIN_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(pin != null) launchUserCertRequestService(pin);
            else {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Intent resultIntent = new Intent(getActivity().getApplicationContext(),
                            CertResponseActivity.class);
                    startActivity(resultIntent);
                } else showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                        responseVS.getMessage());
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView(...)", "progressVisible: ");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.cert_request_form, container, false);
        mainLayout = (FrameLayout)rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        getActivity().setTitle(getString(R.string.request_certificate_form_lbl));
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_lbl);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //finish();
                Intent intent = new Intent(getActivity().getApplicationContext(),
                        EventsVSActivity.class);
                startActivity(intent);
            }
        });
        givennameText = (EditText)rootView.findViewById(R.id.given_name_edit);
        surnameText = (EditText)rootView.findViewById(R.id.surname_edit);
        mailText = (EditText)rootView.findViewById(R.id.mail_edit);
        phoneText = (EditText)rootView.findViewById(R.id.phone_edit);
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
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                //LOGD(TAG + ".onKey(...)", " - keyCode: " + keyCode);
                if (event != null && keyCode == KeyEvent.KEYCODE_ENTER) {
                    submitForm();
                    return true;
                } else return false;
            }
        });
        Button requestButton = (Button) rootView.findViewById(R.id.request_button);
        requestButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        return rootView;
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() {
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(nifText.getWindowToken(), 0);
      	if (validateForm ()) {
    		String nif = NifUtils.validate(nifText.getText().toString().toUpperCase());

			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
            Dialog dialog = builder.setTitle(getString(R.string.request_certificate_form_lbl)).
                    setMessage(Html.fromHtml(getString(R.string.cert_data_confirm_msg, givenname,
                    surname, nif, phone, email))).setPositiveButton(getString(
                    R.string.continue_lbl), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    PinDialogFragment.showPinScreenWithoutHashValidation(getFragmentManager(),
                            broadCastId, getString(R.string.keyguard_password_enter_first_pin_code));
                }
            }).setNegativeButton(getString(R.string.cancel_lbl), null).show();
            //to avoid avoid dissapear on screen orientation change
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    		TextView textView = ((TextView) dialog.findViewById(android.R.id.message));
            textView.setGravity(Gravity.CENTER);
      	}
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        LOGD(TAG + ".showMessage", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    private boolean validateForm () {
    	LOGD(TAG + ".validateForm()", "");
        nif = NifUtils.validate(nifText.getText().toString());
    	if(nif == null) {
    		showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.nif_error));
    		return false;
    	}
    	if(TextUtils.isEmpty(givennameText.getText().toString())){
    		showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.givenname_missing_msg));
    		return false;
    	} else {
            givenname = Normalizer.normalize(givennameText.getText().toString().toUpperCase(),
                    Normalizer.Form.NFD);
            givenname  = givenname.replaceAll("[^\\p{ASCII}]", "");
        }
    	if(TextUtils.isEmpty(surnameText.getText().toString())){
    		showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.surname_missing_msg));
    		return false;
    	} else {
            surname = Normalizer.normalize(surnameText.getText().toString().toUpperCase(),
                    Normalizer.Form.NFD);
            surname = surname.replaceAll("[^\\p{ASCII}]", "");
        }
        if(TextUtils.isEmpty(phoneText.getText().toString())){
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.phone_missing_msg));
            return false;
        } else {
            phone = phoneText.getText().toString();
        }
        if(TextUtils.isEmpty(mailText.getText().toString())){
            showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.mail_missing_msg));
            return false;
        } else {
            email = Normalizer.normalize(mailText.getText().toString(), Normalizer.Form.NFD);
        }
    	TelephonyManager telephonyManager = (TelephonyManager)getActivity().getSystemService(
                Context.TELEPHONY_SERVICE);
    	// phone = telephonyManager.getLine1Number(); -> operator dependent
    	//IMSI
    	//phone = telephonyManager.getSubscriberId();
        //the IMEI for GSM and the MEID or ESN for CDMA phones. Null if device ID is not available.
    	deviceId = telephonyManager.getDeviceId();
    	if(deviceId == null || deviceId.trim().isEmpty()) {
    		deviceId = android.os.Build.SERIAL;
    		if(deviceId == null || deviceId.trim().isEmpty()) {
    			deviceId = UUID.randomUUID().toString();
    		}
    	}
		LOGD(TAG + ".validateForm() ", "deviceId: " + deviceId);
    	return true;
    }

    private void launchUserCertRequestService(String pin) {
        LOGD(TAG + ".launchUserCertRequestService() ", "launchUserCertRequestService");
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                UserCertRequestService.class);
        startIntent.putExtra(PIN_KEY, pin);
        startIntent.putExtra(DEVICE_ID_KEY, deviceId);
        startIntent.putExtra(PHONE_KEY, phone);
        startIntent.putExtra(NAME_KEY, givenname);
        startIntent.putExtra(SURNAME_KEY, surname);
        startIntent.putExtra(NIF_KEY, nif);
        startIntent.putExtra(EMAIL_KEY, email);
        startIntent.putExtra(CALLER_KEY, broadCastId);
        getActivity().startService(startIntent);
        showProgress(true, true);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), android.R.anim.fade_in));
            progressContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

}