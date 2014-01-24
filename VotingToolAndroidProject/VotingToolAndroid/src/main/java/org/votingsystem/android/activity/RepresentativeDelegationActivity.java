package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.DownloadReceiptDialogFragment;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.InputFilterMinMax;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class RepresentativeDelegationActivity extends ActionBarActivity {
	
	public static final String TAG = "RepresentativeDelegationActivity";

    public static final String ANONYMOUS_SELECTED_KEY  = "ANONYMOUS_SELECTED_KEY";
    public static final String PUBLIC_SELECTED_KEY     = "PUBLIC_SELECTED_KEY";

    private View progressContainer;
    private TypeVS operationType;
    private Button acceptButton;
    private CheckBox anonymousCheckBox;
    private CheckBox publicCheckBox;
    private EditText weeks_delegation;
    private FrameLayout mainLayout;
    private AppContextVS contextVS = null;
    private String broadCastId = null;
    private UserVS representative = null;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private Date anonymousDelegationFromDate;
    private Date anonymousDelegationToDate;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                "intent.getExtras(): " + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        TypeVS typeVS = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        if(typeVS == null && responseVS != null) typeVS = responseVS.getTypeVS();
        if(pin != null) launchSignAndSendService(pin);
        else {
            if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                try {
                    DownloadReceiptDialogFragment newFragment = DownloadReceiptDialogFragment.newInstance(
                            responseVS.getStatusCode(), getString(R.string.error_lbl),
                            responseVS.getNotificationMessage(), (String) responseVS.getData(),
                            typeVS);
                    newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
                    showProgress(false, true);
                    return;
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                    responseVS.getNotificationMessage());
        }
        }
    };

    private void launchSignAndSendService(String pin) {
        Log.d(TAG + ".launchUserCertRequestService() ", "pin: " + pin);
        try {
            Intent startIntent = null;
            String serviceURL = null;
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION == operationType) {
                serviceURL = contextVS.getAccessControl().
                        getAnonymousDelegationRequestServiceURL();
                startIntent = new Intent(this, RepresentativeService.class);
                Map dataData = new HashMap();
                startIntent.putExtra(ContextVS.USER_KEY, representative);
                startIntent.putExtra(ContextVS.TIME_KEY, weeks_delegation.getText().toString());
            } else {
                serviceURL = contextVS.getAccessControl().
                        getRepresentativeDelegationServiceURL();
                startIntent = new Intent(this, SignAndSendService.class);
                Map signatureDataMap = new HashMap();
                signatureDataMap.put("operation", operationType.toString());
                signatureDataMap.put("UUID", UUID.randomUUID().toString());
                signatureDataMap.put("accessControlURL", contextVS.getAccessControl().getServerURL());
                signatureDataMap.put("representativeNif", representative.getNif());
                signatureDataMap.put("representativeName", representative.getFullName());
                JSONObject signatureContent = new JSONObject(signatureDataMap);
                startIntent.putExtra(ContextVS.MESSAGE_KEY, signatureContent.toString());
            }

            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URL_KEY, serviceURL);
            String messageSubject = getString(R.string.representative_delegation_lbl);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, messageSubject);
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED);


            startIntent.putExtra(ContextVS.USER_KEY, representative);
            showProgress(true, true);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        broadCastId = this.getClass().getSimpleName();
    	super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        representative = (UserVS) getIntent().getSerializableExtra(ContextVS.USER_KEY);
        setContentView(R.layout.representative_delegation);
        mainLayout = (FrameLayout)findViewById(R.id.mainLayout);
        mainLayout.getForeground().setAlpha(0);
        progressContainer = findViewById(R.id.progressContainer);
        acceptButton = (Button) findViewById(R.id.accept_button);
        anonymousCheckBox = (CheckBox) findViewById(R.id.anonymous_delegation_checkbox);
        publicCheckBox = (CheckBox) findViewById(R.id.public_delegation_checkbox);
        weeks_delegation = (EditText)findViewById(R.id.weeks_delegation);
        EditText et = (EditText) findViewById(R.id.weeks_delegation);
        et.setFilters(new InputFilter[]{
                new InputFilterMinMax(1, ContextVS.MAX_WEEKS_ANONYMOUS_DELEGATION)});

        String editorFileName = "delegation_message_" +
                Locale.getDefault().getLanguage().toLowerCase() + ".html";
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
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                showProgress(false, true);
            }
        });
        showProgress(true, true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.representative_delegation_lbl));
        if(savedInstanceState != null) {
            operationType = (TypeVS) savedInstanceState.getSerializable(ContextVS.TYPEVS_KEY);
            int selectedCheckBoxId = -1;
            if(savedInstanceState.getBoolean(ANONYMOUS_SELECTED_KEY, false)) {
                selectedCheckBoxId = R.id.anonymous_delegation_checkbox;
                anonymousCheckBox.setChecked(true);
            } else if(savedInstanceState.getBoolean(PUBLIC_SELECTED_KEY, false))  {
                selectedCheckBoxId = R.id.public_delegation_checkbox;
                publicCheckBox.setChecked(true);
            }
            if(selectedCheckBoxId > 0) onCheckboxClicked(selectedCheckBoxId);
        }
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        showProgress(false, true);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            ((LinearLayout)findViewById(R.id.form_layout)).setVisibility(View.GONE);
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
            ((LinearLayout)findViewById(R.id.form_layout)).setVisibility(View.VISIBLE);
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
        onCheckboxClicked(view.getId());
    }

    public void onCheckboxClicked(int selectedBoxId) {
        switch(selectedBoxId) {
            case R.id.anonymous_delegation_checkbox:
                publicCheckBox.setChecked(false);
                break;
            case R.id.public_delegation_checkbox:
                anonymousCheckBox.setChecked(false);
                break;
        }
        if(anonymousCheckBox.isChecked())
            ((LinearLayout)findViewById(R.id.weeks_delegation_layout)).setVisibility(View.VISIBLE);
        else ((LinearLayout)findViewById(R.id.weeks_delegation_layout)).setVisibility(View.GONE);
        if(anonymousCheckBox.isChecked() || publicCheckBox.isChecked()) {
            acceptButton.setEnabled(true);
        } else acceptButton.setEnabled(false);
        Log.d(TAG + ".onCheckboxClicked(...) ", "anonymousCheckBox.isChecked(): " + anonymousCheckBox.isChecked() +
                " - publicCheckBox.isChecked(): " + publicCheckBox.isChecked());
    }

    public void onButtonClicked(View view) {
        switch(view.getId()) {
            case R.id.cancel_button:
                onBackPressed();
                break;
            case R.id.accept_button:
                String confirmDialogMsg = null;
                if(anonymousCheckBox.isChecked()) {
                    if(TextUtils.isEmpty(weeks_delegation.getText())) {
                        showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                                getString(R.string.anonymous_delegation_time_msg));
                        ((EditText)findViewById(R.id.weeks_delegation)).requestFocus();
                        return;
                    }
                    Calendar calendar = DateUtils.getNextMonday(Calendar.getInstance().getTime());
                    anonymousDelegationFromDate = calendar.getTime();
                    Integer weeksDelegation = Integer.valueOf(weeks_delegation.getText().toString());
                    calendar.add(Calendar.DAY_OF_YEAR, weeksDelegation*7);
                    anonymousDelegationToDate = calendar.getTime();
                    confirmDialogMsg = getString(R.string.anonymous_delegation_confirm_msg,
                            representative.getFullName(),  weeks_delegation.getText().toString(),
                            DateUtils.getLongDate_Es(anonymousDelegationFromDate),
                            DateUtils.getLongDate_Es(anonymousDelegationToDate));
                    operationType = TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION;
                }  else {
                    confirmDialogMsg = getString(R.string.public_delegation_confirm_msg,
                             representative.getFullName());
                    operationType = TypeVS.REPRESENTATIVE_SELECTION;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.representative_delegation_lbl)).
                        setMessage(Html.fromHtml(confirmDialogMsg)).setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                PinDialogFragment.showPinScreen(getSupportFragmentManager(),
                                        broadCastId, null, false, null);
                            }
                        }).setNegativeButton(getString(R.string.cancel_lbl), null);
                AlertDialog dialog = builder.show();
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
        outState.putBoolean(ANONYMOUS_SELECTED_KEY, anonymousCheckBox.isChecked());
        outState.putBoolean(PUBLIC_SELECTED_KEY, publicCheckBox.isChecked());
        outState.putSerializable(ContextVS.TYPEVS_KEY, operationType);
    }

}