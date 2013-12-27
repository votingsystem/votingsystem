package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.Html;
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
import org.votingsystem.android.service.SignAndSendService;
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
public class RepresentativeFragment extends Fragment implements CertPinDialogListener {

	public static final String TAG = "UserCertRequestFragment";
    public static final String REPRESENTATIVE_ID_KEY = "REPRESENTATIVE_ID_KEY";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    " ========= intent.getExtras(): " + intent.getExtras());
            String action = intent.getAction();
            if(action.equalsIgnoreCase(ContextVS.SIGN_AND_SEND_ACTION_ID)){

            }
        }
    };

    private ContextVS contextVS;
    private TextView progressMessage;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ContextVS.SIGN_AND_SEND_ACTION_ID);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, intentFilter);

        View rootView = inflater.inflate(R.layout.representative_fragment, container, false);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        getActivity().setTitle(ContextVS.getMessage("representativeCaption"));

        EditText nifText = (EditText)rootView.findViewById(R.id.nif_edit);



        Button selectButton = (Button) rootView.findViewById(R.id.select_representative_button);
        selectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showPinScreen(null);
            }
        });
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        progressMessage = (TextView)rootView.findViewById(R.id.progressMessage);
        progressMessage.setText(R.string.loading_data_msg);
        mainLayout.getForeground().setAlpha(0);
        isProgressShown = false;
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
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
    }


    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
    }

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
	    		Log.d(TAG + ".onOptionsItemSelected(...) ", "home");
	    		Intent intent = new Intent(getActivity(), NavigationDrawer.class);
	    		startActivity(intent);
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}


	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", "caption: " + caption + "  - showMessage: " + message);
		AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
		builder.setTitle(caption).setMessage(message).show();
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
        Intent signAndSendServiceIntent = new Intent(getActivity(), SignAndSendService.class);
        signAndSendServiceIntent.putExtra("pin", pin);
        getActivity().startService(signAndSendServiceIntent);
        showProgress(true, true);
    }

    public void showProgress(boolean shown, boolean animate) {
        if (isProgressShown == shown) return;
        isProgressShown = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_out));
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
                        getActivity(), android.R.anim.fade_in));
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

}