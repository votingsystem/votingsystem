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

package org.sistemavotacion.android;

import android.app.ProgressDialog;
import android.content.Context;
import android.webkit.WebView;
import android.widget.Toast;


public class JavaScriptInterface {
	
	public static final String TAG = "JavaScriptInterface";
	
    Context mContext;
    WebView webView; 
    private ProgressDialog progressDialog = null;

    /** Instantiate the interface and set the context */
    JavaScriptInterface(Context c) {
        mContext = c;
    }
    
    public void setAppMessage (String appMessage) {
    	Toast.makeText(mContext, appMessage, Toast.LENGTH_SHORT).show();
    }

  
}
