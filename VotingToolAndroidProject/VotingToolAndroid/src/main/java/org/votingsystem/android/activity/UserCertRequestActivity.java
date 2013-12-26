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

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.UserCertRequestFragment;

public class UserCertRequestActivity  extends ActionBarActivity {

	public static final String TAG = "UserCertRequestActivity";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.user_cert_request_activity);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        UserCertRequestFragment requestFragment = new UserCertRequestFragment();
        requestFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, requestFragment).commit();
    }

}