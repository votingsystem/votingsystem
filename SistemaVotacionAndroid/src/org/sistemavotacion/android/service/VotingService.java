package org.sistemavotacion.android.service;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.KEY_STORE_FILE;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;
import static org.sistemavotacion.android.Aplicacion.TIMESTAMP_VOTE_HASH;
import static org.sistemavotacion.android.Aplicacion.VOTE_SIGN_MECHANISM;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.VotingEventScreen;
import org.sistemavotacion.json.DeObjetoAJSON;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.seguridad.TimeStampWrapper;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.GetTimeStampTask;
import org.sistemavotacion.task.GetVotingCertTask;
import org.sistemavotacion.task.SendFileTask;
import org.sistemavotacion.task.TaskListener;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.StringUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class VotingService extends Service implements TaskListener {
	
	public static final String TAG = "VotingService";
	
    private static final int TIMESTAMP_ACCESS_REQUEST        = 0;
    private static final int TIMESTAMP_VOTE                  = 1;
    private static final int TIMESTAMP_CANCEL_ACCESS_REQUEST = 2;
	
    private static final int VOTE_REQUEST                    = 3;
    private static final int CANCEL_ACCESS_REQUEST           = 4;
    
    private Evento event;
    private VotingServiceListener voteProcessListener;
    private byte[] keyStoreBytes;
    private char[] password;
    private PKCS10WrapperClient pkcs10WrapperClient = null;
    private AsyncTask runningTask = null;
    private SMIMEMessageWrapper timeStampedDocument;


	private IBinder iBinder = new VotingServiceBinder();
	
	public class VotingServiceBinder extends Binder {
		public VotingService getBinder() {
			return VotingService.this;
		}
	}

    @Override public IBinder onBind(Intent intent) {
    	Log.d(TAG + ".onBind(...) ", " *** VotingService - onBind ");
		return iBinder;
    }

	@Override public boolean onUnbind(Intent intent) {
    	Log.d(TAG + ".onBind(...) ", " *** VotingService - onUnbind ");
		/*try { th.interrupt();} catch (Exception e) {}*/
		voteProcessListener = null;
		return super.onUnbind(intent);
	}

	@Override public void unbindService(ServiceConnection conn) {
    	Log.d(TAG + ".onBind(...) ", " *** VotingService - unbindService ");
		super.unbindService(conn);
	}
	
    @Override public void onCreate() {
    	Log.d(TAG + ".onCreate(...) ", " *** VotingService - onCreate ");
        super.onCreate();
    }

    @Override public void onDestroy() {
    	Log.d(TAG + ".onDestroy(...) ", " *** onDestroy ");
        super.onDestroy();
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        Toast.makeText(this, getApplicationContext().getString(
        		R.string.low_memory_msg), 300);
    }
    

	private void processCancelVote(char[] password) throws JSONException {   
        String subject = getString(R.string.cancel_vote_msg_subject); 
        try {
    		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
    				keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
            File signedFile = File.createTempFile("signedDocument", SIGNED_PART_EXTENSION);
        	String usuario = null;
            if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
            signedFile = signedMailGenerator.genFile(usuario, 
    				Aplicacion.getControlAcceso().getNombreNormalizado(), 
    				event.getCancelVoteData(), subject, null, 
    				SignedMailGenerator.Type.USER, signedFile);
    		setTimeStampedDocument(TIMESTAMP_CANCEL_ACCESS_REQUEST, signedFile, TIMESTAMP_VOTE_HASH);
        } catch(Exception ex) {
        	ex.printStackTrace();
			voteProcessListener.setMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
        }

	}
    
    @Override
    public void onStart(Intent intent, int startId) {
    	Log.d(TAG + ".onStart(...) ", " *** onStart ");
        super.onStart(intent, startId);
    }

	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG + ".onStartCommand(...) ", " - flags: " + flags + " - startId: " + startId);
        return super.onStartCommand(intent, flags, startId);
    }

    private void setTimeStampedDocument(int timeStampOperation, File document,  
            String timeStamprequestAlg) {
        if(document == null) return;
        try {
        	timeStampedDocument = new SMIMEMessageWrapper(null, document);
        	runningTask = new GetTimeStampTask(timeStampOperation, 
        			timeStampedDocument.getTimeStampRequest(timeStamprequestAlg), this).execute(
        			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL));
        } catch (Exception ex) {
			Log.e(TAG + ".setTimeStampedDocument(...)", ex.getMessage(), ex);
        }
    }
    
    public void obtenerSolicitudAcceso() throws NoSuchAlgorithmException, Exception {
    	String asunto = getString(R.string.request_msg_subject, 
        		event.getEventoId());
    	Log.d(TAG + ".obtenerSolicitudAcceso()", " - asunto: " + asunto);
        String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
				keyStoreBytes, ALIAS_CERT_USUARIO, password, VOTE_SIGN_MECHANISM);
		String contenidoFirma = DeObjetoAJSON.obtenerSolicitudAccesoJSON(event);
        File solicitudAcceso = new File(getApplicationContext().getFilesDir(), "accessRequest_" + 
		        		event.getEventoId() + SIGNED_PART_EXTENSION);
		solicitudAcceso = signedMailGenerator.genFile(usuario, 
				Aplicacion.getControlAcceso().getNombreNormalizado(), 
				contenidoFirma, asunto, null, SignedMailGenerator.Type.USER, 
				solicitudAcceso);
		setTimeStampedDocument(TIMESTAMP_ACCESS_REQUEST, solicitudAcceso, 
				TIMESTAMP_VOTE_HASH);
		
		/* problema -> javax.activation.UnsupportedDataTypeException: 
		 * no object DCH for MIME type application/pkcs7-signature
		MimeMessage solicitudAccesoMimeMessage = dnies.gen(usuario, 
				Aplicacion.getControlAcceso().getNombreNormalizado(), 
				contenidoFirma, asunto, null, SignedMailGenerator.Type.USER);
		Object content = solicitudAccesoMimeMessage.getContent();
		MimeMultipart mimeMultipart = null;
	    if (content.getClass().isAssignableFrom(MimeMultipart.class)) {
	    	mimeMultipart = (MimeMultipart) content;
	    }
	    SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);*/
		/*Tambien se puede obtener el digest
		SMIMESignedGenerator gen = dnies.getSMIMESignedGenerator();
		byte[] contentDigestBytes = (byte[])gen.getGeneratedDigests().get(SMIMESignedGenerator.DIGEST_SHA1);
		String contentDigest = Base64.encodeToString(contentDigestBytes, Base64.DEFAULT);
		Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - contentDigest: " + contentDigest);*/
    	
    }
    
    public void processVote(Evento event, VotingServiceListener voteProcessListener, 
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processVote(...)", " - procesarVoto");
    	this.event = event.initVoteData();
    	this.voteProcessListener = voteProcessListener;
    	this.keyStoreBytes = keyStoreBytes;
    	this.password = password;
    	pkcs10WrapperClient = PKCS10WrapperClient.buildCSRVoto(KEY_SIZE, SIG_NAME, 
		        SIGNATURE_ALGORITHM, PROVIDER, CONTROL_ACCESO_URL, 
		        event.getEventoId().toString(), event.getHashCertificadoVotoHex());
    	obtenerSolicitudAcceso();
    }

	@Override
	public void processTaskMessages(List<String> messages, AsyncTask task) { }

	private void processTimeStampResponse (int timeStampRequest, 
			TimeStampWrapper timeStampWrapper) {
		try {
			switch(timeStampRequest) {
				case TIMESTAMP_ACCESS_REQUEST:
			        runningTask = new GetVotingCertTask(this, 
			        		timeStampedDocument.setTimeStampToken(
			                timeStampWrapper), pkcs10WrapperClient).execute(ServerPaths.
			        		getURLSolicitudAcceso(CONTROL_ACCESO_URL));
					break;
				case TIMESTAMP_VOTE:
					runningTask = new SendFileTask(VOTE_REQUEST, this, 
		            		timeStampedDocument.setTimeStampToken(
		            		timeStampWrapper)).execute(ServerPaths.getURLVoto(
			    			event.getCentroControl().getServerURL()));
					break;
				case TIMESTAMP_CANCEL_ACCESS_REQUEST:
					runningTask = new SendFileTask(CANCEL_ACCESS_REQUEST, this, 
							timeStampedDocument.setTimeStampToken(
							timeStampWrapper)).execute(
							ServerPaths.getURLAnulacionVoto(Aplicacion.CONTROL_ACCESO_URL));
					break;
			}	
		} catch (Exception ex) {
			Log.e(TAG + ".processTimeStampResponse(...)", ex.getMessage(), ex);
		}
	}
	
	@Override
	public void showTaskResult(AsyncTask task) {
		Log.d(TAG + ".showTaskResult(...)", " - task: " + task.getClass());
		if(task instanceof GetTimeStampTask) {
			GetTimeStampTask timeStampTask = (GetTimeStampTask)task;
			if(Respuesta.SC_OK == timeStampTask.getStatusCode()) {
				processTimeStampResponse(
						((GetTimeStampTask)task).getId(), (GetTimeStampTask)task);
			}
		} else if(task instanceof GetVotingCertTask) {
			GetVotingCertTask getVotingCertTask = (GetVotingCertTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - statuscode: " + getVotingCertTask.getStatusCode());
	        if(Respuesta.SC_OK == getVotingCertTask.getStatusCode()) {
		        try {
		            String votoJSON = DeObjetoAJSON.obtenerVotoParaEventoJSON(event);
		            String usuario = null;
		            if (Aplicacion.getUsuario() != null) usuario = 
		            		Aplicacion.getUsuario().getNif();
		            File votoFirmado = new File(getApplicationContext().getFilesDir(), "vote_" + 
	                		event.getEventoId() + SIGNED_PART_EXTENSION);
	                votoFirmado = getVotingCertTask.genSignedFile(usuario, 
	                		event.getCentroControl().getNombreNormalizado(),
	                        votoJSON, getString(R.string.vote_msg_subject), null, 
	                        SignedMailGenerator.Type.USER, votoFirmado);
		            setTimeStampedDocument(TIMESTAMP_VOTE, votoFirmado, TIMESTAMP_VOTE_HASH);
				} catch (Exception e) {
					voteProcessListener.setMsg(Respuesta.SC_ERROR_EJECUCION, 
							e.getMessage());
				}	
	        } else {
	        	voteProcessListener.setMsg(getVotingCertTask.getStatusCode(),
	        			getVotingCertTask.getMessage());
	        }
		} else if(task instanceof SendFileTask) {
			SendFileTask sendFileTask = (SendFileTask)task;
			Log.d(TAG + ".showTaskResult(...)", " sendFileTask - statuscode: " + sendFileTask.getStatusCode());
			if(Respuesta.SC_OK == sendFileTask.getStatusCode()) {
				switch(sendFileTask.getId()) {
				case VOTE_REQUEST:
	                try {
						SMIMEMessageWrapper votoValidado = new SMIMEMessageWrapper(null,
								new ByteArrayInputStream(sendFileTask.getMessage().getBytes()), null);
						VoteReceipt receipt = new VoteReceipt(Respuesta.SC_OK, votoValidado, event);
						voteProcessListener.proccessReceipt(receipt);
						
						NotificationManager notificationManager =
							    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
						Intent intent = new Intent(VotingService.this, VotingEventScreen.class);
						AppData.INSTANCE.putReceipt(StringUtils.getCadenaNormalizada(
								receipt.getEventoURL()), receipt);
						intent.putExtra(VotingEventScreen.RECEIPT_KEY_PROP_NAME, 
								StringUtils.getCadenaNormalizada(receipt.getEventoURL()));
					    PendingIntent pIntent = PendingIntent.getActivity(
					    		VotingService.this, 0, intent, 0);
					    NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(VotingService.this)
				        	.setContentTitle(event.getAsunto())
				        	.setContentText(getString(R.string.voted_lbl) + ": " + event.getOpcionSeleccionada().
				        		getContenido()).setSmallIcon(R.drawable.poll_48)
				        .setContentIntent(pIntent);
					    Notification notification = notBuilder.getNotification();
					    
					    
						/*Intent intentSaveVote = new Intent(VotingService.this, VotingEventScreen.class);
						intentSaveVote.putExtra(VotingEventScreen.INTENT_EXTRA_DIALOG_PROP_NAME, 
								VotingEventScreen.Operation.SAVE_VOTE);
						Intent intentCancelVote = new Intent(VotingService.this, VotingEventScreen.class);
						intentCancelVote.putExtra(VotingEventScreen.INTENT_EXTRA_DIALOG_PROP_NAME, 
								VotingEventScreen.Operation.CANCEL_VOTE);
					    PendingIntent pIntent = PendingIntent.getActivity(
					    		VotingService.this, 0, intent, 0);
					    PendingIntent pIntentSaveVote = PendingIntent.getActivity(
					    		VotingService.this, 0, intentSaveVote, 0);
					    PendingIntent pIntentCancelVote = PendingIntent.getActivity(
					    		VotingService.this, 0, intentCancelVote, 0);

					    Notification noti = new Notification.Builder(VotingService.this)
					        .setContentTitle(event.getAsunto())
					        .setContentText(getString(R.string.voted_lbl) + ": " + event.getOpcionSeleccionada().
					        		getContenido()).setSmallIcon(R.drawable.poll_22)
					        .setContentIntent(pIntent)
					        .addAction(R.drawable.cancel_22x22, getString(R.string.cancel_vote_lbl), pIntent)
					        .addAction(R.drawable.filesave_22x22, getString(R.string.save_receipt_lbl), pIntent).build();*/
					    // Hide the notification after its selected
					    notification.flags |= Notification.FLAG_AUTO_CANCEL;
					  //notificationManager.notify(/* id */, notification);
					    notificationManager.notify(receipt.initNotificationId(), notification);
					} catch (Exception ex) {
						ex.printStackTrace();
						String msg = getString(R.string.receipt_error_msg) 
								+ ": " + ex.getMessage();
						voteProcessListener.setMsg(Respuesta.SC_ERROR_EJECUCION, msg);
					}
					break;
				case CANCEL_ACCESS_REQUEST:
					try {
	                	SMIMEMessageWrapper receipt = new SMIMEMessageWrapper(null,
								new ByteArrayInputStream(sendFileTask.getMessage().getBytes()), null);
	                	voteProcessListener.setMsg(Respuesta.SC_OK, getString(R.string.canceling_access_request_msg));
					} catch (Exception ex) {
						ex.printStackTrace();
						String msg = getString(R.string.receipt_error_msg) 
								+ ": " + ex.getMessage();
						voteProcessListener.setMsg(Respuesta.SC_ERROR_EJECUCION, msg);
					}
					break;
				default:
					Log.d(TAG + ".showTaskResult(...)", " Unknown SendFileTask id: " + sendFileTask.getId());
				}
			} else {
				voteProcessListener.setMsg(sendFileTask.getStatusCode(), sendFileTask.getMessage());
			}
		}
	}
    
}