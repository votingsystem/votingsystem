/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.votingsystem.android.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.UserCertRequestFormFragment;
import org.votingsystem.model.ContextVS;

public class CertRequestActivity extends FragmentActivity {
	
	public static final String TAG = "CertRequestActivity";

    ContextVS contextVS;

    @Override protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getApplicationContext());
        Log.d(TAG + ".onCreate(...)", "contextVS.getState(): " + contextVS.getState() +
                "savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.app_without_cert_activity);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
                startActivity(intent);
            }
        });

        Button requestButton = (Button) findViewById(R.id.request_button);
        requestButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                intent.putExtra(ContextVS.FRAGMENT_KEY, UserCertRequestFormFragment.class.getName());
                startActivity(intent);
            }
        });

        switch(contextVS.getState()) {
            case WITH_CSR:
                startActivity(new Intent(this, UserCertResponseActivity.class));
                break;
            case WITH_CERTIFICATE:
                AlertDialog.Builder builder= new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.
                        request_certificate_menu));
                builder.setMessage(Html.fromHtml(
                        getString(R.string.request_cert_again_msg)));
                builder.setPositiveButton(getString(
                        R.string.ok_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(CertRequestActivity.this,
                                FragmentContainerActivity.class);
                        intent.putExtra(ContextVS.FRAGMENT_KEY,
                                UserCertRequestFormFragment.class.getName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                CertRequestActivity.super.onBackPressed();
                            }
                });
                builder.show();
                break;
        }
    }

    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", "onStop");
    };

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", "onDestroy");
    };

}