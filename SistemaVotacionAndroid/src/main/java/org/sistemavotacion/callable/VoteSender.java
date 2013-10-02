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
import org.bouncycastle2.util.encoders.Base64;
import org.sistemavotacion.android.AppData;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.seguridad.VotingSystemKeyStoreException;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.util.HttpHelper;
import org.sistemavotacion.util.ServerPaths;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.sistemavotacion.android.AppData.ALIAS_CERT_USUARIO;

public class VoteSender implements Callable<Respuesta> {

    public static final String TAG = "VoteSender";

    private Evento event;
    private char[] password;
    private Context context = null;
    private String serviceURL = null;
    private AppData appData = null;
    private byte[] keyStoreBytes = null;

    public VoteSender(Evento event, byte[] keyStoreBytes, char[] password, Context context) {
        this.event = event;
        this.keyStoreBytes = keyStoreBytes;
        this.password = password;
        this.context = context;
        appData = AppData.getInstance(context);
    }

    @Override public Respuesta call() {
        Log.d(TAG + ".processVote(...)", " - call - event subject: " + event.getAsunto());
        Respuesta respuesta = null;
        try {
            event.initVoteData();
            String serviceURL = ServerPaths.getURLVoto(event.getCentroControl().getServerURL());
            String asunto = context.getString(R.string.request_msg_subject, event.getEventoId());
            String usuario = null;
            if (appData.getUsuario() != null) usuario = appData.getUsuario().getNif();

            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
            Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
            X509Certificate userCert = (X509Certificate) chain[0];
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    privateKey, chain, AppData.SIGNATURE_ALGORITHM);
            String contenidoFirma = event.getAccessRequestJSON().toString();
            SMIMEMessageWrapper solicitudAcceso = signedMailGenerator.genMimeMessage(
                    usuario, appData.getControlAcceso().getNombreNormalizado(),
                    contenidoFirma, asunto, null);
            AccessRequestor accessRequestor = new AccessRequestor(solicitudAcceso,
                    event, appData.getControlAcceso().getCertificado(),
                    ServerPaths.getURLSolicitudAcceso(appData.getAccessControlURL()),context);
            respuesta = accessRequestor.call();
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
            String votoJSON = event.getVoteJSON().toString();
            PKCS10WrapperClient pkcs10WrapperClient = accessRequestor.getPKCS10WrapperClient();
            SMIMEMessageWrapper signedVote = pkcs10WrapperClient.genSignedMessage(
                    event.getHashCertificadoVotoBase64(), event.getCentroControl().getNombreNormalizado(),
                    votoJSON, context.getString(R.string.vote_msg_subject), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedVote, context);
            respuesta = timeStamper.call();
            if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                cancelAccessRequest(signedMailGenerator, usuario);
                return respuesta;
            }
            signedVote = timeStamper.getSmimeMessage();
            String documentContentType = AppData.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            byte[] messageToSend = Encryptor.encryptSMIME(signedVote,
                    event.getCentroControl().getCertificado());
            HttpResponse response  = HttpHelper.sendByteArray(messageToSend,
                    documentContentType, serviceURL);
            if(Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                SMIMEMessageWrapper votoValidado = Encryptor.decryptSMIMEMessage(
                        responseBytes, pkcs10WrapperClient.getKeyPair().getPublic(),
                        pkcs10WrapperClient.getKeyPair().getPrivate());
                VoteReceipt receipt = new VoteReceipt(Respuesta.SC_OK, votoValidado, event);
                byte[] base64EncodedKey = Base64.encode(
                        pkcs10WrapperClient.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(base64EncodedKey, userCert);
                receipt.setPkcs10WrapperClient(pkcs10WrapperClient);
                receipt.setEncryptedKey(encryptedKey);
                respuesta.setData(receipt);
            } else {
                cancelAccessRequest(signedMailGenerator, usuario);
                respuesta = new Respuesta(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
            }
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
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            return new Respuesta(Respuesta.SC_ERROR, context.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            return new Respuesta(Respuesta.SC_ERROR, ex.getLocalizedMessage());
        }
        return respuesta;
    }


    private Respuesta cancelAccessRequest(SignedMailGenerator signedMailGenerator, String usuario) {
        Log.d(TAG + ".cancelAccessRequest(...)", " - cancelAccessRequest ");
        try {
            String subject = context.getString(R.string.cancel_vote_msg_subject);
            String serviceURL = ServerPaths.getURLAnulacionVoto(appData.getAccessControlURL());
            boolean isEncryptedResponse = true;
            SMIMEMessageWrapper cancelAccessRequest = signedMailGenerator.genMimeMessage(
                    usuario, appData.getControlAcceso().getNombreNormalizado(),
                    event.getCancelVoteData(), subject, null);
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL, event.getCancelVoteData(),
                    subject, isEncryptedResponse, keyStoreBytes, password,
                    appData.getControlAcceso().getCertificado(), context);
            return smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        }
    }

}