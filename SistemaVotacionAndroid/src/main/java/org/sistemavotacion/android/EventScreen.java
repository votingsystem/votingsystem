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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import org.sistemavotacion.android.ui.CertNotFoundDialog;
import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.callable.SignedPDFSender;
import org.sistemavotacion.modelo.CampoDeEvento;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.sistemavotacion.android.AppData.ASUNTO_MENSAJE_FIRMA_DOCUMENTO;
import static org.sistemavotacion.android.AppData.KEY_STORE_FILE;
import static org.sistemavotacion.android.AppData.MAX_SUBJECT_SIZE;

public class EventScreen extends ActionBarActivity implements CertPinDialogListener {
	
	public static final String TAG = "EventScreen";
	
    private Button firmarEnviarButton;
    private Evento evento =  null;
    private Map<Integer, EditText> mapaCamposReclamacion;
    private AppData appData;
    private View progressContainer;
    private FrameLayout mainLayout;
    private boolean isProgressShown;
    private boolean isDestroyed = true;
    private ProcessSignatureTask processSignatureTask;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_screen);
        appData = AppData.getInstance(getBaseContext());
		evento = appData.getEvent();
        if(evento == null) return;
		TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
		String subject = evento.getAsunto();
		if(subject != null && subject.length() > MAX_SUBJECT_SIZE) 
			subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
		asuntoTextView.setText(subject);
		TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		contenidoTextView.setText(Html.fromHtml(evento.getContenido()));
		contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
		firmarEnviarButton = (Button) findViewById(R.id.firmar_enviar_button);
        if (!evento.estaAbierto()) {
            Log.d(TAG + ".onCreate(..)", " - Event closed");
        	firmarEnviarButton.setEnabled(false);
        }
        firmarEnviarButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	Log.d(TAG + "- firmarEnviarButton -", " - estado: " + appData.getEstado().toString());
            	if (!AppData.Estado.CON_CERTIFICADO.equals(appData.getEstado())) {
            		Log.d(TAG + "-firmarEnviarButton-", " - showCertNotFoundDialog");
            		showCertNotFoundDialog();
            		return;
            	}
            	if (evento.getTipo().equals(Tipo.EVENTO_RECLAMACION)) {
            		if(evento.getCampos() != null && evento.getCampos().size() > 0) {
                		showClaimFieldsDialog();
                        return;
            		}
            	}
            	showPinScreen(null);
            }
        });
        String subtTitle = null;
        switch(evento.getTipo()) {
            case EVENTO_FIRMA:
                getSupportActionBar().setLogo(R.drawable.manifest_32);
                switch(evento.getEstadoEnumValue()) {
                    case ACTIVO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin())));
                        break;
                    case PENDIENTE_COMIENZO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_pendind_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
                        break;
                    case CANCELADO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_pendind_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case FINALIZADO:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
                        break;
                    default:
                        getSupportActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                }
                break;
            case EVENTO_RECLAMACION:
                getSupportActionBar().setLogo(R.drawable.filenew_32);
                switch(evento.getEstadoEnumValue()) {
                    case ACTIVO:
                        getSupportActionBar().setTitle(getString(R.string.claim_open_lbl,
                                DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin())));
                        break;
                    case PENDIENTE_COMIENZO:
                        getSupportActionBar().setTitle(getString(R.string.claim_pending_lbl));
                        break;
                    case CANCELADO:
                        getSupportActionBar().setTitle(getString(R.string.claim_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case FINALIZADO:
                        setTitle(getString(R.string.claim_closed_lbl));
                        subtTitle = getString(R.string.inicio_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaInicio()) + " - " +
                                "" + getString(R.string.fin_lbl) + ": " +
                                DateUtils.getShortSpanishStringFromDate(evento.getFechaFin());
                    default:
                        getSupportActionBar().setTitle(getString(R.string.claim_closed_lbl));
                }
                break;
        }
        if(subtTitle != null) getSupportActionBar().setSubtitle(subtTitle);
        mainLayout = (FrameLayout) findViewById( R.id.mainLayout);
        progressContainer = findViewById(R.id.progressContainer);
        mainLayout.getForeground().setAlpha( 0);
        isProgressShown = false;
        isDestroyed = false;
	}
	
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Intent intent = new Intent(this, NavigationDrawer.class);
	    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    		startActivity(intent);            
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}
	
	public void onClickSubject(View v) {
		Log.d(TAG + ".onClickSubject(...)", " - onClickSubject");
		if(evento != null && evento.getAsunto() != null &&
				evento.getAsunto().length() > MAX_SUBJECT_SIZE) {
	    	AlertDialog.Builder builder= new AlertDialog.Builder(EventScreen.this);
			builder.setTitle(getString(R.string.subject_lbl));
			builder.setMessage(evento.getAsunto());
			builder.show();	
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
	
	private void showClaimFieldsDialog() {
        Log.d(TAG + ".showClaimFieldsDialog(...)", " - showClaimFieldsDialog");
		if (evento.getCampos() == null) {
            Log.d(TAG + ".showClaimFieldsDialog(...)", " - claim without fields");
            return;
        }
    	AlertDialog.Builder builder= new AlertDialog.Builder(EventScreen.this);
        LayoutInflater inflater = getLayoutInflater();
        ScrollView mScrollView = (ScrollView) inflater.inflate(R.layout.claim_dinamic_form,
                (ViewGroup) getCurrentFocus());
        LinearLayout mFormView = (LinearLayout) mScrollView.findViewById(R.id.form);
        final TextView errorMsgTextView = (TextView) mScrollView.findViewById(R.id.errorMsg);
        errorMsgTextView.setVisibility(View.GONE);
        Set<CampoDeEvento> campos = evento.getCampos();

        mapaCamposReclamacion = new HashMap<Integer, EditText>();
		for (CampoDeEvento campo : campos) {
            addFormField(campo.getContenido(), InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    mFormView, campo.getId().intValue());
		}
		builder.setTitle(R.string.dialogo_campos_reclamacion_caption).setView(mScrollView).
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
                Set<CampoDeEvento> campos = evento.getCampos();
                for (CampoDeEvento campo : campos) {
                    EditText editText = mapaCamposReclamacion.get(campo.getId().intValue());
                    String valorCampo = editText.getText().toString();
                    if ("".equals(valorCampo)) {
                        errorMsgTextView.setVisibility(View.VISIBLE);
                        return;
                    } else campo.setValor(valorCampo);
                    Log.d(TAG + " - claim field dialog", " - campo id: " + campo.getId() + " - text: " + valorCampo);
                }
                dialog.dismiss();
                showPinScreen(null);
            }
        });
	}

    private void addFormField(String label, int type, LinearLayout mFormView, int id) {
        Log.d(TAG + ".addFormField(...)", " - addFormField - field: " + label);
        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(getDefaultParams(true));
        tvLabel.setText(label);

        EditText editView = new EditText(this);
        editView.setLayoutParams(getDefaultParams(false));
        // setting an unique id is important in order to save the state
        // (content) of this view across screen configuration changes
        editView.setId(id);
        editView.setInputType(type);

        mFormView.addView(tvLabel);
        mFormView.addView(editView);

        mapaCamposReclamacion.put(id, editView);
    }

    private LayoutParams getDefaultParams(boolean isLabel) {
        LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
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
        if(isDestroyed) return;
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle(caption).setMessage(message).show();
	}
	
    private void showPinScreen(String message) {
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
		Log.d(TAG + ".setPin(...)", " --- setPin"); 
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

    private class ProcessSignatureTask extends AsyncTask<URL, Integer, Respuesta> {

        private String pin = null;

        public ProcessSignatureTask(String pin) {
            this.pin = pin;
        }

        protected Respuesta doInBackground(URL... urls) {
            Log.d(TAG + ".ProcessSignatureTask.doInBackground(...)",
                    " - doInBackground - event type: " + evento.getTipo());
            try {
                Respuesta respuesta = null;
                byte[] keyStoreBytes = null;
                FileInputStream fis = openFileInput(KEY_STORE_FILE);
                keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
                if(evento.getTipo().equals(Tipo.EVENTO_FIRMA)) {
                    SignedPDFSender signedPDFSender = new SignedPDFSender(ServerPaths.getURLPDFManifest(
                            appData.getAccessControlURL(), evento.getEventoId()),
                            ServerPaths.getURLPDFManifestCollector(appData.getAccessControlURL(),
                                    evento.getEventoId()), keyStoreBytes, pin.toCharArray(), null, null, getBaseContext());
                    respuesta = signedPDFSender.call();
                } else if(evento.getTipo().equals(Tipo.EVENTO_RECLAMACION)) {
                    String subject = ASUNTO_MENSAJE_FIRMA_DOCUMENTO + evento.getAsunto();
                    String signatureContent = evento.getSignatureContentJSON();
                    String serviceURL = ServerPaths.getURLReclamacion(
                            appData.getAccessControlURL());
                    boolean isEncryptedResponse = false;
                    SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL,
                            signatureContent, subject, isEncryptedResponse, keyStoreBytes, pin.toCharArray(),
                            appData.getControlAcceso().getCertificado(), getBaseContext());
                    respuesta = smimeSignedSender.call();
                }
                return respuesta;
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
            firmarEnviarButton.setEnabled(false);
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) { }

        protected void onPostExecute(Respuesta response) {
            Log.d(TAG + ".ProcessSignatureTask.onPostExecute(...)", " - onPostExecute - status:" +
                    response.getCodigoEstado());
            showProgress(false, true);
            if(Respuesta.SC_OK == response.getCodigoEstado()) {
                showMessage(getString(R.string.operacion_ok_msg), response.getMensaje());
            } else {
                showMessage(getString(R.string.error_lbl), response.getMensaje());
                firmarEnviarButton.setEnabled(true);
            }
        }

    }
}