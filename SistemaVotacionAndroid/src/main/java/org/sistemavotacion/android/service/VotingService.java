package org.sistemavotacion.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.bouncycastle2.util.encoders.Base64;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.VotingEventScreen;
import org.sistemavotacion.callable.AccessRequestor;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.SMIMESignedSenderTask;
import org.sistemavotacion.util.ServerPaths;
import org.sistemavotacion.util.StringUtils;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;

public class VotingService extends Service {
	
	public static final String TAG = "VotingService";
    
	private IBinder iBinder = new VotingServiceBinder();
	
	private ExecutorService executorService = Executors.newFixedThreadPool(3);
	
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
    
    public void processVote(Integer requestId, Evento event, 
    		ServiceListener voteProcessListener, 
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processVote(...)", " - processVote - event subject: " + event.getAsunto());
    	event.initVoteData();
    	String asunto = getString(R.string.request_msg_subject, 
        		event.getEventoId());
        String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
		
    	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
        PrivateKey privateKey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
        Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
        X509Certificate userCert = (X509Certificate) chain[0];
        
		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
				privateKey, chain, Aplicacion.SIGNATURE_ALGORITHM);
		String contenidoFirma = event.getAccessRequestJSON().toString();
		
		SMIMEMessageWrapper solicitudAcceso = signedMailGenerator.genMimeMessage(
				usuario, Aplicacion.getControlAcceso().getNombreNormalizado(), 
				contenidoFirma, asunto, null);

		AccessRequestor accessRequestor = new AccessRequestor(solicitudAcceso,
				event, Aplicacion.getControlAcceso().getCertificado(), 
				ServerPaths.getURLSolicitudAcceso(CONTROL_ACCESO_URL));
		Future<Respuesta> future = executorService.submit(accessRequestor);
		
		Respuesta respuesta = future.get();
		if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
			voteProcessListener.proccessResponse(requestId, respuesta);
			return;
		}
		String votoJSON = event.getVoteJSON().toString();
		PKCS10WrapperClient pkcs10WrapperClient = accessRequestor.getPKCS10WrapperClient();
		SMIMEMessageWrapper signedVote = pkcs10WrapperClient.
				genSignedMessage(event.getHashCertificadoVotoBase64(), 
				event.getCentroControl().getNombreNormalizado(), 
				votoJSON, getString(R.string.vote_msg_subject), null);
		
        SMIMESignedSenderTask signedSenderTask = new SMIMESignedSenderTask(
        		signedVote, pkcs10WrapperClient.getKeyPair(), 
        		event.getCentroControl().getCertificado());
        signedSenderTask.execute(ServerPaths.getURLVoto(
    			event.getCentroControl().getServerURL()));
        
        Respuesta response = signedSenderTask.get();
        if(Respuesta.SC_OK == response.getCodigoEstado()) {
			SMIMEMessageWrapper votoValidado = response.getSmimeMessage();
			VoteReceipt receipt = new VoteReceipt(Respuesta.SC_OK, votoValidado, event);

			byte[] base64EncodedKey = Base64.encode(
					pkcs10WrapperClient.getPrivateKey().getEncoded());
        	byte[] encryptedKey = Encryptor.encryptMessage(base64EncodedKey, userCert);

        	receipt.setPkcs10WrapperClient(pkcs10WrapperClient);
			receipt.setEncryptedKey(encryptedKey);
			response.setData(receipt);
        	
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
        } else {
        	//cancel access request
            String subject = getString(R.string.cancel_vote_msg_subject); 
            String serviceURL = ServerPaths.getURLAnulacionVoto(Aplicacion.CONTROL_ACCESO_URL);
    		SMIMEMessageWrapper cancelAccessRequest = signedMailGenerator.genMimeMessage(
    				usuario, Aplicacion.getControlAcceso().getNombreNormalizado(), 
    				event.getCancelVoteData(), subject, null);
            signedSenderTask = new SMIMESignedSenderTask(cancelAccessRequest, 
            		null, Aplicacion.getControlAcceso().getCertificado());
            signedSenderTask.execute(serviceURL);
            response = signedSenderTask.get();
            String msg = getString(R.string.voting_service_error_msg);
            response = new Respuesta(Respuesta.SC_ERROR, msg);
            voteProcessListener.proccessResponse(requestId, response);
        	return;
        }
		
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
        voteProcessListener.proccessResponse(requestId, response);
    }
    
}