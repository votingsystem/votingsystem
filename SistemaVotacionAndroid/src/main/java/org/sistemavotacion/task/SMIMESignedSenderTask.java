package org.sistemavotacion.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.callable.MessageTimeStamper;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.HttpHelper;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class SMIMESignedSenderTask extends AsyncTask<String, Void, Respuesta> {

	public static final String TAG = "SMIMESignedSenderTask";

    private SMIMEMessageWrapper smimeMessage = null;
    private X509Certificate destinationCert = null;
    private KeyPair keypair;
    private Context context = null;
    
    public SMIMESignedSenderTask(SMIMEMessageWrapper smimeMessage, 
    		KeyPair keypair, X509Certificate destinationCert, Context context) {
		this.smimeMessage = smimeMessage;
		this.destinationCert = destinationCert;
		this.keypair = keypair;
        this.context = context;
    }
	
	@Override
	protected Respuesta doInBackground(String... urls) {
        Log.d(TAG + ".doInBackground", " - doInBackground - url: " + urls[0]);
        String serviceURL = urls[0];
        MessageTimeStamper timeStamper = null;
        Respuesta respuesta = null;
        try {
            timeStamper = new MessageTimeStamper(smimeMessage, context);
            respuesta = timeStamper.call();
        } catch(Exception ex) {
        	ex.printStackTrace();
        	respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
        //=== OJO, descomentar esto!!!
        //if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
        
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
        	
        } else smimeMessage = timeStamper.getSmimeMessage();
        Log.d(TAG + ".doInBackground", "=========================");
        
        
        //smimeMessage = timeStamper.getSmimeMessage();
        try {
        	String documentContentType = null;
        	byte[] messageToSend = null;
            if(destinationCert != null) {
            	messageToSend = Encryptor.encryptSMIME(
                        smimeMessage, destinationCert);
                documentContentType = AppData.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
	        } else {
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        	smimeMessage.writeTo(baos);
	        	messageToSend = baos.toByteArray();
	        	baos.close();
                documentContentType = AppData.SIGNED_CONTENT_TYPE;
	        }
            HttpResponse response  = HttpHelper.sendByteArray(
            		messageToSend, documentContentType, serviceURL);  
            if(Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
            	byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                if(keypair != null) {
                	SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                    		responseBytes, keypair.getPublic(), keypair.getPrivate());
                    respuesta.setSmimeMessage(signedMessage);
                } else return new Respuesta(Respuesta.SC_OK, responseBytes);
            } else respuesta = new Respuesta(Respuesta.SC_ERROR, 
            		EntityUtils.toByteArray(response.getEntity()));
        } catch(Exception ex) {
        	ex.printStackTrace();
        	respuesta = new Respuesta(
        			Respuesta.SC_ERROR_EXCEPCION, ex.getMessage());
        }
        return respuesta;
	}
	
    @Override  protected void onPostExecute(Respuesta respuesta) {
    	Log.d(TAG + ".onPostExecute", " - statuscode: " + 
				respuesta.getCodigoEstado());
    }

	public SMIMEMessageWrapper getSmimeMessage() {
		return smimeMessage;
	}

}