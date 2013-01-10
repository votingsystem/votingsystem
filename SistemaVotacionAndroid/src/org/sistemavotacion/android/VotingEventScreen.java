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

import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.MAX_SUBJECT_SIZE;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sistemavotacion.android.ui.CertPinScreen;
import org.sistemavotacion.android.ui.CertPinScreenCallback;
import org.sistemavotacion.json.DeObjetoAJSON;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionDeEvento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.GetVotingCertTask;
import org.sistemavotacion.task.SendDataTask;
import org.sistemavotacion.task.VotingListener;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.VotacionHelper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class VotingEventScreen extends 
		SherlockActivity implements CertPinScreenCallback, VotingListener {
	
	public static final String TAG = "VotingEventScreen";
	


    private Evento evento;
    private ProgressDialog progressDialog = null;
    private PKCS10WrapperClient pkcs10WrapperClient = null;
    private AlertDialog certPinDialog;
    private AsyncTask runningTask = null;
    private List<Button> optionButtons = null;
    byte[] keyStoreBytes = null;
	
    DataListener<String> certificadoVotoListener = new DataListener<String>() {
    	
		@Override public void updateData(int statusCode, String data) {
			Log.d(TAG + ".certificadoVotoListener.updateData(...) ", data);
	        if (progressDialog != null && progressDialog.isShowing()) {
	            progressDialog.dismiss();
	        }
	        if(Respuesta.SC_OK == statusCode) {
		        try {
		        	/*pkcs10WrapperClient.initVoteSigner(data.getBytes());
		            String votoJSON = DeObjetoAJSON.obtenerVotoParaEventoJSON(evento);
		            String usuario = null;
		            if (Aplicacion.getUsuario() != null) usuario = 
		            		Aplicacion.getUsuario().getNif();
		            String votoFirmado = pkcs10WrapperClient.genSignedString(usuario, 
		                    evento.getCentroControl().getNombreNormalizado(),
		                    votoJSON, "[VOTO]", null, SignedMailGenerator.Type.USER);
			    	new SendDataTask(votingListener, votoFirmado, true).execute(ServerPaths.getURLVoto(
			    			evento.getCentroControl().getServerURL()));*/
				} catch (Exception e) {
					Log.e(TAG + "VotacionHelper.obtenerCSR(...)", "Message: " + e.getMessage());
					e.printStackTrace();
					setException(e.getMessage());
					setOptionButtonsEnabled(true);
				}	
	        } else {
	        	showMessage(getString(R.string.error_lbl), data);
	        }
		}
		
		@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".certificadoVotoListener.setException() ", exceptionMsg);	
			showMessage(getString(R.string.error_lbl), exceptionMsg);
		}
    };

    DataListener<String> votingListener = new DataListener<String>() {

    	@Override public void updateData(int statusCode, String response) {
			Log.d(TAG + ".votingListener.updateData(...) ",	" - statusCode: " + statusCode);
	        if (progressDialog != null && progressDialog.isShowing()) {
	            progressDialog.dismiss();
	        }
	        if (Respuesta.SC_OK == statusCode) {
                try {
					SMIMEMessageWrapper votoValidado = new SMIMEMessageWrapper(null,
							new ByteArrayInputStream(response.getBytes()), null);
					ReciboVoto  reciboVoto = new ReciboVoto(
							Respuesta.SC_OK, votoValidado, evento);
					showMessage(getString(R.string.operacion_ok_msg), 
							reciboVoto.getMensaje());
				} catch (Exception e) {
					e.printStackTrace();
					setException("Error validando recibo de voto: " + e.getMessage());
					setOptionButtonsEnabled(true);
				}
	        } else setOptionButtonsEnabled(true);
		}
		
    	@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".votingListener.setException(...) ", " - exceptionMsg: " + exceptionMsg);	
	        showMessage(getString(R.string.error_lbl), exceptionMsg);
		}
    };
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
        setTheme(Aplicacion.THEME);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_screen);
		evento = Aplicacion.INSTANCIA.getEventoSeleccionado();
		switch(evento.getEstadoEnumValue()) {
			case ACTIVO:
				setTitle(getString(R.string.voting_open_lbl, 
						DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin())));
				break;
			case PENDIENTE_COMIENZO:
				setTitle(getString(R.string.voting_pending_lbl));
				break;
			default:
				setTitle(getString(R.string.voting_closed_lbl));
		}
		TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
		String subject = evento.getAsunto();
		if(subject != null && subject.length() > MAX_SUBJECT_SIZE) 
			subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
		asuntoTextView.setText(subject);
		TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
		contenidoTextView.setText(Html.fromHtml(evento.getContenido()));
		contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
		Set<OpcionDeEvento> opciones = evento.getOpciones();
		LinearLayout linearLayout = (LinearLayout)findViewById(R.id.contenedor_evento);
		optionButtons = new ArrayList<Button>();
		FrameLayout.LayoutParams paramsButton = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		paramsButton.setMargins(10, 10, 10, 10);
	    
		for (final OpcionDeEvento opcion:opciones) {
			Button opcionButton = new Button(this);
			opcionButton.setText(opcion.getContenido());
			opcionButton.setOnClickListener(new Button.OnClickListener() {
				
				OpcionDeEvento opcionSeleccionada = opcion;
				
	            public void onClick(View v) {
	            	Log.d(TAG + "- opcionButton - opcionId: " + 
	            			opcionSeleccionada.getId(), "estado: " + 
	            			Aplicacion.INSTANCIA.getEstado().toString());
	            	evento.setOpcionSeleccionada(opcionSeleccionada);
	            	if (!Aplicacion.Estado.CON_CERTIFICADO.equals(Aplicacion.INSTANCIA.getEstado())) {
	            		Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
	            		showCertNotFoundDialog();
	            		return;
	            	} else showPinScreen(null);
	            }
	        });
			optionButtons.add(opcionButton);
			if (!evento.estaAbierto()) opcionButton.setEnabled(false);
			linearLayout.addView(opcionButton, paramsButton);			   
		}
		((Button)findViewById(R.id.firmar_enviar_button)).setVisibility(View.GONE);
		if (Aplicacion.Estado.CON_CERTIFICADO.equals(Aplicacion.INSTANCIA.getEstado())) {
			FileInputStream fis = null;
			try {
				fis = openFileInput(KEY_STORE_FILE);
				keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
			} catch (Exception e) {
				e.printStackTrace();
				showMessage(getString(R.string.error_lbl), e.getMessage());
			}	
		}
	}

	private void showCertNotFoundDialog() {
    	AlertDialog.Builder builder= new AlertDialog.Builder(VotingEventScreen.this);
		builder.setTitle(getString(R.string.error_lbl))
			.setMessage(Html.fromHtml(getString(
					R.string.certificado_no_encontrado_msg)))
			.setPositiveButton(R.string.solicitar_label, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	Intent intent = null;
	          	  	switch(Aplicacion.INSTANCIA.getEstado()) {
	          	  		case CON_CSR:
	          	  			intent = new Intent(getApplicationContext(), UserCertResponseForm.class);
	          	  			break;
	          	  		case SIN_CSR:
	          	  			intent = new Intent(getApplicationContext(), UserCertRequestForm.class);
	          	  			break;
	          	  	}
	          	  	if(intent != null) {
		          	  	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		          	    startActivity(intent);
	          	  	}
	            }
				})
			.setNegativeButton(R.string.cancelar_button, null).show();
	}
	
	public void onClickSubject(View v) {
		Log.d(TAG + ".onClickSubject(...)", " - onClickSubject");
		if(evento != null && evento.getAsunto() != null &&
				evento.getAsunto().length() > MAX_SUBJECT_SIZE) {
	    	AlertDialog.Builder builder= new AlertDialog.Builder(VotingEventScreen.this);
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
	
	private void firmarEnviarVoto(char[] password) {
		try {
			setOptionButtonsEnabled(false);
			VotacionHelper.procesarVoto(getApplicationContext(), evento, this, 
					keyStoreBytes, password);

	    	/*VotacionHelper.prepararVoto(evento);
	    	Log.d(TAG + ".firmarEnviarVoto(...)", " - HashCertificadoVotoHex:" + evento.getHashCertificadoVotoHex());
	    	pkcs10WrapperClient = PKCS10WrapperClient.buildCSRVoto(KEY_SIZE, SIG_NAME, 
			        SIGNATURE_ALGORITHM, PROVIDER, CONTROL_ACCESO_URL, 
			        evento.getEventoId().toString(), evento.getHashCertificadoVotoHex());
			String csr = new String(pkcs10WrapperClient.getPEMEncodedRequestCSR());
			Log.d(TAG  + ".firmarEnviarVoto(...)", " - csr:" + csr);
	    	File solicitudAcceso = VotacionHelper.obtenerSolicitudAcceso(keyStoreBytes, evento, password);
	    	Log.d(TAG + ".firmarEnviar(...)", " - solicitudAcceso: " 
	    			+ FileUtils.getStringFromFile(solicitudAcceso));
	        GetVotingCertTask obtenerCertificadoVotoTask = new GetVotingCertTask(
	        		certificadoVotoListener, solicitudAcceso, csr.getBytes());
	        runningTask = obtenerCertificadoVotoTask;
	        obtenerCertificadoVotoTask.execute(ServerPaths.getURLSolicitudAcceso(CONTROL_ACCESO_URL));*/
		} catch (IOException ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			showMessage(getString(R.string.error_lbl), 
					getString(R.string.pin_error_msg));
		} catch (Exception ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			showMessage(getString(R.string.error_lbl), ex.getMessage());
		}
		setOptionButtonsEnabled(true);
	}

	
	private void showMessage(String caption, String message) {
		Log.d(TAG + ".showMessage(...) ", " - caption: " 
				+ caption + "  - showMessage: " + message);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setTitle(caption).setMessage(message).show();
	}
	
    private void showPinScreen(String message) {
    	AlertDialog.Builder builder= new AlertDialog.Builder(
    			VotingEventScreen.this);
    	CertPinScreen certPinScreen = new CertPinScreen(
    			getApplicationContext(), VotingEventScreen.this);
    	if(message != null) certPinScreen.setMessage(message);
    	builder.setView(certPinScreen);
    	certPinDialog = builder.create();
    	certPinDialog.show();
    }

	@Override public void setPin(final String pin) {
		if(pin != null) {
	        progressDialog = ProgressDialog.show(VotingEventScreen.this, 
	        		getString(R.string.back_to_cancel_lbl), 
	        		getString(R.string.sending_data_lbl), true,
		            true, new DialogInterface.OnCancelListener() {
		                @Override
		                public void onCancel(DialogInterface dialog) { 
		                	Log.d(TAG + ".ProgressDialog", "cancelando tarea"); 
		                	if(runningTask != null) runningTask.cancel(true);
		                }
	        		});
	        firmarEnviarVoto(pin.toCharArray());
		} 
		certPinDialog.dismiss();
	}

	@Override
	public void proccessAccessRequest(File accessRequest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void proccessReceipt(ReciboVoto receipt) {
		showMessage(getString(R.string.operacion_ok_msg), 
				receipt.getMensaje());
	}

	@Override
	public void setException(String exceptionMsg) {
		showMessage(getString(R.string.error_lbl), exceptionMsg);
		setOptionButtonsEnabled(true);
	}

	@Override
	public void setRunningTask(AsyncTask runningTask) {
		this.runningTask = runningTask;
	}
	
}