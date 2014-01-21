package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.TicketContentProvider;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketFragment extends Fragment {

    public static final String TAG = "TicketFragment";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(pin != null) {

            } else {
                int responseStatusCode = intent.getIntExtra(ContextVS.RESPONSE_STATUS_KEY,
                        ResponseVS.SC_ERROR);
                String caption = intent.getStringExtra(ContextVS.CAPTION_KEY);
                String message = intent.getStringExtra(ContextVS.MESSAGE_KEY);
                TypeVS resultOperation = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
                if(resultOperation == TypeVS.CANCEL_VOTE){
                    if(ResponseVS.SC_OK == responseStatusCode) { }
                    getActivity().onBackPressed();
                }
                showProgress(false, true);
                showMessage(responseStatusCode, caption, message);
            }
        }
    };


    private AppContextVS contextVS;
    private ReceiptContainer selectedTicket;
    private View progressContainer;
    private FrameLayout mainLayout;
    private Menu menu;
    private TextView ticketSubject;
    private TextView ticket_content;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private SMIMEMessageWrapper selectedTicketSMIME;
    private String broadCastId = null;


    public static Fragment newInstance(int cursorPosition) {
        TicketFragment fragment = new TicketFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(String ticketURL, TypeVS type) {
        TicketFragment fragment = new TicketFragment();
        Bundle args = new Bundle();
        args.putSerializable(ContextVS.TYPEVS_KEY, type);
        args.putString(ContextVS.URL_KEY, ticketURL);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        broadCastId = this.getClass().getSimpleName() + "_" + cursorPosition;
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.ticket_fragment, container, false);
        LinearLayout ticketDataContainer = (LinearLayout) rootView.
                findViewById(R.id.ticket_data_container);
        ticket_content = (TextView)rootView.findViewById(R.id.ticket_content);
        ticket_content.setMovementMethod(LinkMovementMethod.getInstance());
        ticketSubject = (TextView)rootView.findViewById(R.id.ticket_subject);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TypeVS type = (TypeVS) getArguments().getSerializable(ContextVS.TYPEVS_KEY);
        String ticketURL = getArguments().getString(ContextVS.URL_KEY);
        if(savedInstanceState != null) {
            selectedTicket = (ReceiptContainer) savedInstanceState.getSerializable(
                    ContextVS.RECEIPT_KEY);
            initTicketScreen(selectedTicket);
        } else {
            if(ticketURL != null) {
                selectedTicket = new ReceiptContainer(type, ticketURL);
                String selection = TicketContentProvider.URL_COL + "=? ";
                String[] selectionArgs = new String[]{ticketURL};
                Cursor cursor = getActivity().getContentResolver().query(
                        TicketContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
                if(cursor.getCount() > 0 ) {
                    cursor.moveToFirst();
                    byte[] serializedTicketContainer = cursor.getBlob(cursor.getColumnIndex(
                            TicketContentProvider.SERIALIZED_OBJECT_COL));
                    Long ticketId = cursor.getLong(cursor.getColumnIndex(TicketContentProvider.ID_COL));
                    try {
                        selectedTicket = (ReceiptContainer) ObjectUtils.
                                deSerializeObject(serializedTicketContainer);
                        selectedTicket.setLocalId(ticketId);
                        initTicketScreen(selectedTicket);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    TicketDownloader getDataTask = new TicketDownloader();
                    getDataTask.execute(ticketURL);
                }
            } else {
                int cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
                Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                        TicketContentProvider.CONTENT_URI, null, null, null, null);
                cursor.moveToPosition(cursorPosition);
                byte[] serializedTicketContainer = cursor.getBlob(cursor.getColumnIndex(
                        TicketContentProvider.SERIALIZED_OBJECT_COL));
                Long ticketId = cursor.getLong(cursor.getColumnIndex(TicketContentProvider.ID_COL));
                try {
                    selectedTicket = (ReceiptContainer) ObjectUtils.
                            deSerializeObject(serializedTicketContainer);
                    selectedTicket.setLocalId(ticketId);
                    initTicketScreen(selectedTicket);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initTicketScreen (ReceiptContainer ticket) {
        Log.d(TAG + ".initTicketScreen(...)", "type: " + ticket.getType() + " - messageId: " +
            ticket.getMessageId());
        try {
            selectedTicketSMIME = ticket.getReceipt();
            ticketSubject.setText(getString(R.string.smime_subject_msg, selectedTicket.getSubject()));
            ticket_content.setText(Html.fromHtml(getTicketContentFormatted(selectedTicket)));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setActionBar() {
        if(selectedTicket == null) return;
        switch(selectedTicket.getType()) {

        }
        if(selectedTicket.getLocalId() < 0) {
            menu.removeItem(R.id.delete_ticket);
        } else menu.removeItem(R.id.save_ticket);
        if(getActivity() instanceof ActionBarActivity) {
            ((ActionBarActivity)getActivity()).setTitle(getString(R.string.ticket_lbl));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(
                    selectedTicket.getTypeDescription(getActivity()));
            ((ActionBarActivity)getActivity()).getSupportActionBar().setLogo(R.drawable.euro_32);
        }
    }

    @Override public void onStart() {
        Log.d(TAG + ".onStart(...) ", "");
        super.onStart();
    }


    @Override public void onDestroy() {
        Log.d(TAG + ".onDestroy()", "");
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedTicket != null) outState.putSerializable(ContextVS.RECEIPT_KEY, selectedTicket);
    }

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "");
        super.onStop();
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "onResume");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Log.d(TAG + ".onCreateOptionsMenu(...) ", " selected ticket type:" +
                selectedTicket.getType());
        menuInflater.inflate(R.menu.ticket_fragment, menu);
        this.menu = menu;
        setActionBar();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        AlertDialog dialog = null;
        switch (item.getItemId()) {

        }
        return super.onOptionsItemSelected(item);
    }

    private void showMessage(Integer statusCode,String caption,String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    public class TicketDownloader extends AsyncTask<String, String, ResponseVS> {

        public TicketDownloader() { }

        @Override protected void onPreExecute() {showProgress(true, true); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String ticketURL = urls[0];
            return HttpHelper.getData(ticketURL, null);
        }

        @Override  protected void onProgressUpdate(String... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    selectedTicket.setReceiptBytes(responseVS.getMessageBytes());
                    initTicketScreen(selectedTicket);
                    setActionBar();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showMessage(ResponseVS.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage());
                }
            } else {
                showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage());
            }
            showProgress(false, true);
        }
    }


    public String getTicketContentFormatted(ReceiptContainer selectedTicket) {
        String result = null;
        try {
            switch(selectedTicket.getType()) {
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

}