package org.sistemavotacion.android.service;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;
import static org.sistemavotacion.android.Aplicacion.TIMESTAMP_VOTE_HASH;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.VotingSystemKeyStoreException;
import org.sistemavotacion.seguridad.EncryptionHelper;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.GetFileTask;
import org.sistemavotacion.task.GetTimeStampTask;
import org.sistemavotacion.task.SendFileTask;
import org.sistemavotacion.task.SignTimestampSendPDFTask;
import org.sistemavotacion.task.TaskListener;
import org.sistemavotacion.util.ServerPaths;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.itextpdf.text.pdf.PdfReader;

public class PublishService extends Service implements TaskListener {
	
	public static final String TAG = "PublishService";

    private PublishServiceListener serviceListener;
    private AsyncTask runningTask = null;
    private Operation pendingOperation;

	private IBinder iBinder = new PublishServiceBinder();
	
	public class PublishServiceBinder extends Binder {
		public PublishService getBinder() {
			return PublishService.this;
		}
	}

    @Override public IBinder onBind(Intent intent) {
    	Log.d(TAG + ".onBind(...) ", " *** PublishService - onBind ");
		return iBinder;
    }

	@Override public boolean onUnbind(Intent intent) {
    	Log.d(TAG + ".onBind(...) ", " *** PublishService - onUnbind ");
		/*try { th.interrupt();} catch (Exception e) {}*/
    	serviceListener = null;
		return super.onUnbind(intent);
	}

	@Override public void unbindService(ServiceConnection conn) {
    	Log.d(TAG + ".onBind(...) ", " *** PublishService - unbindService ");
		super.unbindService(conn);
	}
	
    @Override public void onCreate() {
    	Log.d(TAG + ".onCreate(...) ", " *** PublishService - onCreate ");
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
    
    public void publishDocument (Operation operation,
    		byte[] keyStoreBytes, char[] password, 
    		PublishServiceListener serviceListener) {
    	Log.d(TAG + ".publishDocument(...)", " operation: " + operation.getTipo());
    	this.pendingOperation = operation;
    	this.serviceListener = serviceListener;
    	String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
        File signedFile = null;
        SignedMailGenerator signedMailGenerator = null;
        try {
            if(pendingOperation.getTipo() != null && pendingOperation.getTipo() != 
            		Operation.Tipo.PUBLICACION_MANIFIESTO_PDF) {
                signedFile = File.createTempFile("signedDocument", SIGNED_PART_EXTENSION);
                signedFile.deleteOnExit();
                signedMailGenerator = new SignedMailGenerator(
    					keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
            }
            GetTimeStampTask timeStampTask = null;
            SMIMEMessageWrapper timeStampedDocument = null;
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
			//X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
    		switch(pendingOperation.getTipo()) {
				case PUBLICACION_MANIFIESTO_PDF:
					GetFileTask getFileTask = (GetFileTask)new GetFileTask(null, this).execute(
							pendingOperation.getUrlDocumento());
					if(Respuesta.SC_OK == getFileTask.get()) {
						try {
							PdfReader pdfFile = new PdfReader(getFileTask.getFileData());
							File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
							pdfFirmadoFile.deleteOnExit();
							Log.d(TAG + ".showTaskResult(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
							Certificate[] signerCertsChain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
							SignTimestampSendPDFTask signTimestampSendPDFTask = (SignTimestampSendPDFTask) 
									new SignTimestampSendPDFTask(this, null, ServerPaths.getURLTimeStampService(
									CONTROL_ACCESO_URL), null, null, signerPrivatekey, signerCertsChain, 
									pdfFile, pdfFirmadoFile, this).execute(pendingOperation.getUrlEnvioDocumento());
						} catch(Exception ex) {
							ex.printStackTrace();
						}
					} else {
						serviceListener.setPublishServiceMsg(
								getFileTask.getStatusCode(), getFileTask.getMessage());
					}
					break;
				case PUBLICACION_VOTACION_SMIME:
			        signedFile = signedMailGenerator.genFile(usuario, 
							Aplicacion.getControlAcceso().getNombreNormalizado(), 
							operation.getContenidoFirma().toString(), 
							operation.getAsuntoMensajeFirmado(), null, SignedMailGenerator.Type.USER, 
							signedFile);
			        timeStampedDocument = new SMIMEMessageWrapper(null, signedFile);
			        timeStampTask = (GetTimeStampTask) new GetTimeStampTask(null, 
			    			timeStampedDocument.getTimeStampRequest(), this).execute(
			    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL));
			        if(Respuesta.SC_OK == timeStampTask.get()) {
			        	File fileToEncrypt = timeStampedDocument.setTimeStampToken(timeStampTask);
			        	EncryptionHelper.encryptSMIMEFile(fileToEncrypt,
			        			Aplicacion.getControlAcceso().getCertificado());
			        	runningTask = new SendFileTask(null, this, fileToEncrypt).
								execute(pendingOperation.getUrlEnvioDocumento());
			        } else {
						String msg = getString(R.string.timestamp_connection_error_msg) 
								+ " - " + timeStampTask.getMessage();
						serviceListener.setPublishServiceMsg(timeStampTask.getStatusCode(), msg);
			        }
					break;
				case PUBLICACION_RECLAMACION_SMIME:
			        signedFile = signedMailGenerator.genFile(usuario, 
							Aplicacion.getControlAcceso().getNombreNormalizado(), 
							operation.getContenidoFirma().toString(), 
							operation.getAsuntoMensajeFirmado(), null, SignedMailGenerator.Type.USER, 
							signedFile);
			        timeStampedDocument = new SMIMEMessageWrapper(null, signedFile);
			        timeStampTask = (GetTimeStampTask) new GetTimeStampTask(null, 
			    			timeStampedDocument.getTimeStampRequest(), this).execute(
			    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL));
			        if(Respuesta.SC_OK == timeStampTask.get()) {
			        	File fileToEncrypt = timeStampedDocument.setTimeStampToken(timeStampTask);
			        	EncryptionHelper.encryptSMIMEFile(fileToEncrypt, 
			        			Aplicacion.getControlAcceso().getCertificado());
			            runningTask = new SendFileTask(null, this,
			            		fileToEncrypt).execute(pendingOperation.getUrlEnvioDocumento());
			        } else {
						String msg = getString(R.string.timestamp_connection_error_msg) 
								+ " - " + timeStampTask.getMessage();
						serviceListener.setPublishServiceMsg(timeStampTask.getStatusCode(), msg);
			        }
					break;	
				case ASOCIAR_CENTRO_CONTROL_SMIME:
			        signedFile = signedMailGenerator.genFile(usuario, 
							Aplicacion.getControlAcceso().getNombreNormalizado(), 
							operation.getContenidoFirma().toString(), 
							operation.getAsuntoMensajeFirmado(), null, SignedMailGenerator.Type.USER, 
							signedFile);
			        timeStampedDocument = new SMIMEMessageWrapper(null, signedFile);
			        timeStampTask = (GetTimeStampTask) new GetTimeStampTask(null, 
			    			timeStampedDocument.getTimeStampRequest(), this).execute(
			    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL));
			        if(Respuesta.SC_OK == timeStampTask.get()) {
			        	runningTask = new SendFileTask(null, this, 
								timeStampedDocument.setTimeStampToken(timeStampTask)).
								execute(pendingOperation.getUrlEnvioDocumento());
			        } else {
						String msg = getString(R.string.timestamp_connection_error_msg) 
								+ " - " + timeStampTask.getMessage();
						serviceListener.setPublishServiceMsg(timeStampTask.getStatusCode(), msg);
			        }
					break;
					default:
						Log.d(TAG + ".processOperation(...) ", " --- unknown operation: " + pendingOperation.getTipo().toString());
			}
        } catch(VotingSystemKeyStoreException ex) {
        	ex.printStackTrace();
        	serviceListener.setPublishServiceMsg(
        			Respuesta.SC_ERROR_EJECUCION, getString(R.string.pin_error_msg) );
        } catch(Exception ex) {
        	ex.printStackTrace();
        	serviceListener.setPublishServiceMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
        }

    }
    

