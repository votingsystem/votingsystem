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

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.ASUNTO_MENSAJE_FIRMA_DOCUMENTO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.MAX_SUBJECT_SIZE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sistemavotacion.android.ui.CertPinScreen;
import org.sistemavotacion.android.ui.CertPinScreenCallback;
import org.sistemavotacion.json.DeObjetoAJSON;
import org.sistemavotacion.modelo.CampoDeEvento;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.FileListener;
import org.sistemavotacion.task.GetFileTask;
import org.sistemavotacion.task.SendFileTask;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.PdfUtils;
import org.sistemavotacion.util.ServerPaths;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.itextpdf.text.pdf.PdfReader;

public class EventScreen extends 
		SherlockActivity implements CertPinScreenCallback {
	
	public static final String TAG = "EventScreen";

    private Button firmarEnviarButton;
    private Evento evento;
    private Map<Long, EditText> mapaCamposReclamacion;
    private ProgressDialog progressDialog = null;
    private AlertDialog certPinDialog;
    private AsyncTask runningTask = null;
    byte[] keyStoreBytes = null;
	
    
    DataListener<String> sendFileListener = new DataListener<String>() {

    	@Override public void updateData(int codigoEstado, String data) {
			Log.d(TAG + ".sendFileListener.updateData(...) ", " - data: " + data);
	        if (progressDialog != null && progressDialog.isShowing()) {
	            progressDialog.dismiss();
	        }

	        if(Respuesta.SC_OK == codigoEstado) {
	        	showMessage(getString(R.string.operacion_ok_msg), null);
	        } else showMessage(getString(R.string.error_lbl), data);
		}
		
    	@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".sendFileListener.setException() ", "exceptionMsg: " + exceptionMsg);	
	        showMessage(getString(R.string.error_lbl), exceptionMsg);
	        firmarEnviarButton.setEnabled(true);
		}
    };

    
	@Override
	public void onCreate(Bundle savedInstanceState) {
        setTheme(Aplicacion.THEME);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_screen);
		evento = Aplicacion.INSTANCIA.getEventoSeleccionado();
		setTitle(evento);
		TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
		String subject = evento.getAsunto();
		if(subject != null && subject.length() > MAX_SUBJECT_SIZE) 
			subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
		asuntoTextView.setText(subject);
		TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
		contenidoTextView.setText(Html.fromHtml(evento.getContenido()));
		contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
		firmarEnviarButton = (Button) findViewById(R.id.firmar_enviar_button);
        if (!evento.estaAbierto()) {
        	firmarEnviarButton.setEnabled(false);
        } else Log.d(TAG + ".onCreate(..)", "Evento cerrado");
        firmarEnviarButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	Log.d(TAG + "-firmarEnviarButton-", " - estado: " + Aplicacion.INSTANCIA.getEstado().toString());
            	if (!Aplicacion.Estado.CON_CERTIFICADO.equals(Aplicacion.INSTANCIA.getEstado())) {
            		Log.d(TAG + "-firmarEnviarButton-", "mostrando dialogo certificado no encontrado");
            		showCertNotFoundDialog();
            		return;
            	}
            	if (evento.getTipo().equals(Tipo.EVENTO_RECLAMACION)) {
            		if(evento.getCampos() != null && evento.getCampos().size() > 0) {
                		showClaimFieldsDialog();	
            		} else showPinScreen(null); 
            		return;
            	}
            	showPinScreen(null);
            }
        });
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

	private void setTitle(Evento event) {
		switch(evento.getTipo()) {
			case EVENTO_FIRMA:
				switch(evento.getEstadoEnumValue()) {
					case ACTIVO:
						setTitle(getString(R.string.manifest_open_lbl, 
								DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin())));
						break;
					case PENDIENTE_COMIENZO:
						setTitle(getString(R.string.manifest_pendind_lbl));
						break;
					default:
						setTitle(getString(R.string.manifest_closed_lbl));
				}
				break;
			case EVENTO_RECLAMACION:
				switch(evento.getEstadoEnumValue()) {
					case ACTIVO:
						setTitle(getString(R.string.claim_open_lbl, 
								DateUtils.getElpasedTimeHoursFromNow(evento.getFechaFin())));
						break;
					case PENDIENTE_COMIENZO:
						setTitle(getString(R.string.claim_pending_lbl));
						break;
					default:
						setTitle(getString(R.string.claim_closed_lbl));
				}
				break;
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
	
	private void firmarEnviarSMIME(char[] password) throws Exception {
		String asunto = ASUNTO_MENSAJE_FIRMA_DOCUMENTO + evento.getAsunto();
        String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
        String urlEnvio = null;
        if (evento.getTipo().equals(Tipo.EVENTO_FIRMA)) {
        	urlEnvio = ServerPaths.getURLEventoFirmado(CONTROL_ACCESO_URL);
        } else if (evento.getTipo().equals(Tipo.EVENTO_RECLAMACION)) {
        	urlEnvio = ServerPaths.getURLReclamacion(CONTROL_ACCESO_URL);
        }
		SignedMailGenerator dnies = 
				new SignedMailGenerator(keyStoreBytes, ALIAS_CERT_USUARIO, password);
		String signatureContent = DeObjetoAJSON.obtenerFirmaParaEventoJSON(evento);
		File simimeSignedFile = dnies.genFile(usuario, 
				Aplicacion.getControlAcceso().getNombreNormalizado(), 
				signatureContent, asunto, null, SignedMailGenerator.Type.USER);
        SendFileTask enviarArchivoTask = new SendFileTask(sendFileListener, simimeSignedFile);
        runningTask = enviarArchivoTask;
        enviarArchivoTask.execute(urlEnvio);
	}
	
	private void firmarEnviar(final char[] password) {    
		try {
	    	if(evento.getTipo().equals(Tipo.EVENTO_FIRMA)) {
		        firmarEnviarButton.setEnabled(false);
		        FileListener pdfSigner = new FileListener () {
		    		@Override
		    		public void porcessFileData(byte[] fileData) {
		    			File root = Environment.getExternalStorageDirectory();
		    			File pdfFirmadoFile = new File(root, 
		    					Aplicacion.MANIFEST_FILE_NAME + "_" + evento.getEventoId() +".pdf");
		    	        PdfReader pdfFile;
		    	        try {
			    	        pdfFile = new PdfReader(fileData);
			            	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			                PrivateKey key = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
			                Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
			    			PdfUtils.firmar(pdfFile, new FileOutputStream(pdfFirmadoFile), key, chain);
			    	        SendFileTask enviarArchivoTask = new SendFileTask(sendFileListener, pdfFirmadoFile);
			    	        runningTask = enviarArchivoTask.execute(ServerPaths.getURLPDFManifestCollector(
			    	        		CONTROL_ACCESO_URL, evento.getEventoId()));
		    	        } catch (IOException ex) {
		    				Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
		    				showMessage(getString(R.string.error_lbl), 
		    						getString(R.string.pin_error_msg));
		    			} catch (Exception ex) {
		    				Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
		    				showMessage(getString(R.string.error_lbl), ex.getMessage());
		    			}
		    		}

		    		@Override
		    		public void setException(String exceptionMsg) {
		    			Log.d(TAG + ".fileListener.setException() ", "exceptionMsg: " + exceptionMsg);	
		    	        showMessage(getString(R.string.error_lbl), exceptionMsg);
		    		}};

	    		GetFileTask getFileTask = new GetFileTask(pdfSigner);
	    		runningTask = getFileTask.execute(ServerPaths.getURLPDFManifest(
		    			CONTROL_ACCESO_URL, evento.getEventoId()));

				
	    	} else {
		        firmarEnviarButton.setEnabled(false);
		        firmarEnviarSMIME(password);
	    	} 
		} catch (IOException ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			showMessage(getString(R.string.error_lbl), 
					getString(R.string.pin_error_msg));
		} catch (Exception ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			showMessage(getString(R.string.error_lbl), ex.getMessage());
		}
        
	}
	
	private void showCertNotFoundDialog() {
    	AlertDialog.Builder builder= new AlertDialog.Builder(EventScreen.this);
		builder.setTitle(getString(R.string.error_lbl))
			.setMessage(Html.fromHtml(getString(R.string.certificado_no_encontrado_msg)))
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
	
	private void showEmptyFieldsDialog() {
    	AlertDialog.Builder builder= new AlertDialog.Builder(EventScreen.this);
		builder.setTitle(getString(R.string.error_lbl))
			.setMessage(Html.fromHtml(getString(R.string.campos_vacios_msg)));
		builder.setPositiveButton(getString(R.string.ok_button), null).show();
	}
	
	private void showClaimFieldsDialog() {
		if (evento.getCampos() == null) return;
    	AlertDialog.Builder builder= new AlertDialog.Builder(EventScreen.this);
    	LinearLayout linearLayout = new LinearLayout(this);
        Set<CampoDeEvento> campos = evento.getCampos();
        mapaCamposReclamacion = new HashMap<Long, EditText>();
		for (CampoDeEvento campo : campos) {
			TextView textView = new TextView(this);
			textView.setText(campo.getContenido());
			textView.setId(Long.valueOf(campo.getId()).intValue());
			textView.setLayoutParams(new LayoutParams(
		            LayoutParams.MATCH_PARENT,
		            LayoutParams.WRAP_CONTENT));
			
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)textView.getLayoutParams();
			params.setMargins(20, 0, 20, 0); //left, top, right, bottom
			textView.setLayoutParams(params);
			
			linearLayout.addView(textView);
			EditText editText = new EditText(this);
			editText.setLayoutParams(new LayoutParams(
		            LayoutParams.MATCH_PARENT,
		            LayoutParams.WRAP_CONTENT));
			
			LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams)editText.getLayoutParams();
			params1.setMargins(20, 0, 20, 0); //left, top, right, bottom
			editText.setLayoutParams(params1);
			
			mapaCamposReclamacion.put(campo.getId(), editText);
			linearLayout.addView(editText);
		}
		builder.setTitle(R.string.dialogo_campos_reclamacion_caption).setView(linearLayout)
			.setPositiveButton(R.string.aceptar_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Set<CampoDeEvento> campos = evento.getCampos();
					for (CampoDeEvento campo : campos) {
						EditText editText = mapaCamposReclamacion.get(campo.getId());
						String valorCampo = editText.getText().toString();
						if ("".equals(valorCampo)) {
							showEmptyFieldsDialog();
							return;
						} else campo.setValor(valorCampo);
						Log.d(TAG + ".dialogo_campos_reclamacion", "campo id: " + campo.getId() + " - text: " + valorCampo);
					}
					showPinScreen(null);          		  
				}
			})
			.setNegativeButton(R.string.cancelar_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) { }
			}).show();
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
    			EventScreen.this);
    	CertPinScreen certPinScreen = new CertPinScreen(
    			getApplicationContext(), EventScreen.this);
    	if(message != null) certPinScreen.setMessage(message);
    	builder.setView(certPinScreen);
    	certPinDialog = builder.create();
    	certPinDialog.show();
    }

	@Override public void setPin(final String pin) {
		if(pin != null) {
	        progressDialog = ProgressDialog.show(EventScreen.this, 
	        		getString(R.string.back_to_cancel_lbl), 
	        		getString(R.string.sending_data_lbl), true,
		            true, new DialogInterface.OnCancelListener() {
		                @Override
		                public void onCancel(DialogInterface dialog) { 
		                	Log.d(TAG + ".ProgressDialog", "cancelando tarea"); 
		                	if(runningTask != null) runningTask.cancel(true);
		                }
	        		});
	        firmarEnviar(pin.toCharArray());
		} 
		certPinDialog.dismiss();
	}
	
}