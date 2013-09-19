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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.sistemavotacion.modelo.Operation;

import java.util.HashMap;
import java.util.Map;


public class JavaScriptInterface {
	
	public static final String TAG = "JavaScriptInterface";
	
	WebActivity webActivity;
    private ProgressDialog progressDialog = null;
    private static Map<String, Operation> sessions = new HashMap<String, Operation>();
    private static Map<String, WebSessionListener> sessionListeners = 
    		new HashMap<String, WebSessionListener>();

    /** Instantiate the interface and set the context */
    JavaScriptInterface(WebActivity webActivity) {
    	this.webActivity = webActivity;
    	showProgressDialog(webActivity.getString(R.string.loading_html_msg));
    }
    
    @JavascriptInterface public void setVotingWebAppMessage (String appMessage) {
		Log.d(TAG + ".setVotingWebAppMessage(...)", " --- appMessage: " + appMessage);
    	try {
			Operation operation = Operation.parse(appMessage);
			if(operation.getCodigoEstado() == Operation.SC_PING) {
				if(progressDialog != null) progressDialog.dismiss();
				webActivity.isPageLoaded(true);
			} else webActivity.processOperation(operation);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    @JavascriptInterface public void showProgressDialog (String appMessage) {
    	progressDialog = new ProgressDialog(webActivity);
    	progressDialog.setMessage(appMessage);
    	progressDialog.setIndeterminate(true);
    	//progressDialog.setCancelable(false);
    	progressDialog.show();
    }
    
    @JavascriptInterface public void setMessage (String appMessage) {
		AlertDialog.Builder builder= new AlertDialog.Builder(webActivity);
		builder.setTitle(webActivity.getString(R.string.error_lbl));
		builder.setMessage(appMessage);
		builder.show();
    }
    

    @JavascriptInterface public void setEditorData (String appMessage) {
    	Log.d(TAG + ".setEditorData(...)", " --- appMessage: " + appMessage);
    	try {
			Operation operation = Operation.parse(appMessage);
			WebSessionListener listener = sessionListeners.get(operation.getSessionId());
			if(listener == null) sessions.put(operation.getSessionId(), operation);
			else  {
				listener.updateEditorData(operation);
				sessionListeners.remove(operation.getSessionId());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void getEditorData(String sessionId, WebSessionListener listener) {
    	Operation sessionData = sessions.get(sessionId);
    	if(sessionData != null) {
    		listener.updateEditorData(sessionData);		
    		sessions.remove(sessionId);
    	} else sessionListeners.put(sessionId, listener);
    }

}
