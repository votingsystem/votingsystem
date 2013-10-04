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
import android.app.NotificationManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.android.ui.CancelVoteDialog;
import org.sistemavotacion.android.ui.CertNotFoundDialog;
import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.android.ui.VotingResultDialog;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.callable.VoteSender;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionDeEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.sistemavotacion.android.AppData.KEY_STORE_FILE;
import static org.sistemavotacion.android.AppData.MAX_SUBJECT_SIZE;

public class VotingEventActivity extends ActionBarActivity implements CertPinDialogListener {

    public static final String TAG = "VotingEventActivity";

    public enum Operation {CANCEL_VOTE, SAVE_VOTE, VOTE};

    public static Operation operation = Operation.VOTE;
    private Evento evento;
    private VoteReceipt receipt;
    private List<Button> optionButtons = null;
    byte[] keyStoreBytes = null;
    private Button saveReceiptButton;
    private Button cancelVoteButton;
    private AppData appData;


    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private ProcessSignatureTask processSignatureTask;
    private boolean isDestroyed = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", " --- onCreate");

        super.onCreate(savedInstanceState);
        appData = AppData.getInstance(getBaseContext());
        if (AppData.Estado.CON_CERTIFICADO.equals(appData.getEstado())) {
            FileInputStream fis = null;
            try {
                fis = openFileInput(KEY_STORE_FILE);
                keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            } catch (Exception e) {
                e.printStackTrace();
                showMessage(getString(R.string.error_lbl), e.getMessage());
            }
        }
        setContentView(R.layout.voting_event_screen);
        saveReceiptButton = (Button) findViewById(R.id.save_receipt_button);
        cancelVoteButton = (Button) findViewById(R.id.cancel_vote_button);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setLogo(R.drawable.poll_32);
        operation = Operation.VOTE;
        evento = appData.getEvent();
        chechControlCenterCert();
        setEventScreen(evento);
        mainLayout = (FrameLayout) findViewById( R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha( 0);
        isProgressShown = false;
        isDestroyed = false;
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG + ".onRestoreInstanceState(...) ", " -- onRestoreInstanceState --");
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.event, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, NavigationDrawer.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.eventInfo:
                Intent infoIntent = new Intent(this, EventStatisticsActivity.class);
                infoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(infoIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setReceiptScreen(final VoteReceipt receipt) {
        Log.d(TAG + ".setReceiptScreen(...)", " - setReceiptScreen");
        ((LinearLayout)findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
        setTitle(getString(R.string.already_voted_lbl, receipt.getVoto().
                getOpcionSeleccionada().getContenido()));
        TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
        String subject = receipt.getVoto().getAsunto();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        asuntoTextView.setText(subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
        contenidoTextView.setText(Html.fromHtml(receipt.getVoto().getContenido()) + "\n");
        contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<OpcionDeEvento> opciones = receipt.getVoto().getOpciones();
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.option_button_container);
        if(optionButtons == null) {
            optionButtons = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 15, 15);
            for (final OpcionDeEvento opcion:opciones) {
                Button opcionButton = new Button(this);
                opcionButton.setText(opcion.getContenido());
                optionButtons.add(opcionButton);
                opcionButton.setEnabled(false);
                linearLayout.addView(opcionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
        //int dialog = getIntent().getIntExtra(INTENT_EXTRA_DIALOG_PROP_NAME, 0);
        //switch(dialog) {}
    }

    public void cancelVote(View v) {
        Log.d(TAG + ".cancelVote(...)", " - cancelVote");
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(receipt.getNotificationId());
        operation = Operation.CANCEL_VOTE;
        showPinScreen(getString(R.string.cancel_vote_msg));
    }

    public void saveVote(View v) {
        Log.d(TAG + ".saveVote(...)", " - saveVote");
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(receipt.getNotificationId());
		/*Log.d(TAG + ".guardarReciboButton ", " - Files dir path: " +
		getActivity().getApplicationContext().getFilesDir().getAbsolutePath());
		String receiptFileName = StringUtils.getCadenaNormalizada(reciboVoto.getEventoURL()) ;

		File file = new File(getActivity().getApplicationContext().getFilesDir(), receiptFileName);
		Log.d(TAG + ".guardarReciboButton ", " - Files path: " + file.getAbsolutePath());
		try {
			reciboVoto.writoToFile(file);
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.operacion_ok_msg)).
				setMessage(getString(R.string.receipt_file_saved_msg)).show();
		} catch(Exception ex) {
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.error_lbl)).
				setMessage(ex.getMessage()).show();
		}*/
        VoteReceiptDBHelper db = new VoteReceiptDBHelper(VotingEventActivity.this);
        try {
            receipt.setId(db.insertVoteReceipt(receipt));
            saveReceiptButton.setEnabled(false);
        } catch (Exception ex) {
            Log.e(TAG + ".guardarReciboButton.setOnClickListener(...) ", ex.getMessage(), ex);
        }
    }

    /*@Override public boolean onCreateOptionsMenu(Menu menu) {
    	Intent intent = new Intent(this, FragmentTabsPager.class);
        menu.add(getString(R.string.panel_principal_lbl)).setIntent(intent)
            .setIcon(R.drawable.password_22x22)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }*/

    private void setEventScreen(final Evento event) {
        Log.d(TAG + ".setEventScreen(...)", " - setEventScreen");
        ((LinearLayout)findViewById(R.id.receipt_buttons)).setVisibility(View.GONE);
        switch(event.getEstadoEnumValue()) {
            case ACTIVO:
                setTitle(getString(R.string.voting_open_lbl,
                        DateUtils.getElpasedTimeHoursFromNow(event.getFechaFin())));
                break;
            case PENDIENTE_COMIENZO:
                setTitle(getString(R.string.voting_pending_lbl));
                break;
            default:
                setTitle(getString(R.string.voting_closed_lbl));
        }
        TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        String subject = event.getAsunto();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        asuntoTextView.setText(subject);
        TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
        contenidoTextView.setText(Html.fromHtml(event.getContenido()));
        //contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<OpcionDeEvento> opciones = event.getOpciones();
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.option_button_container);
        if(optionButtons != null) linearLayout.removeAllViews();
        optionButtons = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final OpcionDeEvento opcion:opciones) {
            Button opcionButton = new Button(this);
            opcionButton.setText(opcion.getContenido());
            opcionButton.setOnClickListener(new Button.OnClickListener() {
                OpcionDeEvento opcionSeleccionada = opcion;
                public void onClick(View v) {
                    Log.d(TAG + "- opcionButton - opcionId: " +
                            opcionSeleccionada.getId(), "estado: " +
                            appData.getEstado().toString());
                    processSelectedOption(opcionSeleccionada);
                }
            });
            optionButtons.add(opcionButton);
            if (!event.estaAbierto()) opcionButton.setEnabled(false);
            linearLayout.addView(opcionButton, paramsButton);

        }
        if(event.getOpcionSeleccionada() != null) {
            Log.d(TAG + ".setEventScreen", " --- Tiene seleccionada la opcion: " +event.getOpcionSeleccionada().getContenido() );
            processSelectedOption(event.getOpcionSeleccionada());
        } else Log.d(TAG + ".setEventScreen", "Opción seleccionada nula");
    }

    private void processSelectedOption(OpcionDeEvento opcionSeleccionada) {
        Log.d(TAG + ".processSelectedOption", " -- processSelectedOption");
        operation = Operation.VOTE;
        evento.setOpcionSeleccionada(opcionSeleccionada);
        if (!AppData.Estado.CON_CERTIFICADO.equals(appData.getEstado())) {
            Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
            showCertNotFoundDialog();
        } else {
            chechControlCenterCert();
            String contenido = opcionSeleccionada.getContenido().length() >
                    AppData.SELECTED_OPTION_MAX_LENGTH ?
                    opcionSeleccionada.getContenido().substring(0, AppData.SELECTED_OPTION_MAX_LENGTH) +
                            "..." : opcionSeleccionada.getContenido();
            showPinScreen(getString(R.string.option_selected_msg, contenido));
        }
    }

    private void chechControlCenterCert() {
        if(evento.getCentroControl().getCertificado() == null) {
            try {
                appData.checkCert(evento.getCentroControl(), getBaseContext());
            } catch (Exception ex) {
                ex.printStackTrace();
                showMessage(getString(R.string.error_lbl),
                        getString(R.string.CONTROL_CENTER_CONECTION_ERROR_MSG));
            }
        }
    }

    @Override public void onResume() {
        super.onResume();
        Log.d(TAG + ".onResume() ", " - onResume");
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

    public void onClickSubject(View v) {
        Log.d(TAG + ".onClickSubject(...)", " - onClickSubject");
        if(evento != null && evento.getAsunto() != null &&
                evento.getAsunto().length() > MAX_SUBJECT_SIZE) {
            AlertDialog.Builder builder= new AlertDialog.Builder(VotingEventActivity.this);
            builder.setTitle(getString(R.string.subject_lbl));
            builder.setMessage(evento.getAsunto());
            builder.show();
        }
    }

    private void setOptionButtonsEnabled(boolean areEnabled) {
        if(optionButtons == null) return;
        for(Button button:optionButtons) {
            button.setEnabled(areEnabled);
        }
    }

    private void showHtmlMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", " - caption: "
                + caption + "  - showMessage: " + message);
        /*TextView contenidoTextView = new TextView(getApplicationContext());
        contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        contenidoTextView.setPadding(20, 20, 20, 20);
    	contenidoTextView.setText(Html.fromHtml(message));
        contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setView(contenidoTextView).show();*/
        AlertDialog alertDialog= new AlertDialog.Builder(this).
                setTitle(caption).setMessage(Html.fromHtml(message)).create();
        alertDialog.show();
        ((TextView)alertDialog.findViewById(android.R.id.message)).
                setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", " - caption: " + caption + "  - showMessage: " +
                message + " - isDestroyed: " + isDestroyed);
        if(isDestroyed) return;
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle(caption).setMessage(message).show();
    }

