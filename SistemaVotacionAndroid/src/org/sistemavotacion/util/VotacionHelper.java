package org.sistemavotacion.util;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;
import static org.sistemavotacion.android.Aplicacion.TIMESTAMP_VOTE_HASH;
import static org.sistemavotacion.android.Aplicacion.VOTE_SIGN_MECHANISM;
import static org.sistemavotacion.android.Aplicacion.getAppString;
import static org.sistemavotacion.android.Aplicacion.getFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.List;
import java.util.UUID;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.json.DeObjetoAJSON;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.seguridad.TimeStampWrapper;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.GetTimeStampTask;
import org.sistemavotacion.task.GetVotingCertTask;
import org.sistemavotacion.task.SendFileTask;
import org.sistemavotacion.task.TaskListener;
import org.sistemavotacion.task.VotingListener;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class VotacionHelper implements TaskListener {
	
	public static final String TAG = "VotacionHelper";

    private static final int TIMESTAMP_ACCESS_REQUEST = 0;
    private static final int TIMESTAMP_VOTE           = 1;
	
    
    private Evento event;
    private Context context;
    private VotingListener votingListener;
    private byte[] keyStoreBytes;
    private char[] password;
    private PKCS10WrapperClient pkcs10WrapperClient = null;
    private static VotacionHelper instancia;
    private SMIMEMessageWrapper timeStampedDocument;
    
    DataListener<String> voteListener = new DataListener<String>() {

    	@Override public void updateData(int statusCode, String response) {
			Log.d(TAG + ".voteListener.updateData(...) ",	" - statusCode: " + statusCode);
	        if (Respuesta.SC_OK == statusCode) {
                try {
					SMIMEMessageWrapper votoValidado = new SMIMEMessageWrapper(null,
							new ByteArrayInputStream(response.getBytes()), null);
					ReciboVoto receipt = new ReciboVoto(Respuesta.SC_OK, votoValidado, event);
					votingListener.proccessReceipt(receipt);
				} catch (Exception ex) {
					Log.e(TAG + ".voteListener.updateData(...)", ex.getMessage(), ex);
					String msg = context.getString(R.string.receipt_error_msg) 
							+ ": " + ex.getMessage();
					setException(msg);
				}
	        } else setException(response);
		}
		
    	@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".voteListener.setException(...) ", " - exceptionMsg: " + exceptionMsg);
			votingListener.setException(exceptionMsg);
		}
    };
  
    
    private VotacionHelper(Evento event, VotingListener votingListener, 
    		Context context, byte[] keyStoreBytes, char[] password) 
    		throws NoSuchAlgorithmException, InvalidKeyException, 
    		NoSuchProviderException, SignatureException, IOException {
    	this.event = prepararVoto(event);
    	this.votingListener = votingListener;
    	this.context = context;
    	this.keyStoreBytes = keyStoreBytes;
    	this.password = password;
    	pkcs10WrapperClient = PKCS10WrapperClient.buildCSRVoto(KEY_SIZE, SIG_NAME, 
		        SIGNATURE_ALGORITHM, PROVIDER, CONTROL_ACCESO_URL, 
		        event.getEventoId().toString(), event.getHashCertificadoVotoHex());
    }
    
    private static Evento prepararVoto (Evento evento) throws NoSuchAlgorithmException {    
        evento.setOrigenHashSolicitudAcceso(UUID.randomUUID().toString());
        evento.setHashSolicitudAccesoBase64(CMSUtils.obtenerHashBase64(
        		evento.getOrigenHashSolicitudAcceso(), Aplicacion.SIG_HASH));
        evento.setOrigenHashCertificadoVoto(UUID.randomUUID().toString());
        evento.setHashCertificadoVotoBase64(CMSUtils.obtenerHashBase64(
        		evento.getOrigenHashCertificadoVoto(), Aplicacion.SIG_HASH));         
        return evento;
    }
    
    public void obtenerSolicitudAcceso () throws NoSuchAlgorithmException, Exception {
    	String asunto = getAppString(R.string.request_msg_subject, 
        		event.getEventoId());
    	Log.d(TAG + ".obtenerSolicitudAcceso()", " - asunto: " + asunto);
        String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
				keyStoreBytes, ALIAS_CERT_USUARIO, password, VOTE_SIGN_MECHANISM);
		String contenidoFirma = DeObjetoAJSON.obtenerSolicitudAccesoJSON(event);
        File solicitudAcceso = getFile("accessRequest_" + 
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
    
    private void setTimeStampedDocument(int timeStampOperation, File document,  
            String timeStamprequestAlg) {
        if(document == null) return;
        try {
        	timeStampedDocument = new SMIMEMessageWrapper(null, document);
        	GetTimeStampTask getTimeStampTask = new GetTimeStampTask(timeStampOperation, 
        			timeStampedDocument.getTimeStampRequest(timeStamprequestAlg), this);
        	votingListener.setRunningTask(getTimeStampTask.execute(
        			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL)));
        } catch (Exception ex) {
			Log.e(TAG + ".setTimeStampedDocument(...)", ex.getMessage(), ex);
        }
    }
    
    public static void procesarVoto(Context context, 
    		Evento event, VotingListener votingListener, 
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".procesarVoto(...)", " - procesarVoto");
    	instancia = new VotacionHelper(event, votingListener, context,
    			keyStoreBytes, password);
    	instancia.obtenerSolicitudAcceso();
    }

	@Override
	public void processTaskMessages(List<String> messages, AsyncTask task) { }

	private void processTimeStampResponse (int timeStampRequest, 
			TimeStampWrapper timeStampWrapper) {
		try {
			switch(timeStampRequest) {
				case TIMESTAMP_ACCESS_REQUEST:
			        GetVotingCertTask obtenerCertificadoVotoTask = new GetVotingCertTask(
			        		this, timeStampedDocument.setTimeStampToken(
	                		timeStampWrapper), pkcs10WrapperClient);
			        
			        obtenerCertificadoVotoTask.execute(ServerPaths.
			        		getURLSolicitudAcceso(CONTROL_ACCESO_URL));
			        votingListener.setRunningTask(obtenerCertificadoVotoTask);
					break;
				case TIMESTAMP_VOTE:
		            votingListener.setRunningTask(new SendFileTask(voteListener, 
		            		timeStampedDocument.setTimeStampToken(
		            		timeStampWrapper)).execute(ServerPaths.getURLVoto(
			    			event.getCentroControl().getServerURL())));
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
	        if(Respuesta.SC_OK == getVotingCertTask.getStatusCode()) {
		        try {
		            String votoJSON = DeObjetoAJSON.obtenerVotoParaEventoJSON(event);
		            String usuario = null;
		            if (Aplicacion.getUsuario() != null) usuario = 
		            		Aplicacion.getUsuario().getNif();
		            File votoFirmado = getFile("vote_" + 
	                		event.getEventoId() + SIGNED_PART_EXTENSION);
	                votoFirmado = getVotingCertTask.genSignedFile(usuario, 
	                		event.getCentroControl().getNombreNormalizado(),
	                        votoJSON, getAppString(R.string.vote_msg_subject), null, 
	                        SignedMailGenerator.Type.USER, votoFirmado);
		            setTimeStampedDocument(TIMESTAMP_VOTE, votoFirmado, TIMESTAMP_VOTE_HASH);
				} catch (Exception e) {
					votingListener.setException(e.getMessage());
				}	
	        } else if(Respuesta.SC_ERROR_VOTO_REPETIDO == getVotingCertTask.getStatusCode()) {
	        	votingListener.setException(getAppString(R.string.error_vote_repeated_msg));
	        } else {
	        	votingListener.setException(getVotingCertTask.getMessage());
	        }
		}
		
	}
	
}