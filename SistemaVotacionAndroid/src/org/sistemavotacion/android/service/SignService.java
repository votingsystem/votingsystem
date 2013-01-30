package org.sistemavotacion.android.service;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;
import static org.sistemavotacion.android.Aplicacion.TIMESTAMP_VOTE_HASH;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.GetTimeStampTask;
import org.sistemavotacion.task.SendFileTask;
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

public class SignService extends Service implements TaskListener {
	
	public static final String TAG = "SignService";
    
    private SignServiceListener signServiceListener;;
    private AsyncTask runningTask = null;
    private SMIMEMessageWrapper timeStampedDocument;
    private boolean isWithSignedReceipt = false;
    private String serverURL;
    private int operationId = 1;

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
    
    DataListener<String> sendFileListener = new DataListener<String>() {

    	@Override public void updateData(int statusCode, String response) {
			Log.d(TAG + ".sendFileListener.updateData(...) ",	" --- statusCode: " + statusCode);
	        if (Respuesta.SC_OK == statusCode) {
                try {
                	if(isWithSignedReceipt) {
                		SMIMEMessageWrapper receipt = new SMIMEMessageWrapper(null,
    							new ByteArrayInputStream(response.getBytes()), null);
                		//signServiceListener.proccessReceipt(receipt);
                		signServiceListener.setSignServiceMsg(Respuesta.SC_OK, null);
                	}
				} catch (Exception ex) {
					Log.e(TAG + ".sendFileListener.updateData(...)", ex.getMessage(), ex);
					String msg = getString(R.string.receipt_error_msg) 
							+ ": " + ex.getMessage();
					setException(msg);
				}
	        } else setException(response);
		}
		
    	@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".sendFileListener.setException(...) ", " - exceptionMsg: " + exceptionMsg);
			signServiceListener.setSignServiceMsg(Respuesta.SC_ERROR_EJECUCION, exceptionMsg);
		}
    };
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
    
    public void processSignature(String signatureContent, String subject, 
    		String serverURL, SignServiceListener signServiceListener, boolean isWithSignedReceipt,
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".processSignature(...)", " - processSignature");
    	this.signServiceListener = signServiceListener;
    	this.serverURL = serverURL;
    	this.isWithSignedReceipt = isWithSignedReceipt;
    	String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
		SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
				keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
        File signedFile = File.createTempFile("signedDocument", SIGNED_PART_EXTENSION);;
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
					runningTask = new SendFileTask(sendFileListener, 
							timeStampedDocument.setTimeStampToken(
							timeStampTask)).execute(serverURL);
				} catch (Exception ex) {
					Log.e(TAG + ".showTaskResult(...)", ex.getMessage(), ex);
				};
			}
		}
	}

}