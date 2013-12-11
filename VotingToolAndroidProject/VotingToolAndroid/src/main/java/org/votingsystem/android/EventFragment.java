package org.votingsystem.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;

import org.json.JSONObject;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OptionVS;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.PDFSignedSender;
import org.votingsystem.model.EventVS;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.CertPinDialog;
import org.votingsystem.android.ui.CertPinDialogListener;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.FileUtils;

import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.votingsystem.model.ContextVS.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventFragment extends Fragment implements CertPinDialogListener {

    public static final String TAG = "EventFragment";

    private Button firmarEnviarButton;
    private EventVS event = null;
    private int eventIndex;
    private ContextVS contextVS;
    private Map<Integer, EditText> mapaCamposReclamacion;
    private ProcessSignatureTask processSignatureTask;

    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        //TODO, this is for long inactive periods
        if(getActivity() == null) return null;
        Bundle args = getArguments();
        eventIndex =  args.getInt(EventPagerActivity.EventsPagerAdapter.EVENT_INDEX_KEY);
        contextVS = ContextVS.getInstance(getActivity());
        event = (EventVS) contextVS.getEvents().get(eventIndex);
        View rootView = inflater.inflate(R.layout.event_fragment, container, false);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.subject_evento);
        String subject = event.getSubject();
        if(subject != null && subject.length() > MAX_SUBJECT_SIZE)
            subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
        subjectTextView.setText(subject);
        TextView contentTextView = (TextView) rootView.findViewById(R.id.eventvs_content);

        contentTextView.setText(Html.fromHtml(event.getContent()));
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        firmarEnviarButton = (Button) rootView.findViewById(R.id.firmar_enviar_button);
        if (!event.isActive()) {
            Log.d(TAG + ".onCreate(..)", " - Event closed");
            firmarEnviarButton.setEnabled(false);
        }
        firmarEnviarButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG + "- firmarEnviarButton -", " - state: " + contextVS.getState().toString());
                if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
                    Log.d(TAG + "-firmarEnviarButton-", " - showCertNotFoundDialog");
                    showCertNotFoundDialog();
                    return;
                }
                if (event.getTypeVS().equals(TypeVS.CLAIM_EVENT)) {
                    if(event.getCampos() != null && event.getCampos().size() > 0) {
                        showClaimFieldsDialog();
                        return;
                    }
                }
                showPinScreen(null);
            }
        });
        mainLayout = (FrameLayout) rootView.findViewById(R.id.mainLayout);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha( 0);
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


    public void onClickSubject(View v) {
        Log.d(TAG + ".onClickSubject(...)", " - onClickSubject");
        if(event != null && event.getSubject() != null &&
                event.getSubject().length() > MAX_SUBJECT_SIZE) {
            AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.subject_lbl));
            builder.setMessage(event.getSubject());
            builder.show();
        }
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
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
        Log.d(TAG + ".setPin(...)", " --- setPin");
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
        if(processSignatureTask != null) processSignatureTask.cancel(true);
        processSignatureTask = new ProcessSignatureTask(pin);
        processSignatureTask.execute();
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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
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
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
    }


    private void showClaimFieldsDialog() {
        Log.d(TAG + ".showClaimFieldsDialog(...)", " - showClaimFieldsDialog");
        if (event.getCampos() == null) {
            Log.d(TAG + ".showClaimFieldsDialog(...)", " - claim without fields");
            return;
        }
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ScrollView mScrollView = (ScrollView) inflater.inflate(R.layout.claim_dinamic_form,
                (ViewGroup) getActivity().getCurrentFocus());
        LinearLayout mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);
        final TextView errorMsgTextView = (TextView) mScrollView.findViewById(R.id.errorMsg);
        errorMsgTextView.setVisibility(View.GONE);
        Set<OptionVS> campos = event.getCampos();

        mapaCamposReclamacion = new HashMap<Integer, EditText>();
        for (OptionVS campo : campos) {
            addFormField(campo.getContent(), InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    mFormView, campo.getId().intValue());
        }
        builder.setTitle(R.string.eventfields_dialog_caption).setView(mScrollView).
                setPositiveButton(getString(R.string.aceptar_button), null).
                setNegativeButton(R.string.cancelar_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) { }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();//to get positiveButton this must be called first
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View onClick) {
                Set<OptionVS> campos = event.getCampos();
                for (OptionVS campo : campos) {
                    EditText editText = mapaCamposReclamacion.get(campo.getId().intValue());
                    String fieldValue = editText.getText().toString();
                    if ("".equals(fieldValue)) {
                        errorMsgTextView.setVisibility(View.VISIBLE);
                        return;
                    } else campo.setValue(fieldValue);
                    Log.d(TAG + " - claim field dialog", " - campo id: " + campo.getId() + " - text: " + fieldValue);
                }
                dialog.dismiss();
                showPinScreen(null);
            }
        });
    }



    private void addFormField(String label, int type, LinearLayout mFormView, int id) {
        Log.d(TAG + ".addFormField(...)", " - addFormField - field: " + label);
        TextView tvLabel = new TextView(getActivity());
        tvLabel.setLayoutParams(getDefaultParams(true));
        tvLabel.setText(label);

        EditText editView = new EditText(getActivity());
        editView.setLayoutParams(getDefaultParams(false));
        // setting an unique id is important in order to save the state
        // (content) of this view across screen configuration changes
        editView.setId(id);
        editView.setInputType(type);

        mFormView.addView(tvLabel);
        mFormView.addView(editView);

        mapaCamposReclamacion.put(id, editView);
    }


    private LinearLayout.LayoutParams getDefaultParams(boolean isLabel) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isLabel) {
            params.bottomMargin = 5;
            params.topMargin = 10;
        }
        params.leftMargin = 20;
        params.rightMargin = 20;
        return params;
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", " - caption: " + caption + "  - showMessage: " +
                message + " - isDestroyed: " + isDestroyed);
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setTitle(caption).setMessage(message).show();
    }


    private class ProcessSignatureTask extends AsyncTask<URL, Integer, ResponseVS> {

        private String pin = null;

        public ProcessSignatureTask(String pin) {
            this.pin = pin;
        }

        protected ResponseVS doInBackground(URL... urls) {
            Log.d(TAG + ".ProcessSignatureTask.doInBackground(...)",
                    " - doInBackground - event type: " + event.getTypeVS());
            try {
                ResponseVS responseVS = null;
                byte[] keyStoreBytes = null;
                FileInputStream fis = getActivity().openFileInput(KEY_STORE_FILE);
                keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                if(event.getTypeVS().equals(TypeVS.MANIFEST_EVENT)) {
                    PDFSignedSender PDFSignedSender = new PDFSignedSender(
                            contextVS.getAccessControlVS().getEventVSManifestURL(event.getEventVSId()),
                            contextVS.getAccessControlVS().getEventVSManifestCollectorURL(event.getEventVSId()),
                            keyStoreBytes, pin.toCharArray(), null, null,
                            getActivity().getBaseContext());
                    responseVS = PDFSignedSender.call();
                } else if(event.getTypeVS().equals(TypeVS.CLAIM_EVENT)) {
                    String subject = ASUNTO_MENSAJE_FIRMA_DOCUMENTO + event.getSubject();
                    JSONObject signatureContent = event.getSignatureContentJSON();
                    signatureContent.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE);
                    String serviceURL = contextVS.getAccessControlVS().getEventVSClaimCollectorURL();
                    boolean isEncryptedResponse = false;
                    SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                            signatureContent.toString(), subject, isEncryptedResponse,keyStoreBytes,
                            pin.toCharArray(), contextVS.getAccessControlVS().getCertificate(),
                            getActivity().getBaseContext());
                    responseVS = smimeSignedSender.call();
                }
                return responseVS;
            } catch(Exception ex) {
                ex.printStackTrace();
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
            }
        }

        protected void onPreExecute() {
            Log.d(TAG + ".ProcessSignatureTask.onPreExecute(...)", " --- onPreExecute");
            getActivity().getWindow().getDecorView().findViewById(
                    android.R.id.content).invalidate();
            showProgress(true, true);
            firmarEnviarButton.setEnabled(false);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(ResponseVS response) {
            Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", " - onPostExecute - status:" +
                    response.getStatusCode());
            showProgress(false, true);
            if(ResponseVS.SC_OK == response.getStatusCode()) {
                Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", "- response.getMessage():" +
                        response.getMessage());
                showMessage(getString(R.string.operacion_ok_msg), getString(R.string.operacion_ok_msg));
            } else {
                showMessage(getString(R.string.error_lbl), response.getMessage());
                firmarEnviarButton.setEnabled(true);
            }
        }

    }
}

