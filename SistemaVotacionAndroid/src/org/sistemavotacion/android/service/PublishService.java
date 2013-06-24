package org.sistemavotacion.android.service;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.VotingSystemKeyStoreException;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.task.SMIMESignedSenderTask;
import org.sistemavotacion.task.SignTimestampSendPDFTask;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.itextpdf.text.pdf.PdfReader;

public class PublishService extends Service {
	
	public static final String TAG = "PublishService";

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
    
    public void publishDocument (Integer requestId, Operation operation,
    		byte[] keyStoreBytes, char[] password, ServiceListener serviceListener) {
    	Log.d(TAG + ".publishDocument(...)", " operation: " + operation.getTipo());
    	this.pendingOperation = operation;
    	String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
        SignedMailGenerator signedMailGenerator = null;
        SMIMEMessageWrapper smimeMessage        = null;
        SMIMESignedSenderTask signedSenderTask  = null;
        Respuesta respuesta                     = null;
        try {
            if(pendingOperation.getTipo() != null && pendingOperation.getTipo() != 
            		Operation.Tipo.PUBLICACION_MANIFIESTO_PDF) {
                signedMailGenerator = new SignedMailGenerator(
    					keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
            }
			KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
			PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
			//X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(ALIAS_CERT_USUARIO);
    		switch(pendingOperation.getTipo()) {
				case PUBLICACION_MANIFIESTO_PDF:
					GetDataTask getDataTask = new GetDataTask(Aplicacion.PDF_CONTENT_TYPE);
					getDataTask.execute(pendingOperation.getUrlDocumento());
					respuesta = getDataTask.get();
		            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
		            	serviceListener.proccessResponse(requestId, respuesta);
		            	return;
		            } 
					try {
						PdfReader pdfFile = new PdfReader(respuesta.getMessageBytes());
						Certificate[] signerCertsChain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
						SignTimestampSendPDFTask signTimestampSendPDFTask = (SignTimestampSendPDFTask) 
								new SignTimestampSendPDFTask(this, null, null,
										signerPrivatekey, signerCertsChain, pdfFile);				
						
						signTimestampSendPDFTask.execute(pendingOperation.getUrlEnvioDocumento());
						
						respuesta = signTimestampSendPDFTask.get();
						serviceListener.proccessResponse(requestId, respuesta);
						return;
					} catch(Exception ex) {
						ex.printStackTrace();
						serviceListener.proccessResponse(requestId, new Respuesta(
								Respuesta.SC_ERROR, ex.getMessage()));
					}
					break;
				case PUBLICACION_VOTACION_SMIME:
				case PUBLICACION_RECLAMACION_SMIME:
				case ASOCIAR_CENTRO_CONTROL_SMIME:
					smimeMessage = signedMailGenerator.genMimeMessage(usuario, 
							Aplicacion.getControlAcceso().getNombreNormalizado(), 
							operation.getContenidoFirma().toString(), 
							operation.getAsuntoMensajeFirmado(), null);
			        signedSenderTask = new SMIMESignedSenderTask(
			        		smimeMessage, null,	Aplicacion.getControlAcceso().getCertificado());
			        signedSenderTask.execute(pendingOperation.getUrlEnvioDocumento());
			        
			        respuesta = signedSenderTask.get();
			        serviceListener.proccessResponse(requestId, respuesta);
					break;
				default:
					Log.d(TAG + ".processOperation(...) ", " --- unknown operation: " + pendingOperation.getTipo().toString());
			}
        } catch(VotingSystemKeyStoreException ex) {
        	ex.printStackTrace();
        	serviceListener.proccessResponse(requestId, new Respuesta(
        			Respuesta.SC_ERROR, getString(R.string.pin_error_msg)));
        } catch(Exception ex) {
        	ex.printStackTrace();
        	serviceListener.proccessResponse(requestId, new Respuesta(
        			Respuesta.SC_ERROR, ex.getMessage()));
        }
    }
    
}