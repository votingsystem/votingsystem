package org.sistemavotacion.android.service;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.VotingSystemKeyStoreException;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.task.SMIMESignedSenderTask;
import org.sistemavotacion.task.SendFileTask;
import org.sistemavotacion.task.SignTimestampSendPDFTask;
import org.sistemavotacion.util.PdfUtils;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.itextpdf.text.pdf.PdfReader;

public class SignService extends Service {
	
	public static final String TAG = "SignService";

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
    
    public void processPDFSignature (Integer requestId,String urlDocumentToSign, 
    		String urlToSendSignedDocument, byte[] keyStoreBytes, char[] password, 
    		ServiceListener serviceListener) throws Exception {
    	Log.d(TAG + ".processPDFSignature(...)", " - processPDFSignature");

		GetDataTask getDataTask = new GetDataTask(Aplicacion.PDF_CONTENT_TYPE);
		getDataTask.execute(urlDocumentToSign);
		Respuesta respuesta = getDataTask.get();
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
        	serviceListener.proccessResponse(requestId, respuesta);
        	return;
        }
        
		/*File root = Environment.getExternalStorageDirectory();
		File pdfFirmadoFile = new File(root, 
				Aplicacion.MANIFEST_FILE_NAME + "_" + evento.getEventoId() +".pdf");*/
        try {
    		File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
    		pdfFirmadoFile.deleteOnExit();
			Log.d(TAG + ".signPDF(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
            PdfReader pdfReader;
        	pdfReader = new PdfReader(respuesta.getMessageBytes());
        	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey key = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
            Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
			PdfUtils.firmar(pdfReader, new FileOutputStream(pdfFirmadoFile), key, chain);
	        SendFileTask sendFileTask = (SendFileTask) new SendFileTask(
	        		pdfFirmadoFile).execute(urlToSendSignedDocument);
	        respuesta = sendFileTask.get();
	        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
	        	serviceListener.proccessResponse(requestId, new Respuesta(
	        			Respuesta.SC_OK, getString(R.string.operacion_ok_msg)));
	        } else {
	        	serviceListener.proccessResponse(requestId, new Respuesta(
	        			Respuesta.SC_ERROR, respuesta.getMensaje()));
	        }
        } catch (Exception ex) {
			ex.printStackTrace();
			serviceListener.proccessResponse(requestId, new Respuesta(
        			Respuesta.SC_ERROR, ex.getMessage()));
		}

    }
    
    public void processTimestampedPDFSignature (Integer requestId, 
    		String urlDocumentToSign, String urlToSendSignedDocument, byte[] keyStoreBytes, char[] password, 
    		ServiceListener serviceListener) throws Exception {
    	Log.d(TAG + ".processTimestampedPDFSignature(...)", " - processTimestampedPDFSignature");
		GetDataTask getDataTask = new GetDataTask(Aplicacion.PDF_CONTENT_TYPE);
		getDataTask.execute(urlDocumentToSign);
		Respuesta respuesta = getDataTask.get();
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
        	serviceListener.proccessResponse(requestId, respuesta);
        	return;
        }
		try {
			PdfReader pdfFile = new PdfReader(respuesta.getMessageBytes());
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
			//X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
			Certificate[] signerCertsChain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
			
			SignTimestampSendPDFTask signTimestampSendPDFTask =	new SignTimestampSendPDFTask(
					this, null, null, signerPrivatekey, signerCertsChain, pdfFile);	
			
			signTimestampSendPDFTask.execute(urlToSendSignedDocument);
			respuesta = signTimestampSendPDFTask.get();
			serviceListener.proccessResponse(requestId, respuesta);
		} catch(Exception ex) {
			ex.printStackTrace();
			serviceListener.proccessResponse(requestId, new Respuesta(
	    			Respuesta.SC_ERROR, ex.getMessage()));
		}
    }
    
    public void processSignature(Integer requestId, String signatureContent, String subject, 
    		String urlToSendSignedDocument, ServiceListener signServiceListener,
    		boolean isEncryptedResponse, byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processSignature(...)", " - processSignature - isEncryptedResponse: " + isEncryptedResponse);
    	String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
        SMIMEMessageWrapper smimeMessage = null;
        try {
    		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
    				keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
            smimeMessage = signedMailGenerator.genMimeMessage(usuario, 
    				Aplicacion.getControlAcceso().getNombreNormalizado(), 
    				signatureContent, subject, null);
            
        } catch(VotingSystemKeyStoreException ex) {
        	ex.printStackTrace();
        	signServiceListener.proccessResponse(requestId, new Respuesta(
        			Respuesta.SC_ERROR, getString(R.string.pin_error_msg)));
        	return;
        }
        
        KeyPair keypair = null;
        if(isEncryptedResponse) {
        	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
            Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
            PublicKey publicKey = ((X509Certificate)chain[0]).getPublicKey();
            keypair = new KeyPair(publicKey, privateKey);
        }
        
        SMIMESignedSenderTask signedSenderTask = new SMIMESignedSenderTask(
        		smimeMessage, keypair, Aplicacion.getControlAcceso().getCertificado());
        signedSenderTask.execute(urlToSendSignedDocument);
        
        Respuesta response = signedSenderTask.get();
        signServiceListener.proccessResponse(requestId, response);
        
    }

}