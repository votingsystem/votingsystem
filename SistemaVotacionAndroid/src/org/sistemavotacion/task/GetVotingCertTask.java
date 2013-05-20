package org.sistemavotacion.task;

import static org.sistemavotacion.android.Aplicacion.*;
import static org.sistemavotacion.android.Aplicacion.getAppString;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Header;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.EncryptionHelper;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SignedMailGenerator.Type;
import org.sistemavotacion.util.HttpHelper;

import android.os.AsyncTask;
import android.util.Log;

public class GetVotingCertTask extends AsyncTask<String, Void, Integer> {

	public static final String TAG = "GetVotingCertTask";
	
	TaskListener listener = null;
    File solicitudAcceso;
    private int statusCode = Respuesta.SC_ERROR_PETICION;
    private Exception exception = null;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private String message = null;

    
    public GetVotingCertTask(TaskListener listener, File solicitudAcceso, 
    		PKCS10WrapperClient pkcs10WrapperClient) throws Exception {
		this.solicitudAcceso = solicitudAcceso;
		this.pkcs10WrapperClient = pkcs10WrapperClient;
		this.listener = listener;
    }
	
	@Override
	protected Integer doInBackground(String... urls) {
		String url = urls[0];
        Log.d(TAG + ".doInBackground", " - doInBackground - url:" + url);
        try {
        	//File csrFile = File.createTempFile("csrFile", ".csr");
        	//csrFile.deleteOnExit();
        	File csrEncryptedFile = File.createTempFile("csrEncryptedFile", ".p7m");
        	csrEncryptedFile.deleteOnExit();
        	//FileUtils.copyStreamToFile(new ByteArrayInputStream(
        	//		pkcs10WrapperClient.getPEMEncodedRequestCSR()), csrFile);
        	EncryptionHelper.encryptText(pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
        			csrEncryptedFile, Aplicacion.getControlAcceso().getCertificado());
        	EncryptionHelper.encryptSMIMEFile(solicitudAcceso, 
        			Aplicacion.getControlAcceso().getCertificado());
        	
            String accessRequestFileName = ACCESS_REQUEST_FILE_NAME + ":" + 
                    SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(CSR_FILE_NAME, csrEncryptedFile);
            mapToSend.put(accessRequestFileName, solicitudAcceso);
        	
            HttpResponse response = HttpHelper.sendObjectMap(mapToSend, url);
            statusCode = response.getStatusLine().getStatusCode();
            if (Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] encryptedData = EntityUtils.toByteArray(response.getEntity());
                byte[] decryptedData = EncryptionHelper.decryptMessage(
                        encryptedData, null, pkcs10WrapperClient.getPrivateKey());
            	
            	
                pkcs10WrapperClient.initSigner((decryptedData), VOTE_SIGN_MECHANISM);
            } else if(Respuesta.SC_ERROR_VOTO_REPETIDO == 
            		response.getStatusLine().getStatusCode()) {
            	message = EntityUtils.toString(response.getEntity());
            } else {
            	message = response.getStatusLine().toString();
            }
            
        } catch (Exception ex) {
        	Log.e(TAG + ".doInBackground", ex.getMessage(), ex);
        	exception = ex;
        }
        return getStatusCode();
	}
	
    public File genSignedFile(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType, File outputFile) throws Exception {
    	if(Respuesta.SC_OK != statusCode) throw new Exception(
    			getAppString(R.string.vote_cert_request_error_msg)) ;
    	return pkcs10WrapperClient.genSignedFile(fromUser, toUser,
    			textoAFirmar,asunto, header, signerType, outputFile);
    }
	
    @Override
    protected void onPostExecute(Integer statusCode) {
    	Log.d(TAG + ".onPostExecute", " - statusCode: " + statusCode);
    	listener.showTaskResult(this);
    }

	public PKCS10WrapperClient getPkcs10WrapperClient() {
		return pkcs10WrapperClient;
	}

	public String getMessage() {
		if(exception != null) return exception.getMessage();
		return message;
	}

	public int getStatusCode() {
		return statusCode;
	}

	
}