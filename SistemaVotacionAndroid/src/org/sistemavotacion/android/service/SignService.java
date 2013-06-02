package org.sistemavotacion.android.service;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;
import static org.sistemavotacion.android.Aplicacion.TIMESTAMP_VOTE_HASH;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
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
import org.sistemavotacion.util.PdfUtils;
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

public class SignService extends Service implements TaskListener {
	
	public static final String TAG = "SignService";
    private SignServiceListener signServiceListener;

	private IBinder iBinder = new SignServiceBinder();
	
	public class SignServiceBinder extends Binder {
		public SignService getBinder() {
			return SignService.this;
		}
	}

    @Override public IBinder onBind(Intent intent) {
    	Log.d(TAG + ".onBind(...) ", " *** SignService - onBind ");
		return iBinder;
    }

	@Override public boolean onUnbind(Intent intent) {
    	Log.d(TAG + ".onBind(...) ", " *** SignService - onUnbind ");
		/*try { th.interrupt();} catch (Exception e) {}*/
		signServiceListener = null;
		return super.onUnbind(intent);
	}

	@Override public void unbindService(ServiceConnection conn) {
    	Log.d(TAG + ".onBind(...) ", " *** SignService - unbindService ");
		super.unbindService(conn);
	}
	
    @Override public void onCreate() {
    	Log.d(TAG + ".onCreate(...) ", " *** SignService - onCreate ");
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
    
    public void processPDFSignature (String urlDocumentToSign, 
    		String urlToSendSignedDocument, byte[] keyStoreBytes, char[] password, 
    		SignServiceListener serviceListener) throws Exception {
    	Log.d(TAG + ".processPDFSignature(...)", " - processPDFSignature");

    	this.signServiceListener = serviceListener;
    	GetFileTask getFileTask = (GetFileTask)new GetFileTask(
    			null, this).execute(urlDocumentToSign);
    	if(Respuesta.SC_OK == getFileTask.get()) {
    		/*File root = Environment.getExternalStorageDirectory();
    		File pdfFirmadoFile = new File(root, 
    				Aplicacion.MANIFEST_FILE_NAME + "_" + evento.getEventoId() +".pdf");*/
            try {
        		File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
        		pdfFirmadoFile.deleteOnExit();
    			Log.d(TAG + ".signPDF(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
                PdfReader pdfReader;
            	pdfReader = new PdfReader(getFileTask.getFileData());
            	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
                PrivateKey key = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
                Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
    			PdfUtils.firmar(pdfReader, new FileOutputStream(pdfFirmadoFile), key, chain);
    	        SendFileTask sendFileTask = (SendFileTask) new SendFileTask(
    	        		null, this, pdfFirmadoFile).execute(urlToSendSignedDocument);
    	        if(Respuesta.SC_OK == sendFileTask.get()) {
    	        	signServiceListener.setSignServiceMsg(
                			sendFileTask.getStatusCode(), getString(R.string.operacion_ok_msg));
    	        } else {
    	        	signServiceListener.setSignServiceMsg(sendFileTask.getStatusCode(), 
    		        		sendFileTask.getMessage());
    	        }
            } catch (Exception ex) {
    			ex.printStackTrace();
    			signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
    		}
    	} else signServiceListener.setSignServiceMsg(getFileTask.getStatusCode(), 
				getFileTask.getMessage());
    }
    
    public void processTimestampedPDFSignature (String urlDocumentToSign, 
    		String urlToSendSignedDocument, byte[] keyStoreBytes, char[] password, 
    		SignServiceListener serviceListener) throws Exception {
    	Log.d(TAG + ".processTimestampedPDFSignature(...)", " - processTimestampedPDFSignature");
    	this.signServiceListener = serviceListener;
    	GetFileTask getFileTask = (GetFileTask)new GetFileTask(null, this).execute(urlDocumentToSign);
    	if(Respuesta.SC_OK == getFileTask.get()) {
    		try {
    			PdfReader pdfFile = new PdfReader(getFileTask.getFileData());
    			File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
    			pdfFirmadoFile.deleteOnExit();
    			Log.d(TAG + ".signTimestampedPDF(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
    			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
    			PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
    			//X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
    			Certificate[] signerCertsChain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
    			
    			SignTimestampSendPDFTask signTimestampSendPDFTask = (SignTimestampSendPDFTask)
    					new SignTimestampSendPDFTask(this, null,
    	    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL), null, null,
    	    			signerPrivatekey, signerCertsChain, pdfFile, pdfFirmadoFile, this).execute(
    	    			urlToSendSignedDocument);
    		} catch(Exception ex) {
    			ex.printStackTrace();
    			signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
    		}
    	} else signServiceListener.setSignServiceMsg(getFileTask.getStatusCode(), 
				getFileTask.getMessage()); 
    }
    
    public void processSignature(String signatureContent, String subject, 
    		String urlToSendSignedDocument, SignServiceListener signServiceListener, boolean isWithSignedReceipt,
    		boolean isEncryptedResponse, byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processSignature(...)", " - processSignature - isWithSignedReceipt: " + 
    			isWithSignedReceipt + " - isEncryptedResponse: " + isEncryptedResponse);
    	this.signServiceListener = signServiceListener;
    	String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
        File signedFile = null;
        try {
    		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
    				keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
    		signedFile = File.createTempFile("signedDocument", SIGNED_PART_EXTENSION);
            signedFile = signedMailGenerator.genFile(usuario, 
    				Aplicacion.getControlAcceso().getNombreNormalizado(), 
    				signatureContent, subject, null, SignedMailGenerator.Type.USER, 
    				signedFile);
        } catch(VotingSystemKeyStoreException ex) {
        	ex.printStackTrace();
        	signServiceListener.setSignServiceMsg(
        			Respuesta.SC_ERROR_EJECUCION, getString(R.string.pin_error_msg));
        	return;
        } 
        SMIMEMessageWrapper timeStampedDocument = new SMIMEMessageWrapper(null, signedFile);
        GetTimeStampTask getTimeStampTask = (GetTimeStampTask) new GetTimeStampTask(null, 
    			timeStampedDocument.getTimeStampRequest(), this).execute(
    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL));
        if(Respuesta.SC_OK == getTimeStampTask.get()) {
        	try {
            	File fileToEncrypt = timeStampedDocument.setTimeStampToken(getTimeStampTask);
            	EncryptionHelper.encryptSMIMEFile(fileToEncrypt, 
            			Aplicacion.getControlAcceso().getCertificado());
        		SendFileTask sendFileTask = (SendFileTask)new SendFileTask(null, this, 
        				fileToEncrypt).execute(urlToSendSignedDocument);
        		if (Respuesta.SC_OK == sendFileTask.get()) {
                    try {
                    	if(isEncryptedResponse) {
                    		signServiceListener.proccessEncryptedResponse(
                    				sendFileTask.getMessage().getBytes());
                    	} else {
                    		 if(isWithSignedReceipt) {
                         		SMIMEMessageWrapper receipt = new SMIMEMessageWrapper(null,
            							new ByteArrayInputStream(sendFileTask.getMessage().getBytes()), null);
                        		signServiceListener.setSignServiceMsg(Respuesta.SC_OK, null);
                        		signServiceListener.proccessReceipt(receipt); 
                    		 } else {
                    			 signServiceListener.setSignServiceMsg(
                             			sendFileTask.getStatusCode(), getString(R.string.operacion_ok_msg));
                    		 }
                    	}
    				} catch (Exception ex) {
    					ex.printStackTrace();
    					String msg = getString(R.string.receipt_error_msg) 
    							+ ": " + ex.getMessage();
    					signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, msg);
    				}
    	        } else signServiceListener.setSignServiceMsg(sendFileTask.getStatusCode(), 
    	        		sendFileTask.getMessage());
			} catch (Exception ex) {
				ex.printStackTrace();
				signServiceListener.setSignServiceMsg(
						Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
			};
        } else {
        	signServiceListener.setSignServiceMsg(
        			getTimeStampTask.getStatusCode(), getTimeStampTask.getMessage());
        }
    }

	@Override
	public void processTaskMessages(List<String> messages, AsyncTask task) { }
	
	
	@Override
	public void showTaskResult(AsyncTask task) { 
		Log.d(TAG + ".showTaskResult(...)", " - showTaskResult - " + task.getClass());
		if(task instanceof SignTimestampSendPDFTask) {
			SignTimestampSendPDFTask signTimestampSendPDFTask= (SignTimestampSendPDFTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - statusCode: " 
					+ signTimestampSendPDFTask.getStatusCode());    			
			signServiceListener.setSignServiceMsg(signTimestampSendPDFTask.getStatusCode(), 
	    					signTimestampSendPDFTask.getMessage());
		}
	}

}