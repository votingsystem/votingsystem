package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.service.VotingAppService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import java.io.IOException;
import java.util.Properties;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MainActivity extends FragmentActivity {
	
	public static final String TAG = MainActivity.class.getSimpleName();

    private AppContextVS contextVS;
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
        contextVS = (AppContextVS) getApplicationContext();
        Properties props = new Properties();
        try {
            props.load(getAssets().open("VotingSystem.properties"));
            accessControlURL = props.getProperty(ContextVS.ACCESS_CONTROL_URL_KEY);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Uri uriData = getIntent().getData();
        if(uriData != null) runAppService(uriData);
        if(savedInstanceState != null && savedInstanceState.getBoolean(
                ContextVS.LOADING_KEY, false)) {
            showProgressDialog(getString(R.string.connecting_caption),
                    getString(R.string.loading_data_msg));
        }
        if (savedInstanceState != null) return;
        ResponseVS responseVS = getIntent().getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(contextVS.getAccessControl() == null && !contextVS.isInitialized()) runAppService(uriData);
        if(responseVS != null) showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                responseVS.getNotificationMessage());
        else if(contextVS.getAccessControl() != null) {
            Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
            startActivity(intent);
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.MESSAGE_KEY, ContextVS.MESSAGE_KEY);
        if (progressDialog != null && progressDialog.isShowing()) {
            outState.putBoolean(ContextVS.LOADING_KEY, true);
        }
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState: " + outState);
    }

    private void runAppService(Uri uriData) {
        showProgressDialog(getString(R.string.connecting_caption),
                getString(R.string.loading_data_msg));
        Intent startIntent = new Intent(getApplicationContext(), VotingAppService.class);
        if(uriData != null) startIntent.putExtra(ContextVS.URI_KEY, uriData);
        startIntent.putExtra(ContextVS.URL_KEY, accessControlURL);
        startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
        startService(startIntent);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG +  ".onCreateOptionsMenu(...)", "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.reload:
                if(contextVS.getAccessControl() == null) runAppService(null);
                else {
                    Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
                    startActivity(intent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", "onResume");
    }

    private void showProgressDialog(String title, String dialogMessage) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(true);
            progressDialog.setTitle(title);
            progressDialog.setMessage(dialogMessage);
            progressDialog.setIndeterminate(true);
            /*progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
                @Override public void onCancel(DialogInterface dialog){}});*/
        }
        progressDialog.show();
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", "onStop");
    };

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", "onDestroy");
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
    };

}