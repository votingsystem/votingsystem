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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import org.votingsystem.android.R;

public class AppWithoutCertActivity extends FragmentActivity {
	
	public static final String TAG = "AppWithoutCertActivity";

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG + ".onCreate(...)", "");
    	super.onCreate(savedInstanceState);
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
                Intent intent = new Intent(getBaseContext(), UserCertRequestActivity.class);
                startActivity(intent);
            }
        });
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