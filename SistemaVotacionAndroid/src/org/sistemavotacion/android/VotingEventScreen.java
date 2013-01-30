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

import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.MAX_SUBJECT_SIZE;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sistemavotacion.android.service.VotingService;
import org.sistemavotacion.android.service.VotingServiceListener;
import org.sistemavotacion.android.ui.CertPinScreen;
import org.sistemavotacion.android.ui.CertPinScreenCallback;
import org.sistemavotacion.android.ui.VotingResultDialog;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionDeEvento;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class VotingEventScreen extends SherlockFragmentActivity 
	implements CertPinScreenCallback, VotingServiceListener {
	
	public static final String TAG = "VotingEventScreen";
    private static final String CERT_PIN_DIALOG = "certPinDialog";
    public static final String INTENT_EXTRA_DIALOG_PROP_NAME = "dialog";
    public static final String RECEIPT_KEY_PROP_NAME = "receiptKey";
    public static final int CANCEL_VOTE_DIALOG = 1;
    public static final int SAVE_VOTE_DIALOG = 2;
    public static final int SELECTED_OPTION_MAX_LENGTH = 27;

    private Evento evento;
    private VoteReceipt receipt;
    private ProgressDialog progressDialog = null;
    private CertPinScreen certPinScreen;
    private AsyncTask runningTask = null;
    private List<Button> optionButtons = null;
    byte[] keyStoreBytes = null;
	private Intent serviceIntent = null;
	private VotingService votingService;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG + ".onCreate(...)", " - onCreate");
        setTheme(Aplicacion.THEME);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voting_event_screen);
		try {
			getActionBar().setLogo(R.drawable.poll_22x22);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".onCreate(...)", " --- android api 11 I doesn't have method 'setLogo'");
		}  
		serviceIntent = new Intent(VotingEventScreen.this, VotingService.class);
		startService(serviceIntent);
        if(receipt != null || getIntent().getStringExtra(
        		RECEIPT_KEY_PROP_NAME) != null) {
        	Log.d(TAG + ".onCreate(...)", " - receipt key: " + 
        			getIntent().getStringExtra(RECEIPT_KEY_PROP_NAME));
        	receipt = AppData.INSTANCE.getReceipt(
        			getIntent().getStringExtra(RECEIPT_KEY_PROP_NAME));
        	evento = receipt.getVoto();
        	setReceiptScreen(receipt);
        	return;
        }
        evento = Aplicacion.INSTANCIA.getEventoSeleccionado();
        setEventScreen(evento);
	}
	
	private void setReceiptScreen(VoteReceipt receipt) {
		Log.d(TAG + ".setReceiptScreen(...)", " - setReceiptScreen");
		((LinearLayout)findViewById(R.id.receipt_buttons)).setVisibility(View.VISIBLE);
		setTitle(getString(R.string.already_voted_lbl, receipt.getVoto().
				getOpcionSeleccionada().getContenido()));
		TextView asuntoTextView = (TextView) findViewById(R.id.asunto_evento);
		String subject = receipt.getVoto().getAsunto();
		if(subject != null && subject.length() > MAX_SUBJECT_SIZE) 
			subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
		asuntoTextView.setText(subject);
		TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
		contenidoTextView.setText(Html.fromHtml(receipt.getVoto().getContenido()));
		contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
		Set<OpcionDeEvento> opciones = receipt.getVoto().getOpciones();
		LinearLayout linearLayout = (LinearLayout)findViewById(R.id.contenedor_evento);
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
		int dialog = getIntent().getIntExtra("dialog", 0);
		switch(dialog) {
			case CANCEL_VOTE_DIALOG:
				break;
			case SAVE_VOTE_DIALOG:
				break;
		}
	}
	
    @Override public boolean onCreateOptionsMenu(Menu menu) {
    	Intent intent = new Intent(this, FragmentTabsPager.class);
        menu.add(getString(R.string.panel_principal_lbl)).setIntent(intent)
            .setIcon(R.drawable.password_22x22)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }
	
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
		String subject = event.getAsunto();
		if(subject != null && subject.length() > MAX_SUBJECT_SIZE) 
			subject = subject.substring(0, MAX_SUBJECT_SIZE) + " ...";
		asuntoTextView.setText(subject);
		TextView contenidoTextView = (TextView) findViewById(R.id.contenido_evento);
		contenidoTextView.setText(Html.fromHtml(event.getContenido()));
		contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
		Set<OpcionDeEvento> opciones = event.getOpciones();
		LinearLayout linearLayout = (LinearLayout)findViewById(R.id.contenedor_evento);
		optionButtons = new ArrayList<Button>();
		FrameLayout.LayoutParams paramsButton = new 
				FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		paramsButton.setMargins(15, 15, 15, 15);
		for (final OpcionDeEvento opcion:opciones) {
			Button opcionButton = new Button(this);
			opcionButton.setText(opcion.getContenido());
			opcionButton.setOnClickListener(new Button.OnClickListener() {
				OpcionDeEvento opcionSeleccionada = opcion;
	            public void onClick(View v) {
	            	Log.d(TAG + "- opcionButton - opcionId: " + 
	            			opcionSeleccionada.getId(), "estado: " + 
	            			Aplicacion.INSTANCIA.getEstado().toString());
	            	event.setOpcionSeleccionada(opcionSeleccionada);
	            	if (!Aplicacion.Estado.CON_CERTIFICADO.equals(Aplicacion.INSTANCIA.getEstado())) {
	            		Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
	            		showCertNotFoundDialog();
	            		return;
	            	} else {
	            		String contenido = opcionSeleccionada.getContenido().length() > SELECTED_OPTION_MAX_LENGTH ?
	        				 opcionSeleccionada.getContenido().substring(0, SELECTED_OPTION_MAX_LENGTH) + 
	        				 "..." : opcionSeleccionada.getContenido();
	            		showPinScreen(getString(R.string.option_selected_msg, contenido));
	            	} 
	            }
	        });
			optionButtons.add(opcionButton);
			if (!event.estaAbierto()) opcionButton.setEnabled(false);
			linearLayout.addView(opcionButton, paramsButton);			   
		}
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
	

    @Override public void onResume() {
    	super.onResume();
    	Log.d(TAG + ".onResume() ", " - onResume");
    }
    
    @Override protected void onStop() {
        super.onStop();
    	Log.d(TAG + ".onStop()", " - onStop");
    };

    @Override protected void onDestroy() {
        super.onDestroy();
    	Log.d(TAG + ".onDestroy()", " - onDestroy");
    };
    
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
			if(votingService != null) votingService.processVote(evento, this, keyStoreBytes, password);	
			//VotacionHelper.procesarVoto(getApplicationContext(), evento, this, 
			//		keyStoreBytes, password);
		} catch (IOException ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			showMessage(getString(R.string.error_lbl), 
					getString(R.string.pin_error_msg));
			setOptionButtonsEnabled(true);
		} catch (Exception ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			showMessage(getString(R.string.error_lbl), ex.getMessage());
		}
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
	
	private void unbindVotingService() {
		Log.d(TAG + ".unbindVotingService()", "--- unbindVotingService");
		if(serviceConnection != null) unbindService(serviceConnection);
	}
	
	private void stopVotingService() {
		Log.d(TAG + ".stopVotingService()", "--- stopVotingService");
		if(serviceIntent != null) stopService(serviceIntent);
	}
	
    private void showPinScreen(String message) {
		bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    	certPinScreen = CertPinScreen.newInstance(
    			VotingEventScreen.this, message, true);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        certPinScreen.show(ft, CERT_PIN_DIALOG);
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
		if(certPinScreen.getDialog() != null)
			certPinScreen.getDialog().dismiss();
	}

	@Override
	public void proccessReceipt(VoteReceipt receipt) {
		Log.d(TAG + ".proccessReceipt()", "--- proccessReceipt ");
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
		VotingResultDialog votingResultDialog = VotingResultDialog.newInstance(
				getString(R.string.operacion_ok_msg), receipt);
		FragmentManager fm = getSupportFragmentManager();
		votingResultDialog.show(fm, "fragment_voting_result");
		this.receipt = receipt;
        setReceiptScreen(receipt);
		//showMessage(getString(R.string.operacion_ok_msg), 
		//		receipt.getMensaje());
	}

	@Override
	public void setMsg(int statusCode, String msg) {
		Log.d(TAG + ".setMsg()", "--- statusCode: " 
				+ statusCode + " - msg: " + msg);
		String caption  = null;
		if(Respuesta.SC_OK != statusCode) {
			caption = getString(R.string.error_lbl) + " " 
					+ new Integer(statusCode).toString();
		} else caption = getString(R.string.msg_lbl);
		showMessage(caption, msg);
	}

	
	ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG + ".onServiceDisconnected()", " onServiceDisconnected ");
	        //Toast.makeText(VotingEventScreen.this, 
	        //		" --- VotingEventScreen - onServiceDisconnected",300);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG + ".onServiceConnected()", " onServiceConnected ");
			votingService = ((VotingService.VotingServiceBinder) service).getBinder();
	        //Toast.makeText(VotingEventScreen.this, 
	        //		" --- VotingEventScreen - onServiceConnected",300);
		}
	};
	
}