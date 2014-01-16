package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MessageActivity extends FragmentActivity {
	
	public static final String TAG = "MessageActivity";


    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        Log.i(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        Log.i(TAG + ".onCreate(...)", "savedInstanceState -getIntent().getExtras(): " +
                getIntent().getExtras());
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_fragment_container_activity);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", "Intent.ACTION_SEARCH - query: " + query);
            return;
        }
        /*((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                ContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);*/
        ResponseVS responseVS = getIntent().getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(responseVS.getCaption());
        if(responseVS.getMessage() != null) builder.setMessage(Html.fromHtml(responseVS.getMessage())).
                setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                onBackPressed();
                                finish();
                            }
                        });
        if(responseVS.getIconId() > 0) builder.setIcon(responseVS.getIconId());
        AlertDialog dialog = builder.show();
    }


}