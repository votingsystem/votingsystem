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
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
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
    private SMIMEMessageWrapper timeStampedDocument;
    private boolean isWithSignedReceipt = false;
    private String serverURL;
    private String urlSignedDocument;
    private byte[] keyStoreBytes;
    private char[] password;
    private int operationId = 1;

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
    
    public void publishPDF (String urlDocument, String urlSignedDocument, 
    		PublishServiceListener serviceListener,
    		byte[] keyStoreBytes, char[] password) {
    	Log.d(TAG + ".publicarPDF(...)", " - urlDocumento: " + urlDocument 
    			+ " - urlSignedDocument:" + urlSignedDocument);
    	this.keyStoreBytes = keyStoreBytes;
    	this.password = password;
    	this.urlSignedDocument = urlSignedDocument;
		runningTask = new GetFileTask(null, this).execute(urlDocument);
        /*progressLabel.setText("<html>" + getString("obteniendoDocumento") +"</html>");
        mostrarPantallaEnvio(true);
        new ObtenerArchivoWorker(urlDocumento, this).execute();
        setVisible(true);*/
    }
    
    public void processSignature(String signatureContent, String subject, 
    		String serverURL, PublishServiceListener serviceListener, boolean isWithSignedReceipt,
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processSignature(...)", " - processSignature");
    	this.serviceListener = serviceListener;
    	this.serverURL = serverURL;
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
	
	@Override
	public void showTaskResult(AsyncTask task) {
		Log.d(TAG + ".showTaskResult(...)", " - task: " + task.getClass());
		if(task instanceof GetTimeStampTask) {
			GetTimeStampTask timeStampTask = (GetTimeStampTask)task;
			if(Respuesta.SC_OK == timeStampTask.getStatusCode()) {
				try {
					runningTask = new SendFileTask(null, this, 
							timeStampedDocument.setTimeStampToken(
							timeStampTask)).execute(serverURL);
				} catch (Exception ex) {
					Log.e(TAG + ".showTaskResult(...)", ex.getMessage(), ex);
				};
			}
		} else if(task instanceof GetFileTask) {
			GetFileTask getFileTask = (GetFileTask)task;
			if(Respuesta.SC_OK == getFileTask.getStatusCode()) {
				try {
					PdfReader pdfFile = new PdfReader(getFileTask.getFileData());
					File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", SIGNED_PART_EXTENSION);
					KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
					PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
					X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
					Certificate[] signerCertsChain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
					
			    	runningTask = new SignTimestampSendPDFTask(this, null,
			    			ServerPaths.getURLTimeStampService(CONTROL_ACCESO_URL), null, null, signerCert,
			    			signerPrivatekey, signerCertsChain, pdfFile, pdfFirmadoFile, this).execute(urlSignedDocument);
				} catch(Exception ex) {
					ex.printStackTrace();
				}

		    	
		    	/*Context context, Integer id, String urlTimeStampServer, 
	    		String reason, String location, X509Certificate signerCert, PrivateKey signerPrivatekey, 
	    		Certificate[] signerCertsChain, PdfReader reader, File signedFile, TaskListener listener)*/
		    	
		    	
				/*File root = Environment.getExternalStorageDirectory();
    			String fileName = StringUtils.getCadenaNormalizada(getFileTask.getDocumentUrl()) +".pdf";
    			File pdfFirmadoFile = new File(root, fileName);
    	        
    	        try {
	                
	                Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
	    			PdfUtils.firmar(pdfFile, new FileOutputStream(pdfFirmadoFile), key, chain);
	    	        SendFileTask enviarArchivoTask = new SendFileTask(sendFileListener, pdfFirmadoFile);
	    	        runningTask = enviarArchivoTask.execute(urlSignedDocument);
    	        } catch (Exception ex) {
    				ex.printStackTrace();
    				serviceListener.setPublishServiceMsg(Respuesta.SC_ERROR_EJECUCION, ex.getMessage());
    			}*/
			} else {
				serviceListener.setPublishServiceMsg(
						getFileTask.getStatusCode(), getFileTask.getMessage());
			}
			
		} else if(task instanceof SignTimestampSendPDFTask) {
			SignTimestampSendPDFTask signTimestampSendPDFTask = (SignTimestampSendPDFTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - SignTimestampSendPDFTask status code: " 
					+ signTimestampSendPDFTask.getStatusCode());
		} else if(task instanceof SendFileTask) {
			SendFileTask sendFileTask = (SendFileTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - sendFileTask - statusCode: " 
					+ sendFileTask.getStatusCode());
	        if (Respuesta.SC_OK == sendFileTask.getStatusCode()) {
                try {
                	if(isWithSignedReceipt) {
                		/*SMIMEMessageWrapper receipt = new SMIMEMessageWrapper(null,
    							new ByteArrayInputStream(response.getBytes()), null);
                		signServiceListener.setPublishServiceMsg(Respuesta.SC_OK, null);
                		signServiceListener.proccessReceipt(receipt);*/
                	}
				} catch (Exception ex) {
					ex.printStackTrace();
					String msg = getString(R.string.receipt_error_msg) 
							+ ": " + ex.getMessage();
					serviceListener.setPublishServiceMsg(Respuesta.SC_ERROR_EJECUCION, msg);
				}
	        } else serviceListener.setPublishServiceMsg(
	        		sendFileTask.getStatusCode(), sendFileTask.getMessage());
		}
		 
		
	}

}