/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sistemavotacion.util;

import static org.sistemavotacion.android.Aplicacion.*;

import static org.sistemavotacion.android.Aplicacion.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.Aplicacion.CONTROL_ACCESO_URL;
import static org.sistemavotacion.android.Aplicacion.KEY_SIZE;
import static org.sistemavotacion.android.Aplicacion.PROVIDER;
import static org.sistemavotacion.android.Aplicacion.SIGNATURE_ALGORITHM;
import static org.sistemavotacion.android.Aplicacion.SIG_NAME;
import static org.sistemavotacion.android.Aplicacion.SISTEMA_VOTACION_DIR;
import static org.sistemavotacion.android.Aplicacion.TIMESTAMP_USU_HASH;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.json.DeObjetoAJSON;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.task.DataListener;
import org.sistemavotacion.task.GetVotingCertTask;
import org.sistemavotacion.task.SendByteArrayTask;
import org.sistemavotacion.task.SendDataTask;
import org.sistemavotacion.task.SendFileTask;
import org.sistemavotacion.task.VotingListener;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class VotacionHelper implements VotingListener {
	
	public static final String TAG = "VotacionHelper";
	
	private static final int TOKEN_SOLICITUD_ACCESO = 0;
	private static final int TOKEN_VOTO = 1;
	private static int tipoSolicitud = TOKEN_SOLICITUD_ACCESO;
	
	private static SignerInformation signerInformation;
	
	private File solicitudAcceso;
    //public static final DERObjectIdentifier id_signatureTimeStampToken = new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14");
    
    private Evento event;
    private Context context;
    private VotingListener votingListener;
    private PKCS10WrapperClient pkcs10WrapperClient = null;
    private static VotacionHelper instancia;
    
    private VotacionHelper(Evento event, VotingListener votingListener, 
    		Context context) throws NoSuchAlgorithmException, 
    		InvalidKeyException, NoSuchProviderException, SignatureException, IOException {
    	this.event = prepararVoto(event);
    	this.votingListener = votingListener;
    	this.context = context;
    	pkcs10WrapperClient = PKCS10WrapperClient.buildCSRVoto(KEY_SIZE, SIG_NAME, 
		        SIGNATURE_ALGORITHM, PROVIDER, CONTROL_ACCESO_URL, 
		        event.getEventoId().toString(), event.getHashCertificadoVotoHex());
    }

    
    DataListener<byte[]> timeStampListener = new DataListener<byte[]>() {
    	
    	
    	@Override public void updateData(int statusCode, byte[] timeStampResponse) {
			Log.d(TAG + ".timeStampListener.updateData(...) ",	" - statusCode: " + statusCode);
            DERObject derObject = null;
            try {
                TimeStampToken timeStampToken = new TimeStampToken(
                        new CMSSignedData(timeStampResponse));
	            derObject = new ASN1InputStream(timeStampToken.getEncoded()).readObject();
	            
	            Calendar cal = new GregorianCalendar();
	            cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
	            Log.d(TAG + ".timeStampListener.updateData(...) ",	" - Sello de tiempo: " + 
	            		DateUtils.getStringFromDate(cal.getTime()));
	            DERSet derset = new DERSet(derObject);
	            Attribute attribute = new Attribute(PKCSObjectIdentifiers.
                		id_aa_signatureTimeStampToken, derset);
	    		switch(tipoSolicitud) {
					case TOKEN_SOLICITUD_ACCESO:
						Log.d(TAG + ".timeStampListener.updateData(...) ",	" - obtenido sello de tiempo de la solicitud");
						if(signerInformation.getUnsignedAttributes() == null) {
							Log.d(TAG + ".timeStampListener.updateData(...) ",	" signerInformation SIN UnsignedAttributes ");
	                        Hashtable hashTable = new Hashtable();
	                        hashTable.put(PKCSObjectIdentifiers.
	                        		id_aa_signatureTimeStampToken, attribute);
	                        AttributeTable attributeTable = new AttributeTable(hashTable);
							signerInformation.replaceUnsignedAttributes(signerInformation, attributeTable);
						} else {
							Log.d(TAG + ".timeStampListener.updateData(...) ",	" signerInformation CON UnsignedAttributes ");
							signerInformation.getUnsignedAttributes().add(CMSAttributes.signingTime, attribute);
						}
						proccessAccessRequest(solicitudAcceso);
						break;
					case TOKEN_VOTO:
						Log.d(TAG + ".timeStampListener.updateData(...) ",	" - obtenido sello de tiempo del voto");
						File voteSignedAndTimeStamped = pkcs10WrapperClient.getTimeStampedSignedFile(attribute);
			            votingListener.setRunningTask(new SendFileTask(voteListener, 
			            		voteSignedAndTimeStamped).execute(ServerPaths.getURLVoto(
				    			event.getCentroControl().getServerURL())));
						break;
				}
	            /*Hashtable hashtable = new Hashtable();
	            hashtable.put(id_signatureTimeStampToken, att);
	            AttributeTable attributeTable = new AttributeTable(hashtable);  */
            } catch(Exception ex) {
    			Log.e(TAG + ".timeStampListener.updateData(...) ",	ex.getMessage(), ex);
    			setException(ex.getMessage());
            }

		}
		
    	@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".timeStampListener.setException(...) ", " - exceptionMsg: " + exceptionMsg);	
			instancia.setException(exceptionMsg);
		}
    };
    
    DataListener<String> certVoteListener = new DataListener<String>() {

		@Override public void updateData(int statusCode, String data) {
			Log.d(TAG + ".voteCertListener.updateData(...)", data);
	        if(Respuesta.SC_OK == statusCode) {
		        try {
		            String votoJSON = DeObjetoAJSON.obtenerVotoParaEventoJSON(event);
		            String usuario = null;
		            if (Aplicacion.getUsuario() != null) usuario = 
		            		Aplicacion.getUsuario().getNif();
		            TimeStampRequest timeStampRequest = pkcs10WrapperClient.getTimeStampRequest(
		            		usuario, event.getCentroControl().getNombreNormalizado(),
		                    votoJSON, "[VOTO]", null, SignedMailGenerator.Type.USER, 
		                    data.getBytes());
		            tipoSolicitud = TOKEN_VOTO;
		    		votingListener.setRunningTask(new SendByteArrayTask(
		    				timeStampListener, timeStampRequest.getEncoded()).execute(
		    				ServerPaths.getURLTimeStampService(Aplicacion.CONTROL_ACCESO_URL)));
				} catch (Exception e) {
					Log.e(TAG + ".voteCertListener.updateData(...)", e.getMessage(), e);
					setException(e.getMessage());
				}	
	        } else {
	        	setException(data);
	        }
		}
		
		@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".voteCertListener.setException(...) ", " - voteCertListener: " + exceptionMsg);	
			votingListener.setException(exceptionMsg);
		}
    };
    
    DataListener<String> voteListener = new DataListener<String>() {

    	@Override public void updateData(int statusCode, String response) {
			Log.d(TAG + ".voteListener.updateData(...) ",	" - statusCode: " + statusCode);
	        if (Respuesta.SC_OK == statusCode) {
                try {
					SMIMEMessageWrapper votoValidado = new SMIMEMessageWrapper(null,
							new ByteArrayInputStream(response.getBytes()), null);
					ReciboVoto receipt = new ReciboVoto(Respuesta.SC_OK, votoValidado, event);
					votingListener.proccessReceipt(receipt);
				} catch (Exception e) {
					e.printStackTrace();
					String msg = context.getString(R.string.receipt_error_msg) 
							+ ": " + e.getMessage();
					setException(msg);
				}
	        } else setException(response);
		}
		
    	@Override public void setException(String exceptionMsg) {
			Log.d(TAG + ".voteListener.setException(...) ", " - exceptionMsg: " + exceptionMsg);
			instancia.setException(exceptionMsg);
		}
    };
  
    private static Evento prepararVoto (Evento evento) throws NoSuchAlgorithmException {    
        evento.setOrigenHashSolicitudAcceso(UUID.randomUUID().toString());
        evento.setHashSolicitudAccesoBase64(CMSUtils.obtenerHashBase64(
        		evento.getOrigenHashSolicitudAcceso(), Aplicacion.SIG_HASH));
        evento.setOrigenHashCertificadoVoto(UUID.randomUUID().toString());
        evento.setHashCertificadoVotoBase64(CMSUtils.obtenerHashBase64(
        		evento.getOrigenHashCertificadoVoto(), Aplicacion.SIG_HASH));         
        return evento;
    }
    
    public void obtenerSolicitudAcceso (byte[] keyStoreBytes, 
    		char[] password) throws NoSuchAlgorithmException, Exception {
    	Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - obtenerSolicitudAcceso");
    	String asunto = getAppString(R.string.request_msg_subject, 
        		event.getEventoId());
    	Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - asunto: " + asunto);
        String usuario = null;
        if (Aplicacion.getUsuario() != null) usuario = Aplicacion.getUsuario().getNif();
		SignedMailGenerator dnies = new SignedMailGenerator(keyStoreBytes, ALIAS_CERT_USUARIO, password);
		String contenidoFirma = DeObjetoAJSON.obtenerSolicitudAccesoJSON(event);
		solicitudAcceso = dnies.genFile(usuario, 
				Aplicacion.getControlAcceso().getNombreNormalizado(), 
				contenidoFirma, asunto, null, SignedMailGenerator.Type.USER);
		/* problema -> javax.activation.UnsupportedDataTypeException: 
		 * no object DCH for MIME type application/pkcs7-signature
		MimeMessage solicitudAccesoMimeMessage = dnies.gen(usuario, 
				Aplicacion.getControlAcceso().getNombreNormalizado(), 
				contenidoFirma, asunto, null, SignedMailGenerator.Type.USER);
		Object content = solicitudAccesoMimeMessage.getContent();
		MimeMultipart mimeMultipart = null;
	    if (content.getClass().isAssignableFrom(MimeMultipart.class)) {
	    	mimeMultipart = (MimeMultipart) content;
	    }
	    SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);*/
		/*Tambien se puede obtener el digest
		SMIMESignedGenerator gen = dnies.getSMIMESignedGenerator();
		byte[] contentDigestBytes = (byte[])gen.getGeneratedDigests().get(SMIMESignedGenerator.DIGEST_SHA1);
		String contentDigest = Base64.encodeToString(contentDigestBytes, Base64.DEFAULT);
		Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - contentDigest: " + contentDigest);*/
		SMIMESigned solicitudAccesoSMIME = SMIMEMessageWrapper.getSmimeSigned(null,
				new FileInputStream(solicitudAcceso), null);
		signerInformation = ((SignerInformation)
				solicitudAccesoSMIME.getSignerInfos().getSigners().iterator().next());
		AttributeTable table = signerInformation.getSignedAttributes();
		Attribute hash = table.get(CMSAttributes.messageDigest);
		ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
		//String digest = Base64.encodeToString(as.getOctets(), Base64.DEFAULT);
		//Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - digest: " + digest);
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        TimeStampRequest timeStampRequest = reqgen.generate(
        		TIMESTAMP_USU_HASH, as.getOctets());
        //String timeStampRequestStr = Base64.encodeToString(timeStampRequest.getEncoded(), Base64.DEFAULT);
        //byte[] messageImprintDigestBytes = timeStampRequest.getMessageImprintDigest(); 
        //String messageImprintDigestStr = Base64.encodeToString(messageImprintDigestBytes, Base64.DEFAULT);
        //Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - messageImprintDigestStr: " + messageImprintDigestStr);
        //Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - timeStampRequestStr: " + timeStampRequestStr);
		votingListener.setRunningTask(new SendByteArrayTask(
				timeStampListener, timeStampRequest.getEncoded()).execute(
				ServerPaths.getURLTimeStampService(Aplicacion.CONTROL_ACCESO_URL)));
    }
    
    private static File genFile(MimeMessage body) throws Exception {
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File (sdCard.getAbsolutePath() + File.separator + 
				SISTEMA_VOTACION_DIR);
		dir.mkdirs();
		File resultado = new File(dir, "smime");
        body.writeTo(new FileOutputStream(resultado));
        return resultado;
    }
    
    public static void procesarVoto(Context context, 
    		Evento event, VotingListener votingListener, 
    		byte[] keyStoreBytes, char[] password) throws Exception {
    	Log.d(TAG + ".procesarVoto(...)", " - procesarVoto");
    	instancia = new VotacionHelper(event, votingListener, context);
    	instancia.obtenerSolicitudAcceso(keyStoreBytes, password);
    }
    
	public void guardar (String nif, String controlAcceso, String idEvento) throws FileNotFoundException {
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File (sdCard.getAbsolutePath() + File.separator + 
				SISTEMA_VOTACION_DIR + File.separator + controlAcceso + 
				File.separator + nif + File.separator + idEvento);
		dir.mkdirs();
		File file = new File(dir, "filename");
		FileOutputStream f = new FileOutputStream(file);	
	}

	@Override
	public void proccessAccessRequest(File accessRequest) {
        GetVotingCertTask obtenerCertificadoVotoTask = new GetVotingCertTask(
        		certVoteListener, accessRequest, pkcs10WrapperClient.getPEMEncodedRequestCSR());
        obtenerCertificadoVotoTask.execute(ServerPaths.getURLSolicitudAcceso(CONTROL_ACCESO_URL));
        votingListener.setRunningTask(obtenerCertificadoVotoTask);
	}

	@Override
	public void setException(String exceptionMsg) {
		votingListener.setException(exceptionMsg);
	}

	@Override
	public void proccessReceipt(ReciboVoto receipt) {
		// TODO Auto-generated method stub
	}

	@Override public void setRunningTask(AsyncTask runningTask) { }

}