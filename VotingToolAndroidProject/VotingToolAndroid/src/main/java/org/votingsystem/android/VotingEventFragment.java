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

package org.votingsystem.android;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
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
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.votingsystem.android.db.VoteReceiptDBHelper;
import org.votingsystem.android.model.ContextVSAndroid;
import org.votingsystem.android.model.EventVSAndroid;
import org.votingsystem.android.ui.CancelVoteDialog;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.android.ui.VotingResultDialog;
import org.votingsystem.callable.DataGetter;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.callable.VoteSender;
import org.votingsystem.model.OptionVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.android.model.VoteReceipt;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.FileUtils;
import org.votingsystem.android.util.ServerPaths;

import java.io.FileInputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.votingsystem.android.model.ContextVSAndroid.KEY_STORE_FILE;
import static org.votingsystem.android.model.ContextVSAndroid.MAX_SUBJECT_SIZE;

public class VotingEventFragment extends Fragment implements CertPinDialogListener {

    public static final String TAG = "VotingEventFragment";

    public enum Operation {CANCEL_VOTE, SAVE_VOTE, VOTE};

    private Operation operation = Operation.VOTE;
    private EventVSAndroid eventVSAndroid;
    private VoteReceipt receipt;
    private List<Button> optionButtons = null;
    private byte[] keyStoreBytes = null;
    private Button saveReceiptButton;
    private Button cancelVoteButton;
    private ContextVSAndroid contextVSAndroid;

    private View rootView;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private ProcessSignatureTask processSignatureTask;
    private boolean isDestroyed = true;

