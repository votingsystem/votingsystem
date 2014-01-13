package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.WindowManager;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MessageActivity extends FragmentActivity {
	
	public static final String TAG = "MessageActivity";

    private ProgressDialog progressDialog = null;
    private String accessControlURL = null;
    private AlertDialog alertDialog;

    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        Log.i(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_fragment_container_activity);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", "Intent.ACTION_SEARCH - query: " + query);
            return;
        }
        /*((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);*/
        int responseStatusCode = getIntent().getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                ResponseVS.SC_ERROR);
        String caption = getIntent().getStringExtra(ContextVS.CAPTION_KEY);
        String serviceCaller = getIntent().getStringExtra(ContextVS.CALLER_KEY);
        String message = getIntent().getStringExtra(ContextVS.MESSAGE_KEY);
        TypeVS typeVS = (TypeVS) getIntent().getSerializableExtra(ContextVS.TYPEVS_KEY);
        int iconKey = getIntent().getIntExtra(ContextVS.ICON_KEY, -1);
        Log.d(TAG + ".onCreate(...)", "responseStatusCode: " + responseStatusCode +
                " - type: " + typeVS + " - caption: " + caption +
                " - message: " + message + " - serviceCaller: " + serviceCaller +
                " - iconKey: " + iconKey);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(caption).
                setMessage(Html.fromHtml(message)).setPositiveButton(getString(R.string.ok_button),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        onBackPressed();
                        finish();
                    }
                }).show();
        if(iconKey > 0) dialog.setIcon(iconKey);
        dialog.show();
        //to avoid avoid dissapear on screen orientation change
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

}