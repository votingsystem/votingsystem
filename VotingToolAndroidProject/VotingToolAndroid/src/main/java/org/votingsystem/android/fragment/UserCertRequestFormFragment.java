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

import org.votingsystem.android.R;
import org.votingsystem.android.activity.NavigationDrawer;
import org.votingsystem.android.activity.UserCertResponseActivity;
import org.votingsystem.android.service.UserCertRequestService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.NifUtils;

import java.text.Normalizer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class UserCertRequestFormFragment extends Fragment {

	public static final String TAG = "UserCertRequestFormFragment";

    private String givenname = null;
    private String surname = null;
    private String email = null;
    private String nif = null;
    private String phone = null;
    private String deviceId = null;
    private EditText nifText;
    private EditText givennameText;
    private EditText surnameText;
    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            if(pin != null) launchUserCertRequestService(pin);
            else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                if(ResponseVS.SC_OK == responseStatusCode) {
                    Intent resultIntent = new Intent(getActivity().getApplicationContext(),
                            UserCertResponseActivity.class);
                    startActivity(resultIntent);
                } else showMessage(responseStatusCode, caption, message);
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateView(...)", "progressVisible: " + progressVisible);
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
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                //Log.d(TAG + ".onKey(...)", " - keyCode: " + keyCode);
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
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        progressMessage.setText(R.string.loading_data_msg);
        mainLayout.getForeground().setAlpha(0);
        if(progressVisible.get()) showProgress(true, true);
        return rootView;
    }


    @Override public void onStart() {
    	Log.d(TAG + ".onStart(...) ", "onStart");
    	super.onStart();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
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
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
        Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
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

    private void submitForm() {
    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
  		imm.hideSoftInputFromWindow(nifText.getWindowToken(), 0);
      	if (validateForm ()) {
    		String nif = NifUtils.validate(nifText.getText().toString().toUpperCase());

			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
            Dialog dialog = builder.setTitle(getString(R.string.request_certificate_form_lbl)).
                    setMessage(Html.fromHtml(getString(R.string.cert_data_confirm_msg, givenname,
                    surname, nif))).setPositiveButton(getString(
                    R.string.continue_label), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    showPinScreen(getString(
                            R.string.keyguard_password_enter_first_pin_code));
                }
            }).setNegativeButton(getString(R.string.cancel_button), null).show();
    		TextView textView = ((TextView) dialog.findViewById(android.R.id.message));
            textView.setGravity(Gravity.CENTER);
      	}
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    private boolean validateForm () {
    	Log.d(TAG + ".validateForm", "validateForm");
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

    private void launchUserCertRequestService(String pin) {
        Log.d(TAG + ".launchUserCertRequestService() ", "pin: " + pin);
        Intent startIntent = new Intent(getActivity().getApplicationContext(),
                UserCertRequestService.class);
        startIntent.putExtra(ContextVS.PIN_KEY, pin);
        startIntent.putExtra(ContextVS.DEVICE_ID_KEY, deviceId);
        startIntent.putExtra(ContextVS.PHONE_KEY, phone);
        startIntent.putExtra(ContextVS.NAME_KEY, givenname);
        startIntent.putExtra(ContextVS.SURNAME_KEY, surname);
        startIntent.putExtra(ContextVS.NIF_KEY, nif);
        startIntent.putExtra(ContextVS.EMAIL_KEY, email);
        startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
        getActivity().startService(startIntent);
        showProgress(true, true);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, false, this.getClass().getName());
        pinDialog.show(getFragmentManager(), PinDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_in));
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
                    getActivity().getApplicationContext(), android.R.anim.fade_out));
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