	@Override
	public void processTaskMessages(List<String> messages, AsyncTask task) { }
	
	@Override
	public void showTaskResult(AsyncTask task) {
		Log.d(TAG + ".showTaskResult(...)", " - task: " + task.getClass());
		if(task instanceof SendFileTask) {
			SendFileTask sendFileTask = (SendFileTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - sendFileTask - statusCode: " 
					+ sendFileTask.getStatusCode());
	        if (Respuesta.SC_OK == sendFileTask.getStatusCode()) {
                try {
                	if(pendingOperation.isRespuestaConRecibo()) {
                		/*SMIMEMessageWrapper receipt = new SMIMEMessageWrapper(null,
    							new ByteArrayInputStream(response.getBytes()), null);
                		serviceListener.setPublishServiceMsg(Respuesta.SC_OK, null);
                		serviceListener.proccessReceipt(receipt);*/
                	}
                	serviceListener.setPublishServiceMsg(sendFileTask.getStatusCode(), null);
				} catch (Exception ex) {
					ex.printStackTrace();
					String msg = getString(R.string.receipt_error_msg) 
							+ ": " + ex.getMessage();
					serviceListener.setPublishServiceMsg(Respuesta.SC_ERROR_EJECUCION, msg);
				}
	        } else serviceListener.setPublishServiceMsg(
	        		sendFileTask.getStatusCode(), sendFileTask.getMessage());
		} else if(task instanceof SignTimestampSendPDFTask) {
			SignTimestampSendPDFTask signTimestampSendPDFTask = (SignTimestampSendPDFTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - statusCode: " 
					+ signTimestampSendPDFTask.getStatusCode());
			serviceListener.setPublishServiceMsg(signTimestampSendPDFTask.getStatusCode(), 
					signTimestampSendPDFTask.getMessage());
		}
    	
		
	}

}