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

import static org.sistemavotacion.android.Aplicacion.ASUNTO_MENSAJE_FIRMA_DOCUMENTO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.MAX_SUBJECT_SIZE;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sistemavotacion.android.service.SignService;
import org.sistemavotacion.android.service.ServiceListener;
import org.sistemavotacion.android.ui.CertNotFoundDialog;
import org.sistemavotacion.android.ui.CertPinDialog;
import org.sistemavotacion.android.ui.CertPinDialogListener;
import org.sistemavotacion.modelo.CampoDeEvento;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class EventScreen extends FragmentActivity 
	implements CertPinDialogListener, ServiceListener {
	
	public static final String TAG = "EventScreen";
	
	private static final int MANIFEST_REQUEST = 0;
	private static final int CLAIM_REQUEST    = 1;
	
    private Button firmarEnviarButton;
    private Evento evento =  null;
    private Map<Long, EditText> mapaCamposReclamacion;
    private ProgressDialog progressDialog = null;
	private Intent signServiceIntent = null;
	private SignService signService = null;
	private byte[] keyStoreBytes = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_screen);
		evento = Aplicacion.INSTANCIA.getEventoSeleccionado();
		TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
		String subject = evento.getAsunto();
		if(subject != null && subject.length() > MAX_SUBJECT_SIZE) 
			subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
		asuntoTextView.setText(subject);
		TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
		//getActionBar().setHomeButtonEnabled(true);
		try {//android api 11 I don't have this method
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".setTitle(...)", " --- android api 11 doesn't have method 'setLogo'");
		}  
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
		setTitle(evento);
	}

	private void setTitle(Evento event) {
		switch(evento.getTipo()) {
			case EVENTO_FIRMA:
				try {
					getActionBar().setLogo(R.drawable.manifest_32);	
				} catch(NoSuchMethodError ex) {
					Log.d(TAG + ".setTitle(...)", " --- android api 11 doesn't have method 'setLogo'");
				}        		
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
				try {//android api 11 I don't have this method
	        		getActionBar().setLogo(R.drawable.filenew_32);
				} catch(NoSuchMethodError ex) {
					Log.d(TAG + ".setTitle(...)", " --- android api 11 doesn't have method 'setLogo'");
				}   
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

	
	@Override public boolean onOptionsItemSelected(MenuItem item) {  
		Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
		switch (item.getItemId()) {        
	    	case android.R.id.home:  
	    		Intent intent = new Intent(this, FragmentTabsPager.class);   
	    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    		startActivity(intent);            
	    		return true;        
	    	default:            
	    		return super.onOptionsItemSelected(item);    
		}
	}
	
    /*@Override public boolean onCreateOptionsMenu(Menu menu) {
    	Intent intent = new Intent(this, FragmentTabsPager.class);
        menu.add(getString(R.string.panel_principal_lbl)).setIntent(intent)
            .setIcon(R.drawable.password_22x22)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }*/
	
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
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(Aplicacion.CERT_NOT_FOUND_DIALOG_ID);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    certDialog.show(ft, Aplicacion.CERT_NOT_FOUND_DIALOG_ID);
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
    	CertPinDialog pinDialog = CertPinDialog.newInstance(message, this, false);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag(CertPinDialog.TAG);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	    pinDialog.show(ft, CertPinDialog.TAG);
    	signServiceIntent = new Intent(this, SignService.class);
		startService(signServiceIntent);
		bindService(signServiceIntent, signServiceConnection, BIND_AUTO_CREATE);
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
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//repaint main view
				getWindow().getDecorView().findViewById(
						android.R.id.content).invalidate();
		        progressDialog = ProgressDialog.show(EventScreen.this, 
		        		getString(R.string.sending_data_caption), 
		        		getString(R.string.sending_data_lbl), true,
			            true, new DialogInterface.OnCancelListener() {
			                @Override
			                public void onCancel(DialogInterface dialog) { 
			                	Log.d(TAG + ".ProgressDialog", "cancelando tarea"); 
			                	//if(runningTask != null) runningTask.cancel(true);
			                }
		        		});
			}
		});

		Runnable processPinTask = new Runnable() {
			public void run() {
				Log.d(TAG + ".processPinTask()", 
						"--- processPinTask - processPinTask ");
				runOnUiThread(new Runnable() {
					@Override public void run() {
						if(signService == null) {
							Log.e(TAG + ".processPinTask", " - signService NULL"); 
							return;
						}
						try {
					    	if(evento.getTipo().equals(Tipo.EVENTO_FIRMA)) {
					    		signService.processTimestampedPDFSignature(MANIFEST_REQUEST,
				    					ServerPaths.getURLPDFManifest(CONTROL_ACCESO_URL, evento.getEventoId()),
				    					ServerPaths.getURLPDFManifestCollector(CONTROL_ACCESO_URL, evento.getEventoId()), 
				    					keyStoreBytes, pin.toCharArray(), EventScreen.this);
					    	} else  if (evento.getTipo().equals(Tipo.EVENTO_RECLAMACION))  {
					    		String subject = ASUNTO_MENSAJE_FIRMA_DOCUMENTO + evento.getAsunto();
					    		String urlToSendSignedDocument = ServerPaths.getURLReclamacion(CONTROL_ACCESO_URL);
					            String signatureContent = evento.getSignatureContentJSON();
					            boolean isEncryptedResponse = false;
					            signService.processSignature(CLAIM_REQUEST, signatureContent, subject, 
					    				urlToSendSignedDocument, EventScreen.this, isEncryptedResponse, 
					    				keyStoreBytes, pin.toCharArray());
					    		
					    	} 
					    	firmarEnviarButton.setEnabled(false);
						} catch (IOException ex) {
							ex.printStackTrace();
							showMessage(getString(R.string.error_lbl), 
									getString(R.string.pin_error_msg));
					        firmarEnviarButton.setEnabled(true);
						} catch (Exception ex) {
							ex.printStackTrace();
							showMessage(getString(R.string.error_lbl), ex.getMessage());
							firmarEnviarButton.setEnabled(true);
						}
					}
				});
			}
		};
			
		Handler mHandler = new Handler();
		mHandler.removeCallbacks(processPinTask);
        mHandler.postDelayed(processPinTask, 100);
	}
	
    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    	unbindSignService();
    };
	
	private void unbindSignService() {
		Log.d(TAG + ".unbindSignService()", "--- unbindSignService");
		//if(signServiceConnection != null && signServiceIntent != null) 
		if(signService != null)	unbindService(signServiceConnection);
	}
	
	private void stopSignService() {
		Log.d(TAG + ".stopSignService()", "--- stopSignService");
		if(signServiceConnection != null && signServiceIntent != null) 
			stopService(signServiceIntent);
	}
	
	ServiceConnection signServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG + ".signServiceConnection.onServiceDisconnected()", 
					" - signServiceConnection.onServiceDisconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG + ".signServiceConnection.onServiceConnected()", 
					" - signServiceConnection.onServiceConnected");
			signService = ((SignService.SignServiceBinder) service).getBinder();
		}
	};

	@Override
	public void proccessResponse(Integer requestId, Respuesta response) {
		Log.d(TAG + ".proccessResponse()", "--- proccessResponse - statusCode:" + 
				response.getCodigoEstado());
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
		String caption  = null;
		if(Respuesta.SC_OK != response.getCodigoEstado()) {
			Log.d(TAG + ".proccessResponse()", "--- proccessResponse - getMensaje:" + 
					response.getMensaje());
			caption = getString(R.string.error_lbl);
			firmarEnviarButton.setEnabled(true);
		} else {
			caption = getString(R.string.operacion_ok_msg);
		}
		showMessage(caption, response.getMensaje());
	}

}