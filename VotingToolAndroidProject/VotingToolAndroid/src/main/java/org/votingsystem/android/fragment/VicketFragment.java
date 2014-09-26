package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.ActivityVS;
import org.votingsystem.android.contentprovider.VicketContentProvider;
import org.votingsystem.android.service.VicketService;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketFragment extends Fragment {

    public static final String TAG = VicketFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private Vicket selectedVicket;
    private TextView vicketSubject;
    private TextView vicket_content;
    private TextView vicket_cancellation_date;

    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private SMIMEMessageWrapper selectedVicketSMIME;
    private String broadCastId = null;
    private String userCertInfo;
    private Button cancel_button;
    private int cursorPosition;


    public static Fragment newInstance(int cursorPosition) {
        VicketFragment fragment = new VicketFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                    "intent.getExtras(): " + intent.getExtras());
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
            if(intent.getStringExtra(ContextVS.PIN_KEY) != null) launchVicketCancellation();
            else {
                ((ActivityVS)getActivity()).showProgress(false, true);
                ((ActivityVS)getActivity()).showMessage(responseVS.getStatusCode(), responseVS.getCaption(),
                        responseVS.getNotificationMessage());
            }
        }
    };

    private void launchVicketCancellation() {
        Log.d(TAG + ".launchVicketRequest(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    VicketService.class);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.VICKET_CANCEL);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            startIntent.putExtra(ContextVS.ITEM_ID_KEY, cursorPosition);
            ((ActivityVS)getActivity()).showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        cursorPosition =  getArguments().getInt(ContextVS.CURSOR_POSITION_KEY);
        broadCastId = VicketFragment.class.getSimpleName() + "_" + cursorPosition;
        Log.d(TAG + ".onCreateView(...)", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.vicket_fragment, container, false);
        LinearLayout vicketDataContainer = (LinearLayout) rootView.
                findViewById(R.id.vicket_data_container);
        vicket_content = (TextView)rootView.findViewById(R.id.vicket_content);
        vicket_cancellation_date = (TextView)rootView.findViewById(R.id.vicket_cancellation_date);
        vicket_content.setMovementMethod(LinkMovementMethod.getInstance());
        vicketSubject = (TextView)rootView.findViewById(R.id.vicket_subject);
        cancel_button = (Button)rootView.findViewById(R.id.cancel_button);
        setHasOptionsMenu(true);
        if(savedInstanceState != null) {
            selectedVicket = (Vicket) savedInstanceState.getSerializable(ContextVS.RECEIPT_KEY);
            initVicketScreen(selectedVicket);
        } else {
            Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                    VicketContentProvider.CONTENT_URI, null, null, null, null);
            cursor.moveToPosition(cursorPosition);
            byte[] serializedVicket = cursor.getBlob(cursor.getColumnIndex(
                    VicketContentProvider.SERIALIZED_OBJECT_COL));
            Long vicketId = cursor.getLong(cursor.getColumnIndex(VicketContentProvider.ID_COL));
            try {
                selectedVicket = (Vicket) ObjectUtils.deSerializeObject(serializedVicket);
                selectedVicket.setLocalId(vicketId);
                initVicketScreen(selectedVicket);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return rootView;
    }

    private void initVicketScreen(Vicket vicket) {
        Log.d(TAG + ".initVicketScreen(...)", "type: " + vicket.getTypeVS() + " - messageId: " +
            vicket.getMessageId());
        try {
            selectedVicketSMIME = vicket.getReceipt();
            X509Certificate certificate = selectedVicket.getCertificationRequest().getCertificate();
            userCertInfo = getActivity().getString(R.string.cert_info_formated_msg,
                    certificate.getSubjectDN().toString(),
                    certificate.getIssuerDN().toString(),
                    certificate.getSerialNumber().toString(),
                    DateUtils.getLongDate_Es(certificate.getNotBefore()),
                    DateUtils.getLongDate_Es(certificate.getNotAfter()));
            if(selectedVicket != null) {
                vicketSubject.setText("ID: " + selectedVicket.getLocalId() +
                        " - State: " + selectedVicket.getState());
                if(selectedVicket.getReceipt() != null)
                    vicket_content.setText(Html.fromHtml(getVicketContentFormatted(selectedVicket)));
                else vicket_content.setText(Html.fromHtml(userCertInfo));
            }
            if(Vicket.State.OK == selectedVicket.getState()) {
                cancel_button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                                getString(R.string.cancel_vicket_dialog_msg), false, null);
                    }
                });
                cancel_button.setVisibility(View.VISIBLE);
            } else if(Vicket.State.CANCELLED == selectedVicket.getState()) {
                vicket_cancellation_date.setText(getString(R.string.cancellation_date_lbl,
                        DateUtils.getLongDate_Es(selectedVicket.getCancellationDate())));
                vicket_cancellation_date.setVisibility(View.VISIBLE);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedVicket != null) outState.putSerializable(ContextVS.RECEIPT_KEY, selectedVicket);
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
        Log.d(TAG + ".onCreateOptionsMenu(...) ", " selected model type:" + selectedVicket.getTypeVS());
        menuInflater.inflate(R.menu.vicket_fragment, menu);
        try {
            if(selectedVicket.getReceipt() == null) {
                menu.removeItem(R.id.show_timestamp_info);
                menu.removeItem(R.id.share_vicket);
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
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.
                            vicket_cert_caption)).setMessage(Html.fromHtml(userCertInfo)).show();
                    break;
                case R.id.show_timestamp_info:
                    TimeStampInfoDialogFragment newFragment = TimeStampInfoDialogFragment.newInstance(
                            selectedVicket.getReceipt().getSigner().getTimeStampToken(),
                            (AppContextVS) getActivity().getApplicationContext());
                    newFragment.show(getFragmentManager(), TimeStampInfoDialogFragment.TAG);
                    break;
                case R.id.cancel_vicket:
                    PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                            getString(R.string.cancel_vicket_dialog_msg), false, null);
                    break;
                case R.id.share_vicket:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = new String(selectedVicket.getReceipt().getBytes());
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

    public class VicketDownloader extends AsyncTask<String, String, ResponseVS> {

        public VicketDownloader() { }

        @Override protected void onPreExecute() {((ActivityVS)getActivity()).showProgress(true, true); }

        @Override protected ResponseVS doInBackground(String... urls) {
            String vicketURL = urls[0];
            return HttpHelper.getData(vicketURL, null);
        }

        @Override  protected void onProgressUpdate(String... progress) { }

        @Override  protected void onPostExecute(ResponseVS responseVS) {
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    selectedVicket.setReceiptBytes(responseVS.getMessageBytes());
                    initVicketScreen(selectedVicket);
                    setActionBar();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ((ActivityVS)getActivity()).showMessage(ResponseVS.SC_ERROR, getString(R.string.exception_lbl),
                            ex.getMessage());
                }
            } else {
                ((ActivityVS)getActivity()).showMessage(ResponseVS.SC_ERROR, getString(R.string.error_lbl),
                        responseVS.getMessage());
            }
            ((ActivityVS)getActivity()).showProgress(false, true);
        }
    }


    public String getVicketContentFormatted(ReceiptContainer selectedVicket) {
        String result = null;
        try {
            switch(selectedVicket.getTypeVS()) {
                default:
                    return selectedVicket.getReceipt().getSignedContent();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

}