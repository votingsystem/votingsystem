package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.MessageContentProvider;
import org.votingsystem.android.util.CooinBundle;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.android.util.WebSocketMessage;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageFragment extends Fragment {

    public static final String TAG = MessageFragment.class.getSimpleName();

    private WeakReference<CooinFragment> cooinRef;
    private AppContextVS contextVS;
    private WebSocketMessage socketMessage;
    private TypeVS typeVS;
    private CooinBundle cooinBundle;
    private MessageContentProvider.State messageState;
    private Long messageId;
    private String broadCastId;
    private boolean isVisibleToUser = false;
    private Cursor cursor;

    public static Fragment newInstance(int cursorPosition) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        TypeVS typeVS = (TypeVS)intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(intent.getStringExtra(ContextVS.PIN_KEY) != null) {
            switch(responseVS.getTypeVS()) {
                case COOIN:
                    try {
                        List<Cooin> cooinList = Wallet.getCooinList((String) responseVS.getData(),
                                (AppContextVS) getActivity().getApplicationContext());
                        if(cooinList != null) updateWallet();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                    }
                    break;
            }
        }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        View rootView = inflater.inflate(R.layout.message_fragment, container, false);
        LinearLayout message_data_container = (LinearLayout)rootView.findViewById(R.id.message_data_container);
        TextView message_content = (TextView)rootView.findViewById(R.id.message_content);
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        cursor = getActivity().getContentResolver().query(
                MessageContentProvider.CONTENT_URI, null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        Long createdMillis = cursor.getLong(cursor.getColumnIndex(
                MessageContentProvider.TIMESTAMP_CREATED_COL));
        String dateInfoStr = DateUtils.getDayWeekDateStr(new Date(createdMillis));
        ((TextView)rootView.findViewById(R.id.date)).setText(dateInfoStr);
        try {
            JSONObject decryptedJSON = new JSONObject(cursor.getString(
                    cursor.getColumnIndex(MessageContentProvider.JSON_COL)));
            socketMessage = new WebSocketMessage(decryptedJSON);
            messageId = cursor.getLong(cursor.getColumnIndex(MessageContentProvider.ID_COL));
            typeVS =  TypeVS.valueOf(cursor.getString(cursor.getColumnIndex(
                    MessageContentProvider.TYPE_COL)));
            switch (typeVS) {
                case MESSAGEVS:
                     if(isVisibleToUser) ((ActionBarActivity)getActivity()).getSupportActionBar().
                             setTitle(getString(R.string.message_lbl) +
                            " - " + socketMessage.getFrom());
                    message_content.setText(socketMessage.getMessage());
                    break;
                case COOIN_WALLET_CHANGE:
                    if(isVisibleToUser) ((ActionBarActivity)getActivity()).getSupportActionBar().
                            setTitle(getString(R.string.wallet_lbl));
                    cooinRef = new WeakReference<CooinFragment>(new CooinFragment());
                    Bundle args = new Bundle();
                    args.putSerializable(ContextVS.COOIN_KEY, socketMessage.getCooinList().iterator().next());
                    cooinRef.get().setArguments(args);
                    message_data_container.setVisibility(View.GONE);
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, cooinRef.get(),
                            CooinFragment.class.getSimpleName()).commit();
                    /*cooinBundle = CooinBundle.load(socketMessage.getCooinList());
                    message_content.setText(cooinBundle.getAmount().toPlainString() + " " +
                            cooinBundle.getCurrencyCode());*/
                    break;
            }
            messageState =  MessageContentProvider.State.valueOf(cursor.getString(
                    cursor.getColumnIndex(MessageContentProvider.STATE_COL)));
        } catch(Exception ex) { ex.printStackTrace(); }
        broadCastId = MessageFragment.class.getSimpleName() + "_" + cursorPosition;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments() + " - isVisibleToUser: " + isVisibleToUser);
        if (isVisibleToUser) {
            if(messageState == MessageContentProvider.State.NOT_READED) {
                getActivity().getContentResolver().update(MessageContentProvider.getMessageURI(
                        messageId), MessageContentProvider.getContentValues(socketMessage,
                        MessageContentProvider.State.READED), null, null);
                PrefUtils.addNumMessagesNotReaded(contextVS, -1);
            }
        }
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void setUserVisibleHint(boolean isVisibleToUser) {
        LOGD(TAG + ".setUserVisibleHint", "setUserVisibleHint: " + isVisibleToUser +
                " - messageState: " + messageState);
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        if(isVisibleToUser && typeVS != null) {
            switch (typeVS) {//to avoid problems with the pager
                case MESSAGEVS:
                    ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(
                            R.string.message_lbl) + " - " + socketMessage.getFrom());
                    break;
                case COOIN_WALLET_CHANGE:
                    ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(
                            R.string.wallet_lbl));
                    break;
            }
        }
    }

    @Override public void onPause() {
        super.onPause();
        if(cursor != null && !cursor.isClosed()) cursor.close();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected message type:" + socketMessage.getTypeVS());
        menuInflater.inflate(R.menu.message_fragment, menu);
        switch (socketMessage.getOperation()) {
            case MESSAGEVS:
                menu.setGroupVisible(R.id.message_items, true);
                break;
            case COOIN_WALLET_CHANGE:
                menu.setGroupVisible(R.id.cooin_change_items, true);
                break;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                break;
            case R.id.delete_message:
                getActivity().getContentResolver().delete(MessageContentProvider.getMessageURI(
                        messageId), null, null);
                getActivity().onBackPressed();
                return true;
            case R.id.save_to_wallet:
                updateWallet();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWallet() {
        LOGD(TAG + ".updateWallet", "updateWallet");
        if(Wallet.getCooinList() == null) {
            PinDialogFragment.showWalletScreen(getFragmentManager(), broadCastId,
                    getString(R.string.enter_wallet_pin_msg), false, TypeVS.COOIN);
        } else {
            try {
                ResponseVS responseVS = Wallet.updateWallet(cooinBundle.getCooinSet(), contextVS);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                            getString(R.string.error_lbl), responseVS.getMessage(),
                            getFragmentManager());
                } else {
                    String msg = getString(R.string.save_to_wallet_ok_msg, cooinBundle.getAmount().toString() + " " +
                            cooinBundle.getCurrencyCode()) + " " + getString(R.string.for_lbl)  + " " +
                            MsgUtils.getTagVSMessage(cooinBundle.getTagVS(), contextVS);
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                            getString(R.string.save_to_wallet_lbl), msg, getActivity());
                    builder.setPositiveButton(getString(R.string.accept_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().onBackPressed();
                                }
                            });
                    UIUtils.showMessageDialog(builder);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

}