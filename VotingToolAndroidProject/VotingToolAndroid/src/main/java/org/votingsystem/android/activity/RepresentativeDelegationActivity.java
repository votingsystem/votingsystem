package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.ModalProgressDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.fragment.ReceiptFetcherDialogFragment;
import org.votingsystem.android.service.RepresentativeService;
import org.votingsystem.android.service.SignAndSendService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.InputFilterMinMax;
import org.votingsystem.util.ResponseVS;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RepresentativeDelegationActivity extends ActivityBase {
	
	public static final String TAG = RepresentativeDelegationActivity.class.getSimpleName();

    public static final String ANONYMOUS_SELECTED_KEY  = "ANONYMOUS_SELECTED_KEY";
    public static final String PUBLIC_SELECTED_KEY     = "PUBLIC_SELECTED_KEY";

    private TypeVS operationType;
    private Button acceptButton;
    private CheckBox anonymousCheckBox;
    private CheckBox publicCheckBox;
    private EditText weeks_delegation;
    private AppContextVS contextVS = null;
    private String broadCastId = RepresentativeDelegationActivity.class.getSimpleName();
    private UserVS representative = null;
    private Date anonymousDelegationFromDate;
    private Date anonymousDelegationToDate;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver",
                "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) sendDelegation();
        else {
            if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                try {
                    ReceiptFetcherDialogFragment newFragment = ReceiptFetcherDialogFragment.newInstance(
                            responseVS.getStatusCode(), getString(R.string.error_lbl),
                            responseVS.getNotificationMessage(), (String) responseVS.getData(),
                            responseVS.getTypeVS());
                    newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
                    setProgressDialogVisible(false);
                    return;
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            MessageDialogFragment.showDialog(responseVS.getStatusCode(), responseVS.getCaption(),
                    responseVS.getNotificationMessage(), getSupportFragmentManager());
        }
        }
    };

    private void sendDelegation() {
        LOGD(TAG + ".sendDelegation", "sendDelegation");
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
            startIntent.putExtra(ContextVS.TYPEVS_KEY, operationType);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.URL_KEY, serviceURL);
            String messageSubject = getString(R.string.representative_delegation_lbl);
            startIntent.putExtra(ContextVS.MESSAGE_SUBJECT_KEY, messageSubject);
            startIntent.putExtra(ContextVS.CONTENT_TYPE_KEY,
                    ContentTypeVS.JSON_SIGNED);
            startIntent.putExtra(ContextVS.USER_KEY, representative);
            setProgressDialogVisible(true);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        representative = (UserVS) getIntent().getSerializableExtra(ContextVS.USER_KEY);
        setContentView(R.layout.representative_delegation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);


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
                LOGD(TAG + ".loadEditor", "missing editorFileName: " + editorFileName);
                editorFileName = "delegation_message_es.html";
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        WebView webView = (WebView)findViewById(R.id.representative_description);
        webView.loadUrl("file:///android_asset/" + editorFileName);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                setProgressDialogVisible(false);
            }
        });
        setProgressDialogVisible(true);
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

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ModalProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getSupportFragmentManager());
        } else ModalProgressDialogFragment.hide(getSupportFragmentManager());
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
        LOGD(TAG + ".onCheckboxClicked", "anonymousCheckBox.isChecked(): " + anonymousCheckBox.isChecked() +
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
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), getString(
                                        R.string.anonymous_delegation_time_msg), getSupportFragmentManager());
                        ((EditText)findViewById(R.id.weeks_delegation)).requestFocus();
                        return;
                    }
                    Calendar calendar = DateUtils.getMonday(Calendar.getInstance());
                    anonymousDelegationFromDate = calendar.getTime();
                    Integer weeksDelegation = Integer.valueOf(weeks_delegation.getText().toString());
                    calendar.add(Calendar.DAY_OF_YEAR, weeksDelegation*7);
                    anonymousDelegationToDate = calendar.getTime();
                    confirmDialogMsg = getString(R.string.anonymous_delegation_confirm_msg,
                            representative.getFullName(),  weeks_delegation.getText().toString(),
                            DateUtils.getDayWeekDateStr(anonymousDelegationFromDate),
                            DateUtils.getDayWeekDateStr(anonymousDelegationToDate));
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

    private void sendResult(int result, ResponseVS responseVS) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        setResult(result, resultIntent);
        finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
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
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ANONYMOUS_SELECTED_KEY, anonymousCheckBox.isChecked());
        outState.putBoolean(PUBLIC_SELECTED_KEY, publicCheckBox.isChecked());
        outState.putSerializable(ContextVS.TYPEVS_KEY, operationType);
    }

}