    @Override public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", " --- onCreate");
        super.onCreate(savedInstanceState);
        contextVSAndroid = ContextVSAndroid.getInstance(getActivity());
        rootView = inflater.inflate(R.layout.voting_event_fragment, container, false);
        Bundle args = getArguments();
        Integer eventIndex =  args.getInt(EventPagerActivity.EventsPagerAdapter.EVENT_INDEX_KEY);
        if(eventIndex != null) {
            eventVSAndroid = (EventVSAndroid) contextVSAndroid.getEvents().get(eventIndex);
        } else {
            String eventStr = args.getString(ContextVSAndroid.EVENT_KEY);
            try {
                eventVSAndroid = EventVSAndroid.parse(eventStr);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        setEventScreen(eventVSAndroid);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        isProgressShown = false;
        setHasOptionsMenu(true);
        isDestroyed = false;
        return rootView;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.event, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(getActivity(), EventStatisticsPagerActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setReceiptScreen(final VoteReceipt receipt) {
        Log.d(TAG + ".setReceiptScreen(...)", " - setReceiptScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getString(
                R.string.already_voted_lbl, receipt.getVoto().
                getOpcionSeleccionada().getContent()));
        TextView asuntoTextView = (TextView) rootView.findViewById(R.id.asunto_evento);
        String subject = receipt.getVoto().getAsunto();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        asuntoTextView.setText(subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        TextView contenidoTextView = (TextView) rootView.findViewById(R.id.contenido_evento);
        contenidoTextView.setText(Html.fromHtml(receipt.getVoto().getContenido()) + "\n");
        contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<OptionVS> opciones = receipt.getVoto().getOpciones();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtons == null) {
            optionButtons = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 15, 15);
            for (final OptionVS opcion:opciones) {
                Button opcionButton = new Button(getActivity());
                opcionButton.setText(opcion.getContent());
                optionButtons.add(opcionButton);
                opcionButton.setEnabled(false);
                linearLayout.addView(opcionButton, paramsButton);
            }
        } else setOptionButtonsEnabled(false);
    }

    public void cancelVote(View v) {
        Log.d(TAG + ".cancelVote(...)", " - cancelVote");
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(receipt.getNotificationId());
        operation = Operation.CANCEL_VOTE;
        showPinScreen(getString(R.string.cancel_vote_msg));
    }

    public void saveVote(View v) {
        Log.d(TAG + ".saveVote(...)", " - saveVote");
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
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
        VoteReceiptDBHelper db = new VoteReceiptDBHelper(getActivity());
        try {
            receipt.setId(db.insertVoteReceipt(receipt));
            saveReceiptButton.setEnabled(false);
        } catch (Exception ex) {
            Log.e(TAG + ".guardarReciboButton.setOnClickListener(...) ", ex.getMessage(), ex);
        }
    }


    private void setEventScreen(final EventVSAndroid event) {
        Log.d(TAG + ".setEventScreen(...)", " - setEventScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.GONE);
        TextView asuntoTextView = (TextView) rootView.findViewById(R.id.asunto_evento);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        String subject = event.getAsunto();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        asuntoTextView.setText(subject);
        TextView contenidoTextView = (TextView) rootView.findViewById(R.id.contenido_evento);
        contenidoTextView.setText(Html.fromHtml(event.getContenido()));
        //contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<OptionVS> opciones = event.getOpciones();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtons != null) linearLayout.removeAllViews();
        optionButtons = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final OptionVS opcion:opciones) {
            Button opcionButton = new Button(getActivity());
            opcionButton.setText(opcion.getContent());
            opcionButton.setOnClickListener(new Button.OnClickListener() {
                OptionVS opcionSeleccionada = opcion;
                public void onClick(View v) {
                    Log.d(TAG + "- opcionButton - opcionId: " +
                            opcionSeleccionada.getId(), "estado: " +
                            contextVSAndroid.getEstado().toString());
                    processSelectedOption(opcionSeleccionada);
                }
            });
            optionButtons.add(opcionButton);
            if (!event.estaAbierto()) opcionButton.setEnabled(false);
            linearLayout.addView(opcionButton, paramsButton);

        }
        if(event.getOpcionSeleccionada() != null) {
            Log.d(TAG + ".setEventScreen", " --- Tiene seleccionada la opcion: " +event.getOpcionSeleccionada().getContent() );
            processSelectedOption(event.getOpcionSeleccionada());
        } else Log.d(TAG + ".setEventScreen", "Opción seleccionada nula");
    }

    private void processSelectedOption(OptionVS opcionSeleccionada) {
        Log.d(TAG + ".processSelectedOption", " -- processSelectedOption");
        operation = Operation.VOTE;
        eventVSAndroid.setOpcionSeleccionada(opcionSeleccionada);
        if (!ContextVSAndroid.Estado.CON_CERTIFICADO.equals(contextVSAndroid.getEstado())) {
            Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
            showCertNotFoundDialog();
        } else {
            String contenido = opcionSeleccionada.getContent().length() >
                    ContextVSAndroid.SELECTED_OPTION_MAX_LENGTH ?
                    opcionSeleccionada.getContent().substring(0, ContextVSAndroid.SELECTED_OPTION_MAX_LENGTH) +
                            "..." : opcionSeleccionada.getContent();
            showPinScreen(getString(R.string.option_selected_msg, contenido));
        }
    }

    @Override public void onResume() {
        super.onResume();
        Log.d(TAG + ".onResume() ", " - onResume");
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(
                ContextVSAndroid.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVSAndroid.CERT_NOT_FOUND_DIALOG_ID);
    }

    public void onClickSubject(View v) {
        Log.d(TAG + ".onClickSubject(...)", " - onClickSubject");
        if(eventVSAndroid != null && eventVSAndroid.getAsunto() != null &&
                eventVSAndroid.getAsunto().length() > MAX_SUBJECT_SIZE) {
            AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.subject_lbl));
            builder.setMessage(eventVSAndroid.getAsunto());
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
        AlertDialog alertDialog= new AlertDialog.Builder(getActivity()).
                setTitle(caption).setMessage(Html.fromHtml(message)).create();
        alertDialog.show();
        ((TextView)alertDialog.findViewById(android.R.id.message)).
                setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", " - caption: " + caption + "  - showMessage: " +
                message + " - isDestroyed: " + isDestroyed);
        if(isDestroyed) return;
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setTitle(caption).setMessage(message).show();
    }

    private void showPinScreen(String message) {
        isDestroyed = false;
        CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        pinDialog.show(ft, CertPinDialog.TAG);
    }


    @Override public void setPin(final String pin) {
        Log.d(TAG + ".setPin()", "--- setPin - operation: " + operation);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
        if(pin == null) {
            Log.d(TAG + ".setPin()", "--- setPin - pin null");
            return;
        }
        X509Certificate controlCenterCert = contextVSAndroid.getCert(eventVSAndroid.getControlCenter().getServerURL());
        if(eventVSAndroid.getControlCenter().getCertificado() == null && controlCenterCert == null) {
            GetCertTask getCertTask = new GetCertTask(pin);
            getCertTask.execute(eventVSAndroid.getControlCenter().getServerURL());
        } else {
            if(eventVSAndroid.getControlCenter().getCertificado() == null) {
                eventVSAndroid.getControlCenter().setCertificado(controlCenterCert);
            }
            if(processSignatureTask != null) processSignatureTask.cancel(true);
            processSignatureTask = new ProcessSignatureTask(pin);
            processSignatureTask.execute();
        }
    }

    @Override public void onDestroy() {
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
                        getActivity(), android.R.anim.fade_out));
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
                        getActivity(), android.R.anim.fade_in));
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

    private class ProcessSignatureTask extends AsyncTask<URL, Integer, ResponseVS> {

        private String pin = null;

        public ProcessSignatureTask(String pin) {
            this.pin = pin;
        }

        protected void onPreExecute() {
            Log.d(TAG + ".ProcessSignatureTask.onPreExecute(...)", " --- onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(
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

        protected ResponseVS doInBackground(URL... urls) {
            Log.d(TAG + ".ProcessSignatureTask.doInBackground(...)",
                    " - doInBackground - event type: " + eventVSAndroid.getTypeVS());
            ResponseVS responseVS = null;
            switch(operation) {
                case VOTE:
                    try {
                        FileInputStream fis = null;
                        fis = getActivity().openFileInput(KEY_STORE_FILE);
                        keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                        VoteSender voteSender = new VoteSender(eventVSAndroid, keyStoreBytes,
                                pin.toCharArray(), getActivity());
                        responseVS = voteSender.call();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
                    }
                    break;
                case CANCEL_VOTE:
                    String subject = getString(R.string.cancel_vote_msg_subject);
                    String serviceURL = ServerPaths.getURLAnulacionVoto(contextVSAndroid.getAccessControlURL());
                    try {
                        String signatureContent = receipt.getVoto().getCancelVoteData();
                        boolean isEncryptedResponse = true;
                        FileInputStream fis = getActivity().openFileInput(KEY_STORE_FILE);
                        byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                                signatureContent, subject, isEncryptedResponse, keyStoreBytes, pin.toCharArray(),
                                contextVSAndroid.getAccessControl().getCertificado(), getActivity());
                        responseVS = smimeSignedSender.call();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
                    }
                    break;
                default:
                    Log.d(TAG + ".processPinTask(...)", "--- unknown operation: " + operation);
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR, getString(R.string.errorOperacionNoEncontrada));
            }
            return responseVS;
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(ResponseVS response) {
            Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", " - onPostExecute - status:" +
                    response.getStatusCode());
            showProgress(false, true);
            switch(operation) {
                case VOTE:
                    setOptionButtonsEnabled(false);
                    if(ResponseVS.SC_OK == response.getStatusCode()) {
                        receipt = (VoteReceipt)response.getData();
                        VotingResultDialog votingResultDialog = VotingResultDialog.newInstance(
                                getString(R.string.operacion_ok_msg), receipt);
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        votingResultDialog.show(fragmentManager, "fragment_voting_result");

                        setReceiptScreen(receipt);
                    } else if(ResponseVS.SC_ERROR_VOTE_REPEATED == response.getStatusCode()) {
                        showHtmlMessage(getString(R.string.access_request_repeated_caption), getString(
                                R.string.access_request_repeated_msg,
                                eventVSAndroid.getAsunto(), response.getMessage()));
                        return;
                    } else {
                        showHtmlMessage(getString(R.string.error_lbl), response.getMessage());
                        setOptionButtonsEnabled(true);
                    }
                    break;
                case CANCEL_VOTE:
                    if(ResponseVS.SC_OK == response.getStatusCode()) {
                        SMIMEMessageWrapper cancelReceipt = response.getSmimeMessage();
                        receipt.setCancelVoteReceipt(cancelReceipt);
                        String msg = getString(R.string.cancel_vote_result_msg,
                                receipt.getVoto().getAsunto());
                        if(receipt.getId() > 0) {
                            VoteReceiptDBHelper db = new VoteReceiptDBHelper(getActivity());
                            try {
                                db.insertVoteReceipt(receipt);
                            } catch (Exception ex) {
                                Log.e(TAG + ".guardarReciboButton.setOnClickListener(...) ", ex.getMessage(), ex);
                            }
                        }
                        contextVSAndroid.getEvent().setOpcionSeleccionada(null);
                        setEventScreen(contextVSAndroid.getEvent());
                        CancelVoteDialog cancelVoteDialog = CancelVoteDialog.newInstance(
                                getString(R.string.msg_lbl), msg, receipt);
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        cancelVoteDialog.show(fragmentManager, "fragment_cancel_vote_result");
                    } else {
                        cancelVoteButton.setEnabled(true);
                        showMessage(getString(R.string.error_lbl), response.getMessage());
                    }
                    break;
            }
        }
    }

    private class GetCertTask extends AsyncTask<String, Integer, ResponseVS> {

        private String serverURL = null;
        private String pin = null;

        public GetCertTask(String pin) {
            this.pin = pin;
        }

        protected void onPreExecute() {
            Log.d(TAG + ".GetCertTask.onPreExecute(...)", " --- onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(
                    android.R.id.content).invalidate();
            showProgress(true, true);
        }

        protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".GetCertTask.doInBackground(...)", " - serverURL: " + urls[0]);
            serverURL = urls[0];
            String serverCertURL = ServerPaths.getURLCadenaCertificacion(serverURL);
            DataGetter dataGetter = new DataGetter(null, serverCertURL);
            return dataGetter.call();
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(ResponseVS responseVS) {
            Log.d(TAG + ".GetCertTask.onPostExecute(...)", " - onPostExecute - status:" +
                    responseVS.getStatusCode());
            showProgress(false, true);
            try {
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(
                            responseVS.getMessageBytes());
                    X509Certificate serverCert = certChain.iterator().next();
                    contextVSAndroid.putCert(serverURL, serverCert);
                    eventVSAndroid.getControlCenter().setCertificado(serverCert);
                    if(processSignatureTask != null) processSignatureTask.cancel(true);
                    processSignatureTask = new ProcessSignatureTask(pin);
                    processSignatureTask.execute();
                } else {
                    Log.d(TAG + ".getServerCert() ", " - Error message: " + responseVS.getMessage());
                    showMessage(getString(R.string.get_cert_error_msg) + ": " + serverURL,
                            responseVS.getMessage());
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                showMessage(getString(R.string.error_lbl), ex.getMessage());
            }
        }
    }

}