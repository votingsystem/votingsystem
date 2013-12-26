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
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventFragment;
import org.votingsystem.android.fragment.VotingEventFragment;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.model.ContextVS.SERVER_URL_EXTRA_PROP_NAME;

//import org.eclipse.jetty.websocket.WebSocket;
//import org.eclipse.jetty.websocket.WebSocketClient;
//import org.eclipse.jetty.websocket.WebSocketClientFactory;
public class MainActivity extends FragmentActivity {
	
	public static final String TAG = "MainActivity";

    private ContextVS contextVS;
    private ProgressDialog progressDialog = null;
    private OperationVS operationVS = null;
    private Uri uriData = null;
    private String accessControlURL = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet); this doesn't work
        Log.i(TAG + ".onCreate(...)", "onCreate");
    	super.onCreate(savedInstanceState);
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            Log.d(TAG + ".onCreate()", "Intent.ACTION_SEARCH - query: " + query);
            return;
        }
        contextVS = ContextVS.getInstance(getBaseContext());
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())) {
        	//getIntent().getCategories().contains(Intent.CATEGORY_BROWSABLE);
            uriData = getIntent().getData();
            accessControlURL = uriData.getQueryParameter("serverURL");
        } else if(getIntent().getStringExtra(SERVER_URL_EXTRA_PROP_NAME) != null) {
            accessControlURL = getIntent().getStringExtra(SERVER_URL_EXTRA_PROP_NAME);
        } else {
            Properties props = new Properties();
            try {
                props.load(getAssets().open("VotingSystem.properties"));
                accessControlURL = props.getProperty("ACCESS_CONTROL_URL");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AccessControlLoader accessControlLoader = new AccessControlLoader();
        accessControlLoader.execute(accessControlURL);
        //WebsocketLoader websocketLoader = new WebsocketLoader();
        //websocketLoader.execute("ws://192.168.1.20:8080/SistemaVotacionTest/websocket/service");
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG +  ".onCreateOptionsMenu(...)", "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.reload:
                if(accessControlURL == null) {
                    try {
                        Properties props = new Properties();
                        props.load(getAssets().open("VotingSystem.properties"));
                        accessControlURL = props.getProperty("ACCESS_CONTROL_URL");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                AccessControlLoader accessControlLoader = new AccessControlLoader();
                accessControlLoader.execute(accessControlURL);
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
        if (progressDialog == null) progressDialog = new ProgressDialog(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage(dialogMessage);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void setActivityState(ContextVS.State state) {
    	Log.d(TAG + ".setActivityState()", "state: " + state);
    	Intent intent = null;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        contextVS.setState(state);
    	switch (state) {
	    	case WITHOUT_CSR:
	    		String idAplicacion = settings.getString(APPLICATION_ID_KEY, null);
	    		if (idAplicacion == null || "".equals(idAplicacion)) {
	    			Log.d(TAG + ".setActivityState() ", " - guardando ID aplicación");
	    			idAplicacion = UUID.randomUUID().toString();
	    			SharedPreferences.Editor editor = settings.edit();
	    			editor.putString(APPLICATION_ID_KEY, idAplicacion);
			        editor.commit();
	    		}
	            setContentView(R.layout.main_activity);
	            Button cancelButton = (Button) findViewById(R.id.cancel_button);
	            cancelButton.setOnClickListener(new OnClickListener() {
	                public void onClick(View v) { 
	                	//finish(); 
	                	Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
	                	startActivity(intent);
	                }
	            });
	            
	            Button requestButton = (Button) findViewById(R.id.request_button);
	            requestButton.setOnClickListener(new OnClickListener() {
	                public void onClick(View v) {
                        Intent intent = new Intent(getBaseContext(), UserCertRequestActivity.class);
	                	startActivity(intent);
	                }
	            });
	    		break;
	    	case WITH_CSR:
	    		intent = new Intent(getBaseContext(), UserCertResponseActivity.class);
	    		break;
	    	case WITH_CERTIFICATE:
	    		intent = new Intent(getBaseContext(), NavigationDrawer.class);
	    		break;
    	}
    	if(intent != null) {
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	startActivity(intent);	
    	}
    }
    
    private void processOperation(OperationVS operationVS, ContextVS.State state) {
    	Log.d(TAG + ".processOperation(...)", "operationVS: " +
    			operationVS.getTypeVS() + " - state: " + state);
        contextVS.setEvent(operationVS.getEventVS());
        Intent intent = null;
        if(ContextVS.State.WITH_CERTIFICATE == state) {
    		switch(operationVS.getTypeVS()) {
		        case SEND_SMIME_VOTE:
                    intent = new Intent(MainActivity.this, VotingEventFragment.class);
                    break;
		        case MANIFEST_SIGN:
		        case SMIME_CLAIM_SIGNATURE:
                    intent = new Intent(MainActivity.this, EventFragment.class);
		        	break;
		        default: 
		        	Log.e(TAG + ".processOperation(...)", "unknown operationVS");;
	        }
            if(intent != null) {
                try {
                    intent.putExtra(ContextVS.EVENT_KEY, operationVS.getEventVS().toJSON().toString());
                    startActivity(intent);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
    	} else {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.cert_not_found_caption)).
    			setMessage(R.string.cert_not_found_msg).show();
    		setActivityState(state);
    	}
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "caption: " + caption + "  - showMessage: " + message);
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle(caption).setMessage(message).show();
    }

    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", "onStop");
    };

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", "onDestroy");
    };

    /*public class WebsocketLoader extends AsyncTask<String, String, ResponseVS> {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        public WebsocketLoader() { }

        @Override protected void onPreExecute() {
            Log.d(TAG + ".WebsocketLoader.onPreExecute() ", " - onPreExecute");
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            String websocketURL = urls[0];
            Log.d(TAG + ".WebsocketLoader.doInBackground() ", " - websocketURL: " + websocketURL);
            // Bridge Jetty Logging to Android Logging
            //System.setProperty("org.eclipse.jetty.util.log.class",AndroidLog.class.getName());
            //org.eclipse.jetty.util.log.Log.setLog(new AndroidLog());

            WebSocketClientFactory factory = new WebSocketClientFactory();
            try {
                factory.start();
                WebSocketClient client = factory.newWebSocketClient();
                Log.d(TAG +  "WebSocketClient", " - getMaxTextMessageSize: " + client.getMaxTextMessageSize());


                WebSocket.Connection connection = client.open(new URI(websocketURL), new WebSocket.OnTextMessage() {
                    public void onOpen(Connection connection) {
                        Log.d(TAG +  "WebSocketClient.onOpen(..)", " - onOpen");
                    }

                    public void onClose(int closeCode, String message) {
                        Log.d(TAG +  "WebSocketClient.onClose(..)", " - onClose");
                        countDownLatch.countDown();
                    }

                    public void onMessage(String data) {
                        Log.d(TAG +  "WebSocketClient.onMessage(..)", " - onMessage - data: " + data);
                        publishProgress(data);
                    }
                }).get(300, TimeUnit.SECONDS);
                countDownLatch.await();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            return new ResponseVS(ResponseVS.SC_OK);
        }

        @Override  protected void onProgressUpdate(String... progress) {
            Log.d(TAG +  "WebSocketClient.onProgressUpdate(..)", " - data: " + progress[0]);

        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".EventInfoLoader.onPostExecute() ", " - statusCode: " + responseVS.getStatusCode());
        }
    }*/



    public class EventInfoLoader extends AsyncTask<String, Void, ResponseVS> {

        public EventInfoLoader() { }

        @Override protected void onPreExecute() {
            showProgressDialog(getString(R.string.connecting_caption),
                    getString(R.string.loading_data_msg));
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".EventInfoLoader.doInBackground() ", "eventURL: " + urls[0]);
            return HttpHelper.getData(urls[0], null);
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".EventInfoLoader.onPostExecute() ",
                    "statusCode: " + responseVS.getStatusCode());
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            try {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    EventVS selectedEvent = EventVS.parse(responseVS.getMessage());
                    selectedEvent.setOptionSelected(operationVS.
                            getEventVS().getOptionSelected());
                    operationVS.setEventVS(selectedEvent);
                    contextVS.setEvent(selectedEvent);
                    processOperation(operationVS, contextVS.getState());
                } else showMessage(getString(R.string.error_lbl), responseVS.getMessage());
            } catch(Exception ex) {
                ex.printStackTrace();
                showMessage(getString(R.string.error_lbl), ex.getMessage());
            }
        }
    }


    public class AccessControlLoader extends AsyncTask<String, Void, ResponseVS> {

        private String serviceURL = null;

        public AccessControlLoader() { }

        @Override protected void onPreExecute() {
            showProgressDialog(getString(R.string.connecting_caption),
                    getString(R.string.loading_data_msg));
        }

        @Override protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".AccessControlLoader.doInBackground() ", "serviceURL: " + urls[0]);
            return HttpHelper.getData(AccessControlVS.getServerInfoURL(urls[0]), null);
        }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".AccessControlLoader.onPostExecute() ", "statusCode: " + responseVS.getStatusCode());
            if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
            try {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    AccessControlVS accessControl = AccessControlVS.parse(responseVS.getMessage());
                    contextVS.setAccessControlVS(accessControl);
                    if(uriData == null) setActivityState(contextVS.getState());
                    else {//loaded from web browser session
                        String encodedMsg = uriData.getQueryParameter("msg");
                        String msg = StringUtils.decodeString(encodedMsg);
                        Log.d(TAG + ".onPostExecute(...)", "launched by browser - host: " +
                                uriData.getHost() + " - path: " + uriData.getPath() +
                                " - userInfo: " + uriData.getUserInfo() +
                                " - msg: " + msg);
                        if(msg != null) {
                            operationVS = OperationVS.parse(msg);
                        } else {
                            Log.d(TAG + ".onPostExecute(...)", "msg null");
                            operationVS = new OperationVS();
                        }
                        if(operationVS.getEventVS() != null) {
                            EventInfoLoader getDataTask = new EventInfoLoader();
                            getDataTask.execute(operationVS.getEventVS().getURL());
                        } else {
                            Log.d(TAG + ".onPostExecute(...)", "operationVS: " + operationVS.getTypeVS());
                            if(msg != null) {
                                Intent intent = new Intent(MainActivity.this, EventPublishingActivity.class);
                                intent.putExtra(OperationVS.OPERATION_KEY, msg);
                                startActivity(intent);
                            }
                        }
                    }
                } else showMessage(getString(R.string.error_lbl), responseVS.getMessage());
            } catch(Exception ex) {
                ex.printStackTrace();
                showMessage(getString(R.string.error_lbl), ex.getMessage());
            }
        }
    }
}