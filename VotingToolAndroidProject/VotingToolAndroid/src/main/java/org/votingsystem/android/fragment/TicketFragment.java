package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertRequestActivity;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.contentprovider.TicketContentProvider;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TicketVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;

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
    private TicketVS selectedTicket;
    private View progressContainer;
    private FrameLayout mainLayout;
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
        if(savedInstanceState != null) {
            selectedTicket = (TicketVS) savedInstanceState.getSerializable(
                    ContextVS.RECEIPT_KEY);
            initTicketScreen(selectedTicket);
        } else {
            Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                    TicketContentProvider.CONTENT_URI, null, null, null, null);
            cursor.moveToPosition(cursorPosition);
            byte[] serializedTicketContainer = cursor.getBlob(cursor.getColumnIndex(
                    TicketContentProvider.SERIALIZED_OBJECT_COL));
            Long ticketId = cursor.getLong(cursor.getColumnIndex(TicketContentProvider.ID_COL));
            try {
                selectedTicket = (TicketVS) ObjectUtils.deSerializeObject(serializedTicketContainer);
                selectedTicket.setLocalId(ticketId);
                initTicketScreen(selectedTicket);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return rootView;
    }


    private void initTicketScreen (TicketVS ticket) {
        Log.d(TAG + ".initTicketScreen(...)", "type: " + ticket.getTypeVS() + " - messageId: " +
            ticket.getMessageId());
        try {
            selectedTicketSMIME = ticket.getReceipt();
            if(selectedTicket != null) {
                ticketSubject.setText("ID: " + selectedTicket.getLocalId() +
                        " - State: " + selectedTicket.getState() +
                        " - Subject: " + selectedTicket.getSubject());
                if(selectedTicket.getReceipt() != null)
                    ticket_content.setText(Html.fromHtml(getTicketContentFormatted(selectedTicket)));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
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
        Log.d(TAG + ".onCreateOptionsMenu(...) ", " selected ticket type:" + selectedTicket.getTypeVS());
        menuInflater.inflate(R.menu.ticket_fragment, menu);
        try {
            if(selectedTicket.getReceipt() == null) {
                menu.removeItem(R.id.show_timestamp_info);
                menu.removeItem(R.id.share_ticket);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    getActivity().onBackPressed();
                    return true;
                case R.id.cert_info:
                    X509Certificate certificate = selectedTicket.getCertificationRequest().getCertificate();
                    String userCertInfo = getActivity().getString(R.string.cert_info_formated_msg,
                            certificate.getSubjectDN().toString(),
                            certificate.getIssuerDN().toString(),
                            certificate.getSerialNumber().toString(),
                            DateUtils.getDate_Es(certificate.getNotBefore()),
                            DateUtils.getDate_Es(certificate.getNotAfter()));
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.
                            ticket_cert_caption)).setMessage(Html.fromHtml(userCertInfo)).show();
                    break;
                case R.id.show_timestamp_info:
                    TimeStampInfoDialogFragment newFragment = TimeStampInfoDialogFragment.newInstance(
                            selectedTicket.getReceipt().getSigner().getTimeStampToken(),
                            getActivity().getApplicationContext());
                    newFragment.show(getFragmentManager(), TimeStampInfoDialogFragment.TAG);
                    break;
                case R.id.cancel_ticket:

                    break;
                case R.id.share_ticket:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = new String(selectedTicket.getReceipt().getBytes());
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, receiptStr);
                        sendIntent.setType(ContentTypeVS.TEXT.getName());
                        startActivity(sendIntent);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setActionBar() {
        Log.d(TAG + ".setActionBar() ", "");
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
            switch(selectedTicket.getTypeVS()) {
                default:
                    return selectedTicket.getReceipt().getSignedContent();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

}