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
	
    private static final int PDF_TIMESTAMPED_SIGNATURE    = 0;
    private static final int PDF_SIGNATURE      = 1;
    
    private SignServiceListener signServiceListener;;
    private AsyncTask runningTask = null;
    private SMIMEMessageWrapper timeStampedDocument;
    private boolean isWithSignedReceipt = false;
    private String urlDocumentToSign = null; 
    private String urlToSendSignedDocument = null;
    private int operationId = 1;
    
    private byte[] keyStoreBytes;
    private char[] password;

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
    
    public void processPDFSignature (String urlDocumentToSign, 
    		String urlToSendSignedDocument, byte[] keyStoreBytes, char[] password, 
    		SignServiceListener serviceListener) {
    	Log.d(TAG + ".processPDFSignature(...)", " - processSignature");
    	this.keyStoreBytes = keyStoreBytes;
    	this.password = password;
    	this.urlToSendSignedDocument = urlToSendSignedDocument;
    	this.urlDocumentToSign = urlDocumentToSign;
    	this.signServiceListener = serviceListener;
		runningTask = new GetFileTask(PDF_SIGNATURE, this).execute(urlDocumentToSign);
    }
    
    public void processTimestampedPDFSignature (String urlDocumentToSign, 
    		String urlToSendSignedDocument, byte[] keyStoreBytes, char[] password, 
    		SignServiceListener serviceListener) {
    	Log.d(TAG + ".processPDFSignature(...)", " - processSignature");
    	this.keyStoreBytes = keyStoreBytes;
    	this.password = password;
    	this.urlToSendSignedDocument = urlToSendSignedDocument;
    	this.urlDocumentToSign = urlDocumentToSign;
    	this.signServiceListener = serviceListener;
    	runningTask = new GetFileTask(PDF_TIMESTAMPED_SIGNATURE, this).execute(urlDocumentToSign);
    }
    
    public void processSignature(String signatureContent, String subject, 
    		String urlToSendSignedDocument, SignServiceListener signServiceListener, boolean isWithSignedReceipt,
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processSignature(...)", " - processSignature");
    	this.signServiceListener = signServiceListener;
    	this.urlToSendSignedDocument = urlToSendSignedDocument;
    	this.isWithSignedReceipt = isWithSignedReceipt;
    	String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
				keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
        File signedFile = File.createTempFile("signedDocument", SIGNED_PART_EXTENSION);
        signedFile = signedMailGenerator.genFile(usuario, 
				Aplicacion.getControlAcceso().getNombreNormalizado(), 
				signatureContent, subject, null, SignedMailGenerator.Type.USER, 
				signedFile);
		setTimeStampedDocument(operationId, signedFile, TIMESTAMP_VOTE_HASH);
    }

	@Override
	public void processTaskMessages(List<String> messages, AsyncTask task) { }
	
	
	private void signPDF(byte[] pdfFileBytes) {
		Log.d(TAG + ".signPDF(...)", " - signPDF - ");
		/*File root = Environment.getExternalStorageDirectory();
		File pdfFirmadoFile = new File(root, 
				Aplicacion.MANIFEST_FILE_NAME + "_" + evento.getEventoId() +".pdf");*/
        try {
    		File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
    		pdfFirmadoFile.deleteOnExit();
			Log.d(TAG + ".signPDF(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
            PdfReader pdfReader;
        	pdfReader = new PdfReader(pdfFileBytes);
        	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey key = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
            Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
			PdfUtils.firmar(pdfReader, new FileOutputStream(pdfFirmadoFile), key, chain);
	        SendFileTask enviarArchivoTask = new SendFileTask(PDF_SIGNATURE, this, pdfFirmadoFile);
	        runningTask = enviarArchivoTask.execute(urlToSendSignedDocument);
        } catch (Exception ex) {
			ex.printStackTrace();
			signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
		}
	}
	
	private void signTimestampedPDF(byte[] pdfFileBytes) {
		try {
			PdfReader pdfFile = new PdfReader(pdfFileBytes);
			File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
			pdfFirmadoFile.deleteOnExit();
			Log.d(TAG + ".signTimestampedPDF(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
			X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
			Certificate[] signerCertsChain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
			
	    	runningTask = new SignTimestampSendPDFTask(this, null,
	    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL), null, null, signerCert,
	    			signerPrivatekey, signerCertsChain, pdfFile, pdfFirmadoFile, this).execute(
	    			urlToSendSignedDocument);
		} catch(Exception ex) {
			ex.printStackTrace();
			signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
		}
	}
	
	
	@Override
	public void showTaskResult(AsyncTask task) {
		Log.d(TAG + ".showTaskResult(...)", " - task: " + task.getClass());
		if(task instanceof GetTimeStampTask) {
			GetTimeStampTask timeStampTask = (GetTimeStampTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - timeStampTask - statusCode: " 
					+ timeStampTask.getStatusCode());
			if(Respuesta.SC_OK == timeStampTask.getStatusCode()) {
				try {
					runningTask = new SendFileTask(null, this, 
							timeStampedDocument.setTimeStampToken(
							timeStampTask)).execute(urlToSendSignedDocument);
				} catch (Exception ex) {
					ex.printStackTrace();
					signServiceListener.setSignServiceMsg(
							Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
				};
			}
		} else if(task instanceof SendFileTask) {
			SendFileTask sendFileTask = (SendFileTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - sendFileTask - statusCode: " 
					+ sendFileTask.getStatusCode());
	        if (Respuesta.SC_OK == sendFileTask.getStatusCode()) {
                try {
                	if(isWithSignedReceipt) {
                		SMIMEMessageWrapper receipt = new SMIMEMessageWrapper(null,
    							new ByteArrayInputStream(sendFileTask.getMessage().getBytes()), null);
                		signServiceListener.setSignServiceMsg(Respuesta.SC_OK, null);
                		signServiceListener.proccessReceipt(receipt);
                	} else signServiceListener.setSignServiceMsg(
                			sendFileTask.getStatusCode(), getString(R.string.operacion_ok_msg));
				} catch (Exception ex) {
					ex.printStackTrace();
					String msg = getString(R.string.receipt_error_msg) 
							+ ": " + ex.getMessage();
					signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, msg);
				}
	        } else signServiceListener.setSignServiceMsg(sendFileTask.getStatusCode(), 
	        		sendFileTask.getMessage());
		} else if(task instanceof GetFileTask) {
			GetFileTask getFileTask = (GetFileTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - getFileTask - statusCode: " 
					+ getFileTask.getStatusCode());
			if (Respuesta.SC_OK == getFileTask.getStatusCode()) {
				switch(getFileTask.getId()) {
					case PDF_SIGNATURE:
						signPDF(getFileTask.getFileData());
						break;
					case PDF_TIMESTAMPED_SIGNATURE:
						signTimestampedPDF(getFileTask.getFileData());
						break;
				}
			} else signServiceListener.setSignServiceMsg(getFileTask.getStatusCode(), 
					getFileTask.getMessage());
		}  else if (task instanceof SignTimestampSendPDFTask) {
			SignTimestampSendPDFTask signTimestampSendPDFTask = (SignTimestampSendPDFTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - SignTimestampSendPDFTask status code: " 
					+ signTimestampSendPDFTask.getStatusCode());
			signServiceListener.setSignServiceMsg(signTimestampSendPDFTask.getStatusCode(), 
					signTimestampSendPDFTask.getMessage());
		}
	}

}