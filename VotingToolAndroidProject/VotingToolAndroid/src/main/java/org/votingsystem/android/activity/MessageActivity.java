package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageActivity extends ActionBarActivity {
	
	public static final String TAG = MessageActivity.class.getSimpleName();


    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        LOGD(TAG + ".onCreate", "savedInstanceState -getIntent().getExtras(): " +
                getIntent().getExtras());
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container_activity);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            LOGD(TAG + ".onCreate()", "Intent.ACTION_SEARCH - query: " + query);
            return;
        }
        /*((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                AppContextVS.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);*/
        View view = getLayoutInflater().inflate(R.layout.message_activity, null);
        TypeVS typeVS = (TypeVS) getIntent().getSerializableExtra(ContextVS.TYPEVS_KEY);
        String broadCastId = getIntent().getStringExtra(ContextVS.CALLER_KEY);
        if(typeVS != null) {
            switch(typeVS) {
                case PIN_CHANGE:
                    PinDialogFragment.showChangePinScreen(getSupportFragmentManager());
                    return;
            }
        }
        ResponseVS responseVS = getIntent().getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        ((TextView) view.findViewById(R.id.caption_text)).setText(responseVS.getCaption());
        ((TextView) view.findViewById(R.id.message_text)).setText(Html.fromHtml(
                responseVS.getNotificationMessage()));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(view);
        builder.setPositiveButton(getString(R.string.accept_lbl),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        onBackPressed();
                        finish();
                    }
                });
        builder.show();
    }


}