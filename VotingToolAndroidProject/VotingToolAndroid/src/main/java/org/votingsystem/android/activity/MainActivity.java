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

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.service.VotingAppService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.util.Properties;

import static org.votingsystem.model.ContextVS.SERVER_URL_EXTRA_PROP_NAME;

//import org.eclipse.jetty.websocket.WebSocket;
//import org.eclipse.jetty.websocket.WebSocketClient;
//import org.eclipse.jetty.websocket.WebSocketClientFactory;
/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MainActivity extends FragmentActivity {
	
	public static final String TAG = "MainActivity";

    private ContextVS contextVS;
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
        contextVS = ContextVS.getInstance(getBaseContext());
        Uri uriData = null;
        if(Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            //getIntent().getCategories().contains(Intent.CATEGORY_BROWSABLE);
            uriData = getIntent().getData();
            if(uriData != null) Log.d(TAG + ".onCreate(...)", "uriData - host:" + uriData.getHost()+
                    " - path: " + uriData.getPath() + " - userInfo: " + uriData.getUserInfo());
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
        if(savedInstanceState != null && savedInstanceState.getBoolean(
                ContextVS.LOADING_KEY, false)) {
            showProgressDialog(getString(R.string.connecting_caption),
                    getString(R.string.loading_data_msg));
        }
        if (savedInstanceState != null) return;
        String caption = getIntent().getStringExtra(ContextVS.CAPTION_KEY);
        String message = getIntent().getStringExtra(ContextVS.MESSAGE_KEY);
        if(caption != null && message != null) showMessage(null, caption, message);
        else if(uriData != null || contextVS.getAccessControl() == null) runAppService(uriData);
        else if(contextVS.getAccessControl() != null) {
            Intent intent = new Intent(getBaseContext(), NavigationDrawer.class);
            startActivity(intent);
        }
        //WebsocketLoader websocketLoader = new WebsocketLoader();
        //websocketLoader.execute("ws://192.168.1.20:8080/SistemaVotacionTest/websocket/service");
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.MESSAGE_KEY, ContextVS.MESSAGE_KEY);
        if (progressDialog != null && progressDialog.isShowing()) {
            outState.putBoolean(ContextVS.LOADING_KEY, true);
        }
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState: " + outState);
    }

    /*@Override public void onBackPressed() {
        Log.d(TAG +  ".onBackPressed(...)", "");
    }*/

    private void runAppService(Uri uriData) {
        showProgressDialog(getString(R.string.connecting_caption),
                getString(R.string.loading_data_msg));
        Intent startIntent = new Intent(getApplicationContext(), VotingAppService.class);
        if(uriData != null) {
            String encodedMsg = uriData.getQueryParameter("msg");
            String msg = StringUtils.decodeString(encodedMsg);
            startIntent.putExtra(ContextVS.URI_DATA_KEY, msg);
        }
        startIntent.putExtra(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL);
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

}