package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.fragment.CertRequestFormFragment;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertRequestActivity extends ActionBarActivity {
	
	public static final String TAG = CertRequestActivity.class.getSimpleName();

    private AppContextVS appContextVS;

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        appContextVS = getApplicationContext();
        LOGD(TAG + ".onCreate", "appContextVS.getState(): " + appContextVS.getState() +
                " - savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.cert_request_advice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        Button cancelButton = (Button) findViewById(R.id.cancel_lbl);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), EventVSMainActivity.class);
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        switch(appContextVS.getState()) {
            case WITH_CSR:
                startActivity(new Intent(this, CertResponseActivity.class));
                break;
            case WITH_CERTIFICATE:
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.request_certificate_menu),
                        getString(R.string.request_cert_again_msg), this).setPositiveButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
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
                            });
                UIUtils.showMessageDialog(builder);
                break;
        }
        if(getIntent().getStringExtra(ContextVS.OPERATIONVS_KEY) != null) {
            cancelButton.setVisibility(View.GONE);
        }
        getSupportActionBar().setTitle(getString(R.string.cert_request_lbl));
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


    @Override public AppContextVS getApplicationContext() {
        return (AppContextVS) super.getApplicationContext();
    }

}