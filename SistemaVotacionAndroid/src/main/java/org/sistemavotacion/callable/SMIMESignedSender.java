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

package org.sistemavotacion.callable;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.VotingSystemKeyStoreException;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.util.HttpHelper;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.sistemavotacion.android.AppData.ALIAS_CERT_USUARIO;
import static org.sistemavotacion.android.AppData.SIGNATURE_ALGORITHM;

public class SMIMESignedSender implements Callable<Respuesta> {

	public static final String TAG = "SMIMESignedSender";

    private SMIMEMessageWrapper smimeMessage = null;
    private X509Certificate destinationCert = null;
    private char[] password;
    private Context context = null;
    private String serviceURL = null;
    private String subject = null;
    private String signatureContent = null;
    private AppData appData = null;
    private byte[] keyStoreBytes = null;
    private boolean isEncryptedResponse = false;

    public SMIMESignedSender(String serviceURL, String signatureContent, String subject,
             boolean isEncryptedResponse, byte[] keyStoreBytes, char[] password,
             X509Certificate destinationCert, Context context) {
		this.subject = subject;
        this.signatureContent = signatureContent;
		this.keyStoreBytes = keyStoreBytes;
		this.password = password;
        this.context = context;
        this.serviceURL = serviceURL;
        this.isEncryptedResponse = isEncryptedResponse;
        this.destinationCert = destinationCert;
        appData = AppData.getInstance(context);
    }

    @Override public Respuesta call() {
        Log.d(TAG + ".call", " - call - url: " + serviceURL);
        String usuario = null;
        if (appData.getUsuario() != null) usuario = appData.getUsuario().getNif();
        MessageTimeStamper timeStamper = null;
        Respuesta respuesta = null;
        KeyPair keypair = null;
        try {
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    keyStoreBytes, ALIAS_CERT_USUARIO, password, SIGNATURE_ALGORITHM);
            smimeMessage = signedMailGenerator.genMimeMessage(usuario,
                    appData.getControlAcceso().getNombreNormalizado(),
                    signatureContent, subject, null);
            if(isEncryptedResponse) {
                KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
                PrivateKey privateKey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
                Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
                PublicKey publicKey = ((X509Certificate)chain[0]).getPublicKey();
                keypair = new KeyPair(publicKey, privateKey);
            }
            timeStamper = new MessageTimeStamper(smimeMessage, context);
            respuesta = timeStamper.call();
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
            smimeMessage = timeStamper.getSmimeMessage();
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
            } else respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity()));
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            return new Respuesta(Respuesta.SC_ERROR, context.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            return new Respuesta(Respuesta.SC_ERROR, ex.getLocalizedMessage());
        }
        return respuesta;
    }

	public SMIMEMessageWrapper getSmimeMessage() {
		return smimeMessage;
	}

}