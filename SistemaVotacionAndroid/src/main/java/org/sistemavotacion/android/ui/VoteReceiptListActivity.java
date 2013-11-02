package org.sistemavotacion.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.android.NavigationDrawer;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import static org.sistemavotacion.android.AppData.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.AppData.KEY_STORE_FILE;

public class VoteReceiptListActivity extends ActionBarActivity
        implements CertPinDialogListener, ReceiptOperationsListener {

    public static final String TAG = "VoteReceiptListActivity";


    private static final String OPTIONS_DIALOG_ID = "optionsDialog";
    protected VoteReceiptDBHelper db;
    private List<VoteReceipt> voteReceiptList;
    private ReceiptListAdapter adapter;
    private VoteReceipt operationReceipt = null;
    private AppData appData;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;
    private ProcessSignatureTask processSignatureTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", " - onCreate ");
        setContentView(R.layout.vote_receipt_list);
        appData = AppData.getInstance(getBaseContext());
        db = new VoteReceiptDBHelper(this);
        try {
            voteReceiptList = db.getVoteReceiptList();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Log.d(TAG + ".onCreate(...) ", " - voteReceiptList.size(): " + voteReceiptList.size());
        adapter = new ReceiptListAdapter(this);
        if(voteReceiptList.size() > 0)
            ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.GONE);
        adapter.setData(voteReceiptList);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.receipt_list_screen_caption));
        getSupportActionBar().setLogo(R.drawable.receipt_32);
        final ListView listView = (ListView) findViewById(R.id.listView);
        OnItemClickListener clickListener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?>  l, View v, int position, long id) {
                VoteReceipt receipt = ((VoteReceipt) adapter.getItem(position));
                launchOptionsDialog(receipt);
            }};
        listView.setOnItemClickListener(clickListener);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        mainLayout = (FrameLayout) findViewById( R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha( 0);
        isProgressShown = false;
        isDestroyed = false;
    }


    private void launchOptionsDialog(VoteReceipt receipt) {
        String caption = receipt.getVoto().getAsunto();
        String msg = getString(R.string.receipt_options_dialog_msg);
        ReceiptOptionsDialog optionsDialog = ReceiptOptionsDialog.newInstance(
                caption, msg, receipt, this);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(OPTIONS_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        optionsDialog.show(ft, OPTIONS_DIALOG_ID);
    }


    private void showPinScreen(String message) {
        CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        pinDialog.show(ft, "pinDialog");
    }

    private void refreshReceiptList() {
        Log.d(TAG + ".refreshReceiptList(...)", " --- refreshReceiptList");
        try {
            voteReceiptList = db.getVoteReceiptList();
            if(voteReceiptList.size() == 0)
                ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.VISIBLE);
            else ((TextView)findViewById(R.id.emptyListMsg)).setVisibility(View.GONE);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        dismissOptionsDialog();
        adapter.setData(voteReceiptList);
        adapter.notifyDataSetChanged();
        adapter.setNotifyOnChange (true);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG + ".onOptionsItemSelected(...) ", " - home - ");
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

    private class VoteReceiptLisAdapter extends ArrayAdapter<VoteReceipt> {

        Context context;
        List<VoteReceipt> voteReceiptList = new ArrayList<VoteReceipt>();
        int layoutResourceId;

        public VoteReceiptLisAdapter(Context context, int layoutResourceId,
                                     List<VoteReceipt> receipts) {
            super(context, layoutResourceId, receipts);
            this.layoutResourceId = layoutResourceId;
            this.voteReceiptList = receipts;
            this.context = context;
        }

        /**
         * This method will DEFINe what the view inside the list view will
         * finally look like Here we are going to code that the checkbox state
         * is the status of task and check box text is the task name
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            VoteReceipt voteReceipt = voteReceiptList.get(position);
            Log.d(TAG + "VoteReceiptLisAdapter", "voteReceipt: " +  String.valueOf(voteReceipt.getId()));
            return convertView;
        }

    }

    public static class ReceiptListAdapter extends ArrayAdapter<VoteReceipt> {

        private final LayoutInflater mInflater;

        public ReceiptListAdapter(Context context) {
            super(context, R.layout.row_evento);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<VoteReceipt> data) {
            clear();
            if (data != null) {
                for (VoteReceipt receipt : data) {
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
            VoteReceipt voteReceipt = getItem(position);
            if (voteReceipt != null) {
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.event_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                TextView author = (TextView) view.findViewById(R.id.event_author);
                TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);

                subject.setText(voteReceipt.getVoto().getAsunto());
                String dateInfoStr = null;
                ImageView imgView = (ImageView)view.findViewById(R.id.event_state_icon);
                if(DateUtils.getTodayDate().after(voteReceipt.getVoto().getFechaFin())) {
                    imgView.setImageResource(R.drawable.closed);
                    dateInfoStr = "<b>" + getContext().getString(R.string.closed_upper_lbl) + "</b> - " +
                            "<b>" + getContext().getString(R.string.inicio_lbl) + "</b>: " +
                            DateUtils.getShortSpanishStringFromDate(
                                    voteReceipt.getVoto().getFechaInicio()) + " - " +
                            "<b>" + getContext().getString(R.string.fin_lbl) + "</b>: " +
                            DateUtils.getShortSpanishStringFromDate(voteReceipt.getVoto().getFechaFin());
                } else {
                    imgView.setImageResource(R.drawable.open);
                    dateInfoStr = "<b>" + getContext().getString(R.string.remain_lbl,
                            DateUtils.getElpasedTimeHoursFromNow(voteReceipt.getVoto().getFechaFin()))  +"</b>";
                }
                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(voteReceipt.isCanceled()) {
                    Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - voteReceipt: " + voteReceipt.getId()
                            + " -position: " + position + " - isCanceled");
                    receiptState.setText(getContext().getString(R.string.vote_canceled_receipt_lbl));
                    receiptState.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - voteReceipt: " + voteReceipt.getId()
                            + " -position: " + position);
                    receiptState.setVisibility(View.GONE);
                }

                //Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - Usuario: " + voteReceipt.getVoto().getUsuario());
                if(voteReceipt.getVoto().getUsuario() != null && !"".equals(
                        voteReceipt.getVoto().getUsuario().getNombreCompleto())) {
                    String authorStr =  "<b>" + getContext().getString(R.string.author_lbl) + "</b>: " +
                            voteReceipt.getVoto().getUsuario().getNombreCompleto();
                    author.setText(Html.fromHtml(authorStr));
                } else author.setVisibility(View.GONE);
            }
            return view;
        }
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", " - caption: "
                + caption + "  - showMessage: " + message);
        if(isDestroyed) return;
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle(caption).setMessage(message).show();
    }



    @Override public void setPin(final String pin) {
        Log.d(TAG + ".setPin()", "--- setPin - ");
        if(pin != null) {
            if(processSignatureTask != null) processSignatureTask.cancel(true);
            processSignatureTask = new ProcessSignatureTask(pin);
            processSignatureTask.execute();
        }
    }

    @Override public void cancelVote(VoteReceipt receipt) {
        Log.d(TAG + ".cancelVote(...)", " - cancelVote");
        operationReceipt = receipt;
        if (!AppData.Estado.CON_CERTIFICADO.equals(appData.getEstado())) {
            Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
            showCertNotFoundDialog();
        } else {
            showPinScreen(getString(R.string.cancel_vote_msg));
        }
    }

    private void dismissOptionsDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment prev = (DialogFragment) getSupportFragmentManager().findFragmentByTag(OPTIONS_DIALOG_ID);
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
        Fragment prev = getSupportFragmentManager().findFragmentByTag(AppData.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, AppData.CERT_NOT_FOUND_DIALOG_ID);
    }


    @Override public void removeReceipt(VoteReceipt receipt) {
        Log.d(TAG + ".removeReceipt()", " --- receipt: " + receipt.getId());
        try {
            db.deleteVoteReceipt(receipt);
            refreshReceiptList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
        isDestroyed = true;
        if(processSignatureTask != null) processSignatureTask.cancel(true);
    };

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", " - onStop");
        isDestroyed = true;
        if(processSignatureTask != null) processSignatureTask.cancel(true);
    }

    public void showProgress(boolean shown, boolean animate) {
        if (isProgressShown == shown) {
            return;
        }
        isProgressShown = shown;
        if (!shown) {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha( 0); // restore
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        this, android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
        }
    }
    private class ProcessSignatureTask extends AsyncTask<URL, Integer, Respuesta> {

        private String pin = null;

        public ProcessSignatureTask(String pin) {
            this.pin = pin;
        }

        protected Respuesta doInBackground(URL... urls) {
            Log.d(TAG + ".ProcessSignatureTask.doInBackground(...)", " - doInBackground " );
            String subject = getString(R.string.cancel_vote_msg_subject);
            String serverURL = ServerPaths.getURLAnulacionVoto(appData.getAccessControlURL());
            try {
                FileInputStream fis = openFileInput(KEY_STORE_FILE);
                byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, pin.toCharArray());
                PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, pin.toCharArray());
                X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);

                byte[] base64encodedvoteCertPrivateKey = Encryptor.decryptMessage(
                        operationReceipt.getEncryptedKey(), signerCert, signerPrivatekey);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                        Base64.decode(base64encodedvoteCertPrivateKey));
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey certPrivKey = kf.generatePrivate(keySpec);
                operationReceipt.setCertVotePrivateKey(certPrivKey);
                boolean isEncryptedResponse = true;
                String signatureContent = operationReceipt.getVoto().getCancelVoteData();
                String serviceURL = ServerPaths.getURLReclamacion(
                        appData.getAccessControlURL());

                SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                        signatureContent, subject, isEncryptedResponse, keyStoreBytes, pin.toCharArray(),
                        appData.getControlAcceso().getCertificado(), getBaseContext());
                return smimeSignedSender.call();
            } catch(Exception ex) {
                ex.printStackTrace();
                return new Respuesta(Respuesta.SC_ERROR, ex.getLocalizedMessage());
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

        protected void onPostExecute(Respuesta respuesta) {
            Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", " - onPostExecute - status:" +
                    respuesta.getCodigoEstado());
            showProgress(false, true);
            String caption  = null;
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                operationReceipt.setCancelVoteReceipt(respuesta.getSmimeMessage());
                String msg = getString(R.string.cancel_vote_result_msg,
                        operationReceipt.getVoto().getAsunto());
                try {
                    db.updateVoteReceipt(operationReceipt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                refreshReceiptList();
                showMessage(getString(R.string.msg_lbl), msg);
            } else {
                caption = getString(R.string.error_lbl) + " "
                        + respuesta.getCodigoEstado();
                if(Respuesta.SC_ANULACION_REPETIDA == respuesta.getCodigoEstado()) {
                    Log.e(TAG + ".setSignServiceMsg(...)", " --- ANULACION_REPETIDA --- ");
                    operationReceipt.setCanceled(true);
                    try {
                        db.updateVoteReceipt(operationReceipt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showMessage(caption, respuesta.getMensaje());
            }
        }

    }


}