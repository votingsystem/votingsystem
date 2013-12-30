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

package org.votingsystem.android.fragment;

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
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.votingsystem.android.activity.EventPagerActivity;
import org.votingsystem.android.activity.EventStatisticsPagerActivity;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.VoteSender;
import org.votingsystem.android.contentprovider.VoteReceiptDBHelper;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.android.ui.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.FileUtils;
import org.votingsystem.model.OptionVS;
import org.votingsystem.util.HttpHelper;

import java.io.FileInputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.MAX_SUBJECT_SIZE;

public class VotingEventFragment extends Fragment implements CertPinDialogListener,
        View.OnClickListener {

    public static final String TAG = "VotingEventFragment";

    public enum Operation {CANCEL_VOTE, SAVE_VOTE, VOTE};

    private Operation operation = Operation.VOTE;
    private EventVS eventVS;
    private VoteVS receipt;
    private List<Button> optionButtons = null;
    private byte[] keyStoreBytes = null;
    private Button saveReceiptButton;
    private Button cancelVoteButton;
    private ContextVS contextVS;

    private View rootView;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private ProcessSignatureTask processSignatureTask;

    public static VotingEventFragment newInstance(Integer eventId) {
        VotingEventFragment fragment = new VotingEventFragment();
        Bundle args = new Bundle();
        args.putInt(ContextVS.ITEM_ID_KEY, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", "onCreate");
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        rootView = inflater.inflate(R.layout.voting_event_fragment, container, false);
        Bundle args = getArguments();
        Integer eventIndex =  args.getInt(ContextVS.ITEM_ID_KEY);
        if(eventIndex != null) {
            eventVS = (EventVS) contextVS.getEvents().get(eventIndex);
        } else {
            String eventStr = args.getString(ContextVS.EVENT_KEY);
            try {
                eventVS = EventVS.parse(eventStr);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        setEventScreen(eventVS);
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha(0);
        isProgressShown = false;
        setHasOptionsMenu(true);
        Button cancelVoteButton = (Button) rootView.findViewById(R.id.cancel_vote_button);
        cancelVoteButton.setOnClickListener(this);
        Button saveReceiptButton = (Button) rootView.findViewById(R.id.save_receipt_button);
        saveReceiptButton.setOnClickListener(this);
        TextView eventSubject = (TextView) rootView.findViewById(R.id.event_subject);
        eventSubject.setOnClickListener(this);
        return rootView;
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel_vote_button:
                cancelVote(view);
                break;
            case R.id.save_receipt_button:
                saveVote(view);
                break;
            case R.id.event_subject:
                onClickSubject(view);
                break;
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.event, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.eventInfo:
                Intent intent = new Intent(getActivity().getApplicationContext(),
                        EventStatisticsPagerActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setReceiptScreen(final VoteVS receipt) {
        Log.d(TAG + ".setReceiptScreen(...)", " - setReceiptScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
        ((ActionBarActivity)getActivity().getApplicationContext()).getSupportActionBar().
                setTitle(getString(R.string.already_voted_lbl, receipt.getVoto().
                getOptionSelected().getContent()));
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        String subject = receipt.getVoto().getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(receipt.getVoto().getContent()) + "\n");
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<OptionVS> fieldsEventVS = receipt.getVoto().getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtons == null) {
            optionButtons = new ArrayList<Button>();
            FrameLayout.LayoutParams paramsButton = new
                    FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            paramsButton.setMargins(15, 15, 15, 15);
            for (final OptionVS opcion:fieldsEventVS) {
                Button opcionButton = new Button(getActivity().getApplicationContext());
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
		String receiptFileName = StringUtils.getNormalized(reciboVoto.getEventURL()) ;

		File file = new File(getActivity().getApplicationContext().getFilesDir(), receiptFileName);
		Log.d(TAG + ".guardarReciboButton ", " - Files path: " + file.getAbsolutePath());
		try {
			reciboVoto.writoToFile(file);
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.operation_ok_msg)).
				setMessage(getString(R.string.receipt_file_saved_msg)).show();
		} catch(Exception ex) {
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.error_lbl)).
				setMessage(ex.getMessage()).show();
		}*/
        VoteReceiptDBHelper db = new VoteReceiptDBHelper(getActivity().getApplicationContext());
        try {
            receipt.setId(db.insertVoteReceipt(receipt));
            saveReceiptButton.setEnabled(false);
        } catch (Exception ex) {
            Log.e(TAG + ".guardarReciboButton.setOnClickListener(...) ", ex.getMessage(), ex);
        }
    }


    private void setEventScreen(final EventVS event) {
        Log.d(TAG + ".setEventScreen(...)", " - setEventScreen");
        ((LinearLayout)rootView.findViewById(R.id.receipt_buttons)).setVisibility(View.GONE);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_subject);
        cancelVoteButton.setEnabled(true);
        saveReceiptButton.setEnabled(true);
        String subject = event.getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.event_content);
        contentTextView.setText(Html.fromHtml(event.getContent()));
        //contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Set<OptionVS> fieldsEventVS = event.getFieldsEventVS();
        LinearLayout linearLayout = (LinearLayout)rootView.findViewById(R.id.option_button_container);
        if(optionButtons != null) linearLayout.removeAllViews();
        optionButtons = new ArrayList<Button>();
        FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        paramsButton.setMargins(15, 15, 15, 15);
        for (final OptionVS option:fieldsEventVS) {
            Button optionButton = new Button(getActivity().getApplicationContext());
            optionButton.setText(option.getContent());
            optionButton.setOnClickListener(new Button.OnClickListener() {
                OptionVS optionSelected = option;
                public void onClick(View v) {
                    Log.d(TAG + "- optionButton - optionId: " +
                            optionSelected.getId(), "state: " +
                            contextVS.getState().toString());
                    processSelectedOption(optionSelected);
                }
            });
            optionButtons.add(optionButton);
            if (!event.isActive()) optionButton.setEnabled(false);
            linearLayout.addView(optionButton, paramsButton);

        }
        if(event.getOptionSelected() != null) {
            Log.d(TAG + ".setEventScreen", "Selected option: " +event.getOptionSelected().getContent() );
            processSelectedOption(event.getOptionSelected());
        } else Log.d(TAG + ".setEventScreen", "Selected null option");
    }

    private void processSelectedOption(OptionVS optionSelected) {
        Log.d(TAG + ".processSelectedOption", "processSelectedOption");
        operation = Operation.VOTE;
        eventVS.setOptionSelected(optionSelected);
        if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
            showCertNotFoundDialog();
        } else {
            String content = optionSelected.getContent().length() >
                    ContextVS.SELECTED_OPTION_MAX_LENGTH ?
                    optionSelected.getContent().substring(0, ContextVS.SELECTED_OPTION_MAX_LENGTH) +
                            "..." : optionSelected.getContent();
            showPinScreen(getString(R.string.option_selected_msg, content));
        }
    }

    @Override public void onResume() {
        super.onResume();
        Log.d(TAG + ".onResume() ", "onResume");
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(
                ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
    }

    public void onClickSubject(View v) {
        Log.d(TAG + ".onClickSubject(...)", " - onClickSubject");
        if(eventVS != null && eventVS.getSubject() != null &&
                eventVS.getSubject().length() > MAX_SUBJECT_SIZE) {
            AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.subject_lbl));
            builder.setMessage(eventVS.getSubject());
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
        /*TextView contentTextView = new TextView(getApplicationContext());
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        contentTextView.setPadding(20, 20, 20, 20);
    	contentTextView.setText(Html.fromHtml(message));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setView(contentTextView).show();*/
        AlertDialog alertDialog= new AlertDialog.Builder(getActivity()).
                setTitle(caption).setMessage(Html.fromHtml(message)).create();
        alertDialog.show();
        ((TextView)alertDialog.findViewById(android.R.id.message)).
                setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "caption: " + caption + " - showMessage: " + message);
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setTitle(caption).setMessage(message).show();
    }

    private void showPinScreen(String message) {
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
        X509Certificate controlCenterCert = contextVS.getCert(eventVS.getControlCenter().getServerURL());
        if(eventVS.getControlCenter().getCertificate() == null && controlCenterCert == null) {
            GetCertTask getCertTask = new GetCertTask(pin, eventVS.getControlCenter());
            getCertTask.execute();
        } else {
            if(eventVS.getControlCenter().getCertificate() == null) {
                eventVS.getControlCenter().setCertificate(controlCenterCert);
            }
            if(processSignatureTask != null) processSignatureTask.cancel(true);
            processSignatureTask = new ProcessSignatureTask(pin);
            processSignatureTask.execute();
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
        if(processSignatureTask != null) processSignatureTask.cancel(true);
    }

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", " - onStop");
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
                        getActivity().getApplicationContext(), android.R.anim.fade_out));
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
                        getActivity().getApplicationContext(), android.R.anim.fade_in));
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
                    " - doInBackground - event type: " + eventVS.getTypeVS());
            ResponseVS responseVS = null;
            switch(operation) {
                case VOTE:
                    try {
                        FileInputStream fis = null;
                        fis = getActivity().openFileInput(KEY_STORE_FILE);
                        keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                        VoteSender voteSender = new VoteSender(eventVS, keyStoreBytes,
                                pin.toCharArray(), getActivity().getApplicationContext());
                        responseVS = voteSender.call();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
                    }
                    break;
                case CANCEL_VOTE:
                    String subject = getString(R.string.cancel_vote_msg_subject);
                    String serviceURL = contextVS.getAccessControl().getCancelVoteServiceURL();
                    try {
                        String signatureContent = receipt.getVoto().getCancelVoteData();
                        FileInputStream fis = getActivity().openFileInput(KEY_STORE_FILE);
                        byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                                signatureContent, ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                                subject, keyStoreBytes, pin.toCharArray(),
                                contextVS.getAccessControl().getCertificate(),
                                getActivity().getApplicationContext());
                        responseVS = smimeSignedSender.call();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
                    }
                    break;
                default:
                    Log.d(TAG + ".processPinTask(...)", "--- unknown operation: " + operation);
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR, getString(R.string.operationNotFoundErrorMsg));
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
                        receipt = (VoteVS)response.getData();
                        VotingResultDialog votingResultDialog = VotingResultDialog.newInstance(
                                getString(R.string.operation_ok_msg), receipt);
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        votingResultDialog.show(fragmentManager, "fragment_voting_result");

                        setReceiptScreen(receipt);
                    } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == response.getStatusCode()) {
                        showHtmlMessage(getString(R.string.access_request_repeated_caption), getString(
                                R.string.access_request_repeated_msg,
                                eventVS.getSubject(), response.getMessage()));
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
                                receipt.getVoto().getSubject());
                        if(receipt.getId() > 0) {
                            VoteReceiptDBHelper db = new VoteReceiptDBHelper(
                                    getActivity().getApplicationContext());
                            try {
                                db.insertVoteReceipt(receipt);
                            } catch (Exception ex) {
                                Log.e(TAG + ".guardarReciboButton.setOnClickListener(...) ", ex.getMessage(), ex);
                            }
                        }
                        contextVS.getEvent().setOptionSelected(null);
                        setEventScreen(contextVS.getEvent());
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

        private ActorVS actorVS = null;
        private String pin = null;

        public GetCertTask(String pin,ActorVS actorVS) {
            this.actorVS = actorVS;
            this.pin = pin;
        }

        protected void onPreExecute() {
            Log.d(TAG + ".GetCertTask.onPreExecute(...)", " --- onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            showProgress(true, true);
        }

        protected ResponseVS doInBackground(String... urls) {
            Log.d(TAG + ".GetCertTask.doInBackground(...)", " - serviceURL: " +
                    actorVS.getCertChainURL());
            return HttpHelper.getData(actorVS.getCertChainURL(), null);
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
                    contextVS.putCert(actorVS.getServerURL(), serverCert);
                    eventVS.getControlCenter().setCertificate(serverCert);
                    if(processSignatureTask != null) processSignatureTask.cancel(true);
                    processSignatureTask = new ProcessSignatureTask(pin);
                    processSignatureTask.execute();
                } else {
                    Log.d(TAG + ".getServerCert() ", " - Error message: " + responseVS.getMessage());
                    showMessage(getString(R.string.get_cert_error_msg) + ": " +
                            actorVS.getServerURL(), responseVS.getMessage());
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                showMessage(getString(R.string.error_lbl), ex.getMessage());
            }
        }
    }

}