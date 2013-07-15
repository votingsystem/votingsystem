package org.sistemavotacion.callable;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.CMSSignedData;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.HttpHelper;
import org.sistemavotacion.util.ServerPaths;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<Respuesta> {
    
	public static final String TAG = "MessageTimeStamper";
    
    private SMIMEMessageWrapper smimeMessage;
    private static final int numMaxAttempts = 3;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
      
    public MessageTimeStamper (SMIMEMessageWrapper smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest) throws Exception {
        this.timeStampRequest = timeStampRequest;
    }
        
    public MessageTimeStamper (String timeStampDigestAlgorithm, 
    		byte[] digestToTimeStamp) throws Exception {
    	TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        this.timeStampRequest = reqgen.generate(
        		timeStampDigestAlgorithm, digestToTimeStamp);
    }
    
        
    @Override public Respuesta call() throws Exception {
        //byte[] base64timeStampRequest = Base64.encode(timeStampRequest.getEncoded());        
        int numAttemp = 0;
        AtomicBoolean done = new AtomicBoolean(false);
        Respuesta respuesta = null;
        while(!done.get()) {
        	String timeStampServiceURL = ServerPaths.getURLTimeStampService(
        			Aplicacion.CONTROL_ACCESO_URL);
            HttpResponse response = HttpHelper.sendByteArray(
            		timeStampRequest.getEncoded(), "timestamp-query", timeStampServiceURL);
            respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
            		response.getStatusLine().toString());
            if(Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] bytesToken = EntityUtils.toByteArray(response.getEntity());
                timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
                X509Certificate timeStampCert = Aplicacion.getControlAcceso().getTimeStampCert();

                /* -> Android project config problem
                 * SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(Aplicacion.PROVIDER).build(timeStampCert); 
                timeStampToken.validate(timeStampSignerInfoVerifier);*/
                timeStampToken.validate(timeStampCert, Aplicacion.PROVIDER);/**/
                if(smimeMessage != null)
                	smimeMessage.setTimeStampToken(timeStampToken);
                done.set(true);
            } else if(Respuesta.SC_ERROR_TIMESTAMP == 
            		response.getStatusLine().getStatusCode()) {
                if(numAttemp < numMaxAttempts) {
                    ++numAttemp;
                	Log.e(TAG + ".call(...)", "Error getting timestamp - attemp: " + numAttemp);
                } else done.set(true);
                respuesta.setMensaje(EntityUtils.toString(response.getEntity()));
            } else {
            	done.set(true);
            	respuesta.setMensaje(EntityUtils.toString(response.getEntity()));
            } 
            
        }
        return respuesta;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

}