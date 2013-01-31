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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.sistemavotacion.android.db.VoteReceiptDBHelper;
import org.sistemavotacion.android.service.SignService;
import org.sistemavotacion.android.service.SignServiceListener;
import org.sistemavotacion.android.service.VotingService;
import org.sistemavotacion.android.service.VotingServiceListener;
import org.sistemavotacion.android.ui.CancelVoteDialog;
import org.sistemavotacion.android.ui.CertPinScreen;
import org.sistemavotacion.android.ui.CertPinScreenCallback;
import org.sistemavotacion.android.ui.VotingResultDialog;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionDeEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;

import android.app.AlertDialog;
import android.app.NotificationManager;
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
	implements CertPinScreenCallback, VotingServiceListener, SignServiceListener {
	
	public static final String TAG = "VotingEventScreen";
	
	
	public enum Operation {CANCEL_VOTE, SAVE_VOTE, VOTE};
	
    private static final String CERT_PIN_DIALOG = "certPinDialog";
    public static final String INTENT_EXTRA_DIALOG_PROP_NAME = "dialog";
    public static final String RECEIPT_KEY_PROP_NAME = "receiptKey";
    public static final int SELECTED_OPTION_MAX_LENGTH = 27;

    public static Operation operation = Operation.VOTE;
    private Evento evento;
    private VoteReceipt receipt;
    private ProgressDialog progressDialog = null;
    private CertPinScreen certPinScreen;
    private AsyncTask runningTask = null;
    private List<Button> optionButtons = null;
    byte[] keyStoreBytes = null;
	private Intent votingServiceIntent = null;
	private Intent signServiceIntent = null;
	private Button saveReceiptButton;
	private Button cancelVoteButton;
	private VotingService votingService = null;
	private SignService signService = null;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG + ".onCreate(...)", " - onCreate");
        setTheme(Aplicacion.THEME);
		super.onCreate(savedInstanceState);
		if(Aplicacion.INSTANCIA == null) return; 
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
		setContentView(R.layout.voting_event_screen);
		saveReceiptButton = (Button) findViewById(R.id.save_receipt_button);
		cancelVoteButton = (Button) findViewById(R.id.cancel_vote_button);
		try {
			getActionBar().setHomeButtonEnabled(true);
			getActionBar().setLogo(R.drawable.poll_32);
		} catch(NoSuchMethodError ex) {
			Log.d(TAG + ".onCreate(...)", " --- android api 11 I doesn't have method 'setLogo'");
		}  
		votingServiceIntent = new Intent(VotingEventScreen.this, VotingService.class);
		startService(votingServiceIntent);
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
        operation = Operation.VOTE;
        evento = Aplicacion.INSTANCIA.getEventoSeleccionado();
        setEventScreen(evento);
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
		contenidoTextView.setText(Html.fromHtml(receipt.getVoto().getContenido()));
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
    	signServiceIntent = new Intent(VotingEventScreen.this, SignService.class);
		startService(signServiceIntent);
    	operation = Operation.CANCEL_VOTE;
		bindService(signServiceIntent, signServiceConnection, BIND_AUTO_CREATE);
    	showPinScreen(getString(R.string.cancel_vote_msg));
	}
	
	public void saveVote(View v) {
		Log.d(TAG + ".saveVote(...)", " - saveVote");
		/*Log.d(TAG + ".guardarReciboButton ", " - Files dir path: " + 
		getActivity().getApplicationContext().getFilesDir().getAbsolutePath());
		String receiptFileName = StringUtils.getCadenaNormalizada(reciboVoto.getEventoURL()) ;
				
		File file = new File(getActivity().getApplicationContext().getFilesDir(), receiptFileName);
		Log.d(TAG + ".guardarReciboButton ", " - Files path: " + file.getAbsolutePath());
		try {
			reciboVoto.writoToFile(file);
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getActivity().getString(R.string.operacion_ok_msg)).
				setMessage(getActivity().getString(R.string.receipt_file_saved_msg)).show();
		} catch(Exception ex) {
			AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
			builder.setTitle(getActivity().getString(R.string.error_lbl)).
				setMessage(ex.getMessage()).show();
		}*/
    	VoteReceiptDBHelper db = new VoteReceiptDBHelper(VotingEventScreen.this);
		try {
			db.addVoteReceipt(receipt);
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
		contenidoTextView.setMovementMethod(LinkMovementMethod.getInstance());
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
	            			Aplicacion.INSTANCIA.getEstado().toString());
	            	operation = Operation.VOTE;
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
    	unbindSignService();
    	unbindVotingService();
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
		} catch (Exception ex) {
			Log.e(TAG + ".firmarEnviarVoto(...)", "Exception: " + ex.getMessage());
			String msg = ex.getMessage();
			if(msg == null) msg = getString(R.string.alert_exception_caption);
			showMessage(getString(R.string.error_lbl), 
					getString(R.string.pin_error_msg));
			setOptionButtonsEnabled(true);
		}
	}

	private void processCancelVote(char[] password) {
        Map map = new HashMap();
        map.put("origenHashCertificadoVoto", receipt.getVoto().getOrigenHashCertificadoVoto());
        map.put("hashCertificadoVotoBase64", receipt.getVoto().getHashCertificadoVotoBase64());
        map.put("origenHashSolicitudAcceso", receipt.getVoto().getOrigenHashSolicitudAcceso());
        map.put("hashSolicitudAccesoBase64", receipt.getVoto().getHashSolicitudAccesoBase64());
        JSONObject jsonObject = new JSONObject(map);
        String signatureContent = jsonObject.toString();     
        String subject = getString(R.string.cancel_vote_msg_subject); 
        String serverURL = ServerPaths.getURLAnulacionVoto(Aplicacion.CONTROL_ACCESO_URL);
		cancelVoteButton.setEnabled(false);
        try {
    		if(signService != null) signService.processSignature(signatureContent, subject, serverURL, this, 
    				true, keyStoreBytes, password);	
        } catch(Exception ex) {
        	ex.printStackTrace();
			Log.e(TAG + ".processCancelVote(...)", "Exception: " + ex.getMessage());
			cancelVoteButton.setEnabled(true);
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
		//if(votingServiceConnection != null && votingServiceIntent != null) 
		if(votingService != null) unbindService(votingServiceConnection);
	}
	
	private void stopVotingService() {
		Log.d(TAG + ".stopVotingService()", "--- stopVotingService");
		if(votingServiceIntent != null && votingServiceIntent != null) 
			stopService(votingServiceIntent);
	}
	
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
	
    private void showPinScreen(String message) {
		bindService(votingServiceIntent, votingServiceConnection, BIND_AUTO_CREATE);
    	certPinScreen = CertPinScreen.newInstance(
    			VotingEventScreen.this, message, true);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        certPinScreen.show(ft, CERT_PIN_DIALOG);
    }

	@Override public void setPin(final String pin) {
		Log.d(TAG + ".setPin()", "--- setPin - operation: " + operation);
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
	        switch(operation) {
	        	case VOTE:
	        		firmarEnviarVoto(pin.toCharArray());
	        		break;
	        	case CANCEL_VOTE:
	        		processCancelVote(pin.toCharArray());
	        		break;
	        	default:
	        		Log.d(TAG + ".setPin(...)", "--- unknown operation:" + operation);
	        }
	        
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
	

	@Override
	public void setSignServiceMsg(int statusCode, String msg) {
		Log.d(TAG + ".setSignServiceMsg()", "--- statusCode: " 
				+ statusCode + " - msg: " + msg);
		String caption  = null;
		if(Respuesta.SC_OK != statusCode) {
			caption = getString(R.string.error_lbl) + " " 
					+ new Integer(statusCode).toString();
			cancelVoteButton.setEnabled(true);
		}
		showMessage(caption, msg);
	}
	
	@Override
	public void proccessReceipt(SMIMEMessageWrapper cancelReceipt) {
		Log.d(TAG + ".proccessReceipt(...)", "--- proccessReceipt " );
		NotificationManager notificationManager =
			    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(receipt.getNotificationId());
		receipt.setCancelVoteReceipt(cancelReceipt);
		String msg = getString(R.string.cancel_vote_result_msg, 
				this.receipt.getVoto().getAsunto());
		String caption = getString(R.string.msg_lbl);
		setEventScreen(Aplicacion.INSTANCIA.getEventoSeleccionado());
		CancelVoteDialog cancelVoteDialog = CancelVoteDialog.newInstance(
				caption, msg, receipt);
		FragmentManager fm = getSupportFragmentManager();
		cancelVoteDialog.show(fm, "fragment_cancel_vote_result");
	}
	
	ServiceConnection votingServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG + ".votingServiceConnection.onServiceDisconnected()", 
					"votingServiceConnection.onServiceDisconnected ");
	        //Toast.makeText(VotingEventScreen.this, 
	        //		" --- VotingEventScreen - onServiceDisconnected",300);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG + ".votingServiceConnection.onServiceConnected()", 
					"votingServiceConnection.onServiceConnected ");
			votingService = ((VotingService.VotingServiceBinder) service).getBinder();
	        //Toast.makeText(VotingEventScreen.this, 
	        //		" --- VotingEventScreen - onServiceConnected",300);
		}
	};
	
	ServiceConnection signServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG + ".signingServiceConnection.onServiceDisconnected()", 
					" - signingServiceConnection.onServiceDisconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG + ".signingServiceConnection.onServiceConnected()", 
					" - signingServiceConnection.onServiceConnected");
			signService = ((SignService.SignServiceBinder) service).getBinder();
		}
	};
	
}