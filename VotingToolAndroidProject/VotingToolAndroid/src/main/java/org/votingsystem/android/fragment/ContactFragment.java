package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.android.service.WebSocketService;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.WebSocketMessage;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.util.UUID;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactFragment extends Fragment {

	public static final String TAG = ContactFragment.class.getSimpleName();

    private AppContextVS contextVS = null;
    private View rootView;
    private String broadCastId = null;
    private Button toggle_contact_button;
    private UserVS contact;
    private TextView connected_text;
    private Menu menu;
    private ResponseVS contactDataResponse;
    private boolean isDBUserVS;
    private boolean isConnected = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = (ResponseVS) intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        WebSocketMessage socketMsg = intent.getParcelableExtra(ContextVS.WEBSOCKET_MSG_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case WEB_SOCKET_INIT:
                    setProgressDialogVisible(true, getString(R.string.connecting_caption),
                            getString(R.string.connecting_to_service_msg));
                    Utils.toggleWebSocketServiceConnection(contextVS);
                    break;
            }
        } else setProgressDialogVisible(false, null, null);
        if(responseVS != null) {
            switch(responseVS.getTypeVS()) {
                case MESSAGEVS:
                    Intent startIntent = new Intent(getActivity(), WebSocketService.class);
                    startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.MESSAGEVS);
                    startIntent.putExtra(ContextVS.MESSAGE_KEY, responseVS.getMessage());
                    startIntent.putExtra(ContextVS.RESPONSEVS_KEY, contactDataResponse.setTypeVS(TypeVS.MESSAGEVS));
                    startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
                    getActivity().startService(startIntent);
                    break;
            }
        }
        if(socketMsg != null) {
            switch(socketMsg.getStatusCode()) {
                case ResponseVS.SC_WS_CONNECTION_NOT_FOUND:
                    MessageDialogFragment.showDialog(socketMsg.getStatusCode(), getString(
                            R.string.error_lbl), getString(R.string.usevs_connection_not_found_error_msg),
                            getFragmentManager());
                    break;
                case ResponseVS.SC_WS_MESSAGE_SEND_OK:
                    MessageDialogFragment.showDialog(socketMsg.getStatusCode(), getString(
                            R.string.send_message_lbl), getString(R.string.messagevs_send_ok_msg),
                            getFragmentManager());
                    break;
                case ResponseVS.SC_WS_CONNECTION_INIT_OK:
                    break;
                default:
                    MessageDialogFragment.showDialog(socketMsg.getStatusCode(), getString(
                            R.string.error_lbl), socketMsg.getMessage(), getFragmentManager());
            }
        }
        }
    };

    public static Fragment newInstance(Long contactId) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putLong(ContextVS.CURSOR_POSITION_KEY, contactId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(UserVS userVS) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putSerializable(ContextVS.USER_KEY, userVS);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        rootView = inflater.inflate(R.layout.contact, container, false);
        toggle_contact_button = (Button) rootView.findViewById(R.id.toggle_contact_button);
        connected_text = (TextView) rootView.findViewById(R.id.connected_text);
        Long contactId =  getArguments().getLong(ContextVS.CURSOR_POSITION_KEY, -1);
        boolean isDBUserVS = false;
        if(contactId > 0) {
            contact = UserContentProvider.loadUser(contactId, getActivity());
            if(contact != null) isDBUserVS = true;
        } else {
            contact =  (UserVS) getArguments().getSerializable(ContextVS.USER_KEY);
            UserVS contactDB = UserContentProvider.loadUser(contact, getActivity());
            if(contactDB != null) {
                contact = contactDB;
                isDBUserVS = true;
            }
        }
        setContactButtonState(isDBUserVS);
        setHasOptionsMenu(true);
        broadCastId = ContactFragment.class.getSimpleName() + "_" + (contactId != null? contactId:
                UUID.randomUUID().toString());
        if(savedInstanceState != null) {
            contactDataResponse = savedInstanceState.getParcelable(ContextVS.RESPONSEVS_KEY);
            isConnected = savedInstanceState.getBoolean(ContextVS.CONNECTED_KEY);
        }
        if(contactDataResponse == null) new UserVSDataFetcher().execute("");
        return rootView;
    }

    private void deleteContact() {
        getActivity().getContentResolver().delete(UserContentProvider.CONTENT_URI,
                UserContentProvider.CONTACT_URI_COL + " = ?" ,
                new String[]{contact.getContactURI().toString()});
        setContactButtonState(false);
    }

    private void addContact() {
        getActivity().getContentResolver().insert(UserContentProvider.CONTENT_URI,
                UserContentProvider.getContentValues(contact));
        setContactButtonState(true);
    }

    private void setContactButtonState(boolean isDBUserVS) {
        this.isDBUserVS = isDBUserVS;
        if(isDBUserVS) toggle_contact_button.setVisibility(View.GONE);
        else {
            toggle_contact_button.setVisibility(View.VISIBLE);
            toggle_contact_button.setText(getString(R.string.add_contact_lbl));
            toggle_contact_button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { addContact(); }
            });
        }
        if(menu != null) {
            menu.removeItem(R.id.send_message); //to avoid duplicated items
            menu.removeItem(R.id.delete_item);
            if(isConnected) menu.add(R.id.general_items, R.id.send_message, 1,
                        getString(R.string.send_message_lbl));
            if(isDBUserVS) menu.add(R.id.general_items, R.id.delete_item, 3,
                    getString(R.string.remove_contact_lbl));
        }
        if(isConnected) connected_text.setText(getString(R.string.user_connected_lbl, contact.getName()));
        else connected_text.setText(getString(R.string.uservs_disconnected_lbl, contact.getName()));
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        String message = null;
        if(data != null) message = data.getStringExtra(ContextVS.MESSAGE_KEY);
        if(Activity.RESULT_OK == requestCode) {
            MessageDialogFragment.showDialog(ResponseVS.SC_OK, getString(R.string.operation_ok_msg),
                    message, getFragmentManager());
        } else if(message != null) MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                    getString(R.string.operation_error_msg), message, getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.loading_data_msg);
            if(message == null) message = getString(R.string.loading_info_msg);
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contact, menu);
        this.menu = menu;
        setContactButtonState(isDBUserVS);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.send_message:
                if(!contextVS.hasWebSocketConnection()) {
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                            getString(R.string.send_message_lbl),
                            getString(R.string.connection_required_msg),
                            getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    PinDialogFragment.showPinScreen(getFragmentManager(),
                                            broadCastId, getString(R.string.init_authenticated_session_pin_msg),
                                            false, TypeVS.WEB_SOCKET_INIT);
                                }
                            }).setNegativeButton(getString(R.string.cancel_lbl), null);
                    UIUtils.showMessageDialog(builder);
                } else MessageVSInputDialogFragment.showDialog(getString(R.string.messagevs_caption),
                        broadCastId, TypeVS.MESSAGEVS, getFragmentManager());
                return true;
            case R.id.send_money:
                return true;
            case R.id.delete_item:
                deleteContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(contactDataResponse != null) outState.putParcelable(ContextVS.RESPONSEVS_KEY,
                contactDataResponse);
        outState.putBoolean(ContextVS.CONNECTED_KEY, isConnected);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(ContextVS.WEB_SOCKET_BROADCAST_ID));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    public class UserVSDataFetcher extends AsyncTask<String, String, ResponseVS> {

        public UserVSDataFetcher() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true); }

        @Override protected ResponseVS doInBackground(String... params) {
            contactDataResponse = HttpHelper.getData(contextVS.getCooinServer().
                    getDeviceVSConnectedServiceURL(contextVS.getUserVS().getNif()), ContentTypeVS.JSON);
            return contactDataResponse;
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseVS responseVS) {
            try {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    JSONArray deviceArray = ((JSONObject) responseVS.getMessageJSON()).getJSONArray("deviceList");
                    isConnected = false;
                    TelephonyManager telephonyManager = (TelephonyManager)getActivity().
                            getSystemService(Context.TELEPHONY_SERVICE);
                    String deviceId = telephonyManager.getDeviceId();
                    for (int i = 0; i < deviceArray.length(); i++) {
                        DeviceVS deviceVS = DeviceVS.parse((JSONObject) deviceArray.get(i));
                        if(!deviceId.equals(deviceVS.getDeviceId())) {
                            isConnected = true;
                        }
                    }
                    setContactButtonState(isDBUserVS);
                } else MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,getString(R.string.error_lbl),
                            responseVS.getMessage(), getFragmentManager());
                setProgressDialogVisible(false);
                LOGD(TAG + ".UserVSDataFetcher", "isConnected: " + isConnected);
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

}