package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.CertRequestFormFragment;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertRequestActivity extends FragmentActivity {
	
	public static final String TAG = CertRequestActivity.class.getSimpleName();

    private AppContextVS appContextVS;

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        appContextVS = getApplicationContext();
        Log.d(TAG + ".onCreate(...)", "appContextVS.getState(): " + appContextVS.getState() +
                " - savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.cert_request_advice);
        Button cancelButton = (Button) findViewById(R.id.cancel_lbl);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), EventsVSActivity.class);
                startActivity(intent);
            }
        });

        Button requestButton = (Button) findViewById(R.id.request_button);
        requestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                intent.putExtra(FRAGMENT_KEY, CertRequestFormFragment.class.getName());
                startActivity(intent);
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
        switch(appContextVS.getState()) {
            case WITH_CSR:
                startActivity(new Intent(this, CertResponseActivity.class));
                break;
            case WITH_CERTIFICATE:
                AlertDialog dialog= new AlertDialog.Builder(this).setTitle(getString(R.string.
                        request_certificate_menu)).setMessage(Html.fromHtml(
                        getString(R.string.request_cert_again_msg))).setPositiveButton(getString(
                        R.string.ok_lbl), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(CertRequestActivity.this,
                                FragmentContainerActivity.class);
                        intent.putExtra(FRAGMENT_KEY, CertRequestFormFragment.class.getName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).setNegativeButton(getString(R.string.cancel_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                CertRequestActivity.super.onBackPressed();
                            }
                }).show();
                //to avoid avoid dissapear on screen orientation change
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                break;
        }
        if(getIntent().getStringExtra(ContextVS.OPERATIONVS_KEY) != null) {
            cancelButton.setVisibility(View.GONE);
        }
        getActionBar().setLogo(UIUtils.getLogoIcon(this, R.drawable.signature_ok_22));
        getActionBar().setTitle(getString(R.string.cert_request_lbl));
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


    @Override public AppContextVS getApplicationContext() {
        return (AppContextVS) super.getApplicationContext();
    }

}