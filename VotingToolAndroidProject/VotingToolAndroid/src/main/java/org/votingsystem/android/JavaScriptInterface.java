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

package org.votingsystem.android;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import org.votingsystem.android.model.OperationVS;


public class JavaScriptInterface {
	
	public static final String TAG = "JavaScriptInterface";

    private FragmentActivity hostActivity;

    JavaScriptInterface(FragmentActivity hostActivity) {
    	this.hostActivity = hostActivity;
    }
    
    @JavascriptInterface public void setVotingWebAppMessage (String appMessage) {
		Log.d(TAG + ".setVotingWebAppMessage(...)", " --- appMessage: " + appMessage);
    	try {
			OperationVS operationVS = OperationVS.parse(appMessage);
            ((EventPublishingActivity)hostActivity).processOperation(operationVS);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

}