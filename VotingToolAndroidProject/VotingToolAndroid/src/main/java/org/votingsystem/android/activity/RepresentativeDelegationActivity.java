package org.votingsystem.android.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.InputFilterMinMax;
import org.votingsystem.util.ObjectUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeDelegationActivity extends ActionBarActivity {
	
	public static final String TAG = "RepresentativeDelegationActivity";

    private View progressContainer;
    private Button acceptButton;
    private CheckBox anonymousCheckBox;
    private CheckBox publicCheckBox;
    private FrameLayout mainLayout;
    private String broadCastId = null;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
            if(pin != null) launchSignAndSendService(pin);
            else {

            }
        }
    };


    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchUserCertRequestService() ", "pin: " + pin);
        try {
            Intent startIntent = new Intent(RepresentativeDelegationActivity.this,
                    SignAndSendService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            //startIntent.putExtra(ContextVS.TYPEVS_KEY, eventVS.getTypeVS());
            startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
            /*if(eventVS.getTypeVS().equals(TypeVS.MANIFEST_EVENT)) {
                startIntent.putExtra(ContextVS.ITEM_ID_KEY, eventVS.getEventVSId());
            } else {
                startIntent.putExtra(ContextVS.URL_KEY,
                        contextVS.getAccessControl().getEventVSClaimCollectorURL());
                startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                        ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED);
                String messageSubject = getActivity().getString(R.string.signature_msg_subject)
                        + eventVS.getSubject();
                startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, messageSubject);
                JSONObject signatureContent = eventVS.getSignatureContentJSON();
                signatureContent.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE);
                startIntent.putExtra(ContextVS.MESSAGE_KEY, signatureContent.toString());
            }*/
            showProgress(true, true);
            //signAndSendButton.setEnabled(false);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        broadCastId = this.getClass().getSimpleName();
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.representative_delegation);
        mainLayout = (FrameLayout)findViewById(R.id.mainLayout);
        mainLayout.getForeground().setAlpha(0);
        progressContainer = findViewById(R.id.progressContainer);
        acceptButton = (Button) findViewById(R.id.accept_button);
        anonymousCheckBox = (CheckBox) findViewById(R.id.anonymous_delegation_checkbox);
        publicCheckBox = (CheckBox) findViewById(R.id.anonymous_delegation_checkbox);

        EditText et = (EditText) findViewById(R.id.weeks_delegation);
        et.setFilters(new InputFilter[]{ new InputFilterMinMax("1", "52")});

        String editorFileName = "delegation_message_" + Locale.getDefault().getLanguage().toLowerCase() + ".html";
        try {
            if(!Arrays.asList(getResources().getAssets().list("")).contains(editorFileName)) {
                Log.d(TAG + ".loadEditor(...)", "missing editorFileName: " + editorFileName);
                editorFileName = "delegation_message_es.html";
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        WebView webView = (WebView)findViewById(R.id.representative_description);
        webView.loadUrl("file:///android_asset/" + editorFileName);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.representative_delegation_lbl));
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(this,
                        android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }


    public void onCheckboxClicked(View view) {
        switch(view.getId()) {
            case R.id.anonymous_delegation_checkbox:
                publicCheckBox.setChecked(false);
                break;
            case R.id.public_delegation_checkbox:
                anonymousCheckBox.setChecked(false);
                break;
        }
        if(anonymousCheckBox.isChecked() || publicCheckBox.isChecked()) {
            acceptButton.setEnabled(true);
        } else acceptButton.setEnabled(false);
    }

    public void onButtonClicked(View view) {
        switch(view.getId()) {
            case R.id.cancel_button:
                onBackPressed();
                break;
            case R.id.accept_button:

                break;
        }
    }


    private void sendResult(int result, String message) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ContextVS.MESSAGE_KEY, message);
        setResult(result, resultIntent);
        finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
    }
}