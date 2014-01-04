package org.votingsystem.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.contentprovider.VoteReceiptDBHelper;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.ReceiptOptionsDialog;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

public class VoteReceiptListActivity extends ActionBarActivity {

    public static final String TAG = "VoteReceiptListActivity";

    protected VoteReceiptDBHelper db;
    private List<VoteVS> voteVSList;
    private ReceiptListAdapter adapter;
    private VoteVS vote = null;
    private ContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private ProcessSignatureTask processSignatureTask;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                "intent.getExtras(): " + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        ReceiptOptionsDialog.Operation operation = (ReceiptOptionsDialog.Operation) intent.
                getSerializableExtra(ContextVS.OPERATION_KEY);
        if(pin != null) {
            if(processSignatureTask != null) processSignatureTask.cancel(true);
            processSignatureTask = new ProcessSignatureTask(pin);
            processSignatureTask.execute();
        } else {
            VoteVS vote = (VoteVS)intent.getSerializableExtra(ContextVS.VOTE_KEY);
            if(operation == ReceiptOptionsDialog.Operation.CANCEL_VOTE) {
                cancelVote(vote);
            } else if(operation == ReceiptOptionsDialog.Operation.REMOVE_RECEIPT) {
                removeReceipt(vote);
            }
        }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate ");
        setContentView(R.layout.vote_receipt_list);
        contextVS = ContextVS.getInstance(getBaseContext());
        db = new VoteReceiptDBHelper(this);
        try {
            voteVSList = db.getVoteReceiptList();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Log.d(TAG + ".onCreate(...) ", " - voteVSList.size(): " + voteVSList.size());
        adapter = new ReceiptListAdapter(this);
        if(voteVSList.size() > 0)
            ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.GONE);
        adapter.setData(voteVSList);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.receipt_list_screen_caption));
        getSupportActionBar().setLogo(R.drawable.receipt_32);
        final ListView listView = (ListView) findViewById(R.id.listView);
        OnItemClickListener clickListener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?>  l, View v, int position, long id) {
                VoteVS receipt = ((VoteVS) adapter.getItem(position));
                launchOptionsDialog(receipt);
            }};
        listView.setOnItemClickListener(clickListener);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        mainLayout = (FrameLayout) findViewById( R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        if(savedInstanceState != null && savedInstanceState.getBoolean(
                ContextVS.LOADING_KEY, false)) {
            showProgress(true, true);
        }
    }

    private void launchOptionsDialog(VoteVS vote) {
        String caption = vote.getEventVS().getSubject();
        String msg = getString(R.string.receipt_options_dialog_msg);
        ReceiptOptionsDialog optionsDialog = ReceiptOptionsDialog.newInstance(
                caption, msg, vote, this.getClass().getName());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ReceiptOptionsDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        optionsDialog.show(ft, ReceiptOptionsDialog.TAG);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, false, this.getClass().getName());
        pinDialog.show(getSupportFragmentManager(), PinDialogFragment.TAG);
    }

    private void refreshReceiptList() {
        Log.d(TAG + ".refreshReceiptList(...)", "refreshReceiptList");
        try {
            voteVSList = db.getVoteReceiptList();
            if(voteVSList.size() == 0)
                ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.VISIBLE);
            else ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.GONE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        dismissOptionsDialog();
        adapter.setData(voteVSList);
        adapter.notifyDataSetChanged();
        adapter.setNotifyOnChange (true);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, NavigationDrawer.class);
                startActivity(intent);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vote_receipt_list, menu);
        return true;
    }

    private class VoteReceiptLisAdapter extends ArrayAdapter<VoteVS> {

        Context context;
        List<VoteVS> voteVSList = new ArrayList<VoteVS>();
        int layoutResourceId;

        public VoteReceiptLisAdapter(Context context, int layoutResourceId,
                                     List<VoteVS> receipts) {
            super(context, layoutResourceId, receipts);
            this.layoutResourceId = layoutResourceId;
            this.voteVSList = receipts;
            this.context = context;
        }

        /**
         * This method will DEFINe what the view inside the list view will
         * finally look like Here we are going to code that the checkbox state
         * is the status of task and check box text is the task name
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            VoteVS voteVS = voteVSList.get(position);
            Log.d(TAG + "VoteReceiptLisAdapter", "voteVS: " +  String.valueOf(voteVS.getId()));
            return convertView;
        }

    }

    public static class ReceiptListAdapter extends ArrayAdapter<VoteVS> {

        private final LayoutInflater mInflater;

        public ReceiptListAdapter(Context context) {
            super(context, R.layout.row_eventvs);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<VoteVS> data) {
            clear();
            if (data != null) {
                for (VoteVS receipt : data) {
                    add(receipt);
                }
            }
        }

        /**
         * Populate new items in the list.
         */
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.row_receipt, parent, false);
            } else {
                view = convertView;
            }
            VoteVS voteVS = getItem(position);
            if (voteVS != null) {
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.event_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                TextView author = (TextView) view.findViewById(R.id.event_author);
                TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);

                subject.setText(voteVS.getEventVS().getSubject());
                String dateInfoStr = null;
                ImageView imgView = (ImageView)view.findViewById(R.id.event_state_icon);
                if(DateUtils.getTodayDate().after(voteVS.getEventVS().getDateFinish())) {
                    imgView.setImageResource(R.drawable.closed);
                    dateInfoStr = "<b>" + getContext().getString(R.string.closed_upper_lbl) + "</b> - " +
                            "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " +
                            DateUtils.getShortSpanishStringFromDate(
                                    voteVS.getEventVS().getDateBegin()) + " - " +
                            "<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " +
                            DateUtils.getShortSpanishStringFromDate(
                                    voteVS.getEventVS().getDateFinish());
                } else {
                    imgView.setImageResource(R.drawable.open);
                    dateInfoStr = "<b>" + getContext().getString(R.string.remain_lbl, DateUtils.
                            getElpasedTimeHoursFromNow(voteVS.getEventVS().getDateFinish()))  +"</b>";
                }
                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(voteVS.isCanceled()) {
                    Log.d(TAG + ".ReceiptListAdapter.getView(...)", "voteVS: " + voteVS.getId()
                            + " - position: " + position + " - isCanceled");
                    receiptState.setText(getContext().getString(R.string.vote_canceled_receipt_lbl));
                    receiptState.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - voteVS: " + voteVS.getId()
                            + " -position: " + position);
                    receiptState.setVisibility(View.GONE);
                }
                if(voteVS.getEventVS().getUserVS() != null && !"".equals(
                        voteVS.getEventVS().getUserVS().getFullName())) {
                    String authorStr =  "<b>" + getContext().getString(R.string.author_lbl) + "</b>: " +
                            voteVS.getEventVS().getUserVS().getFullName();
                    author.setText(Html.fromHtml(authorStr));
                } else author.setVisibility(View.GONE);
            }
            return view;
        }
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    private void cancelVote(VoteVS receipt) {
        Log.d(TAG + ".cancelVote(...)", " - cancelVote");
        vote = receipt;
        if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
            Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
            showCertNotFoundDialog();
        } else {
            showPinScreen(getString(R.string.cancel_vote_msg));
        }
    }

    private void dismissOptionsDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment prev = (DialogFragment) getSupportFragmentManager().findFragmentByTag(
                ReceiptOptionsDialog.TAG);
        if(prev != null) {
            prev.getDialog().dismiss();
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
        }
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
        Log.d(TAG + ".onResume() ", "onResume");
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    private void removeReceipt(VoteVS receipt) {
        Log.d(TAG + ".removeReceipt()", " --- receipt: " + receipt.getId());
        try {
            db.deleteVoteReceipt(receipt);
            refreshReceiptList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState: " + outState);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", "onDestroy");
        if(processSignatureTask != null) processSignatureTask.cancel(true);
    };

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", "onStop");
        if(processSignatureTask != null) processSignatureTask.cancel(true);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(), android.R.anim.fade_in));
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
                        getApplicationContext(), android.R.anim.fade_out));
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

    private class ProcessSignatureTask extends AsyncTask<URL, Integer, ResponseVS> {

        private String pin = null;

        public ProcessSignatureTask(String pin) {
            this.pin = pin;
        }

        protected ResponseVS doInBackground(URL... urls) {
            Log.d(TAG + ".ProcessSignatureTask.doInBackground(...)", " - doInBackground " );
            String subject = getString(R.string.cancel_vote_msg_subject);
            String serverURL = contextVS.getAccessControl().getAccessServiceURL();
            try {
                FileInputStream fis = openFileInput(KEY_STORE_FILE);
                byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, pin.toCharArray());
                PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, pin.toCharArray());
                X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(USER_CERT_ALIAS);

                byte[] base64encodedvoteCertPrivateKey = Encryptor.decryptMessage(
                        vote.getEncryptedKey(), signerCert, signerPrivatekey);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                        Base64.decode(base64encodedvoteCertPrivateKey));
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey certPrivKey = kf.generatePrivate(keySpec);
                vote.setCertVotePrivateKey(certPrivKey);
                JSONObject cancelDataJSON = new JSONObject(vote.getCancelVoteDataMap());
                String serviceURL =contextVS.getAccessControl().getCancelVoteServiceURL();
                SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                        cancelDataJSON.toString(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, subject,
                        keyStoreBytes, pin.toCharArray(),
                        contextVS.getAccessControl().getCertificate(), getBaseContext());
                return smimeSignedSender.call();
            } catch(Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
            }
        }

        protected void onPreExecute() {
            Log.d(TAG + ".ProcessSignatureTask.onPreExecute(...)", " --- onPreExecute");
            getWindow().getDecorView().findViewById(
                    android.R.id.content).invalidate();
            showProgress(true, true);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", " - onPostExecute - status:" +
                    responseVS.getStatusCode());
            showProgress(false, true);
            String caption  = null;
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                vote.setCancelVoteReceipt(responseVS.getSmimeMessage());
                String msg = getString(R.string.cancel_vote_result_msg,
                        vote.getEventVS().getSubject());
                try {
                    db.updateVoteReceipt(vote);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                refreshReceiptList();
                showMessage(responseVS.getStatusCode(), getString(R.string.msg_lbl), msg);
            } else {
                caption = getString(R.string.error_lbl) + " "
                        + responseVS.getStatusCode();
                if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                    Log.e(TAG + ".setSignServiceMsg(...)", "CANCEL REPEATED");
                    vote.setCanceled(true);
                    try {
                        db.updateVoteReceipt(vote);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showMessage(responseVS.getStatusCode(), caption, responseVS.getMessage());
            }
        }

    }


}