    private void showPinScreen(String message) {
        isDestroyed = false;
        CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        pinDialog.show(ft, CertPinDialog.TAG);
    }


    @Override public void setPin(final String pin) {
        Log.d(TAG + ".setPin()", "--- setPin - operation: " + operation);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
        if(pin == null) {
            Log.d(TAG + ".setPin()", "--- setPin - pin null");
            return;
        }
        if(processSignatureTask != null) processSignatureTask.cancel(true);
        processSignatureTask = new ProcessSignatureTask(pin);
        processSignatureTask.execute();
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
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {return false;}
            });
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
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) { return true; }
            });
        }
    }

    private class ProcessSignatureTask extends AsyncTask<URL, Integer, Respuesta> {

        private String pin = null;

        public ProcessSignatureTask(String pin) {
            this.pin = pin;
        }

        protected void onPreExecute() {
            Log.d(TAG + ".ProcessSignatureTask.onPreExecute(...)", " --- onPreExecute");
            getWindow().getDecorView().findViewById(
                    android.R.id.content).invalidate();
            showProgress(true, true);
            switch(operation) {
                case VOTE:
                    setOptionButtonsEnabled(false);
                    break;
                case CANCEL_VOTE:
                    cancelVoteButton.setEnabled(false);
                    break;
            }
        }

        protected Respuesta doInBackground(URL... urls) {
            Log.d(TAG + ".ProcessSignatureTask.doInBackground(...)",
                    " - doInBackground - event type: " + evento.getTipo());
            Respuesta respuesta = null;
            switch(operation) {
                case VOTE:
                    try {
                        VoteSender voteSender = new VoteSender(evento, keyStoreBytes,
                                pin.toCharArray(), getBaseContext());
                        respuesta = voteSender.call();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return new Respuesta(Respuesta.SC_ERROR, ex.getLocalizedMessage());
                    }
                    break;
                case CANCEL_VOTE:
                    String subject = getString(R.string.cancel_vote_msg_subject);
                    String serviceURL = ServerPaths.getURLAnulacionVoto(appData.getAccessControlURL());
                    try {
                        String signatureContent = receipt.getVoto().getCancelVoteData();
                        boolean isEncryptedResponse = true;
                        FileInputStream fis = openFileInput(KEY_STORE_FILE);
                        byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                                signatureContent, subject, isEncryptedResponse, keyStoreBytes, pin.toCharArray(),
                                appData.getControlAcceso().getCertificado(), getBaseContext());
                        respuesta = smimeSignedSender.call();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        return new Respuesta(Respuesta.SC_ERROR, ex.getLocalizedMessage());
                    }
                    break;
                default:
                    Log.d(TAG + ".processPinTask(...)", "--- unknown operation: " + operation);
                    respuesta = new Respuesta(Respuesta.SC_ERROR, getString(R.string.errorOperacionNoEncontrada));
            }
            return respuesta;
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(Respuesta response) {
            Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", " - onPostExecute - status:" +
                    response.getCodigoEstado());
            showProgress(false, true);
            switch(operation) {
                case VOTE:
                    setOptionButtonsEnabled(false);
                    if(Respuesta.SC_OK == response.getCodigoEstado()) {
                        receipt = (VoteReceipt)response.getData();
                        VotingResultDialog votingResultDialog = VotingResultDialog.newInstance(
                                getString(R.string.operacion_ok_msg), receipt);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        votingResultDialog.show(fragmentManager, "fragment_voting_result");

                        setReceiptScreen(receipt);
                    } else if(Respuesta.SC_ERROR_VOTO_REPETIDO == response.getCodigoEstado()) {
                        showHtmlMessage(getString(R.string.access_request_repeated_caption), getString(
                                R.string.access_request_repeated_msg,
                                evento.getAsunto(), response.getMensaje()));
                        return;
                    } else {
                        showHtmlMessage(getString(R.string.error_lbl), response.getMensaje());
                        setOptionButtonsEnabled(true);
                    }
                    break;
                case CANCEL_VOTE:
                    if(Respuesta.SC_OK == response.getCodigoEstado()) {
                        SMIMEMessageWrapper cancelReceipt = response.getSmimeMessage();
                        receipt.setCancelVoteReceipt(cancelReceipt);
                        String msg = getString(R.string.cancel_vote_result_msg,
                                receipt.getVoto().getAsunto());
                        if(receipt.getId() > 0) {
                            VoteReceiptDBHelper db = new VoteReceiptDBHelper(VotingEventActivity.this);
                            try {
                                db.insertVoteReceipt(receipt);
                            } catch (Exception ex) {
                                Log.e(TAG + ".guardarReciboButton.setOnClickListener(...) ", ex.getMessage(), ex);
                            }
                        }
                        appData.getEvent().setOpcionSeleccionada(null);
                        setEventScreen(appData.getEvent());
                        CancelVoteDialog cancelVoteDialog = CancelVoteDialog.newInstance(
                                getString(R.string.msg_lbl), msg, receipt);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        cancelVoteDialog.show(fragmentManager, "fragment_cancel_vote_result");
                    } else {
                        cancelVoteButton.setEnabled(true);
                        showMessage(getString(R.string.error_lbl), response.getMensaje());
                    }
                    break;
            }
        }
    }

}