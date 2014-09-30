package org.votingsystem.android.activity;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.service.VotingAppService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class IntentFilterActivity extends FragmentActivity {
	
	public static final String TAG = IntentFilterActivity.class.getSimpleName();

    private  ProgressDialog progressDialog = null;
    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", "Intent.ACTION_SEARCH - query: " + query);
            return;
        }
        showProgressDialog(getString(R.string.connecting_caption),
                getString(R.string.loading_data_msg));
        AppContextVS contextVS = (AppContextVS) getApplicationContext();

        ResponseVS responseVS = getIntent().getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(responseVS != null) {
            if(progressDialog != null) progressDialog.dismiss();
            showMessage(responseVS.getStatusCode(), responseVS.getCaption(), responseVS.getMessage());
            return;
        }

        Uri uriData = getIntent().getData();
        if(uriData != null) {
            Intent startIntent = new Intent(getApplicationContext(), VotingAppService.class);
            startIntent.putExtra(ContextVS.URI_KEY, uriData);
            startIntent.putExtra(ContextVS.URL_KEY, contextVS.getAccessControlURL());
            startService(startIntent);
        }
        Intent intent = new Intent(this, EventsVSActivity.class);
        startActivity(intent);
        finish();
    }

    private void showProgressDialog(String title, String dialogMessage) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setTitle(title);
        progressDialog.setMessage(dialogMessage);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    };


}