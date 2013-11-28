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

package org.votingsystem.android.callable;

import android.content.Context;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.android.R;
import org.votingsystem.android.model.AndroidContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.android.model.VoteReceipt;
import org.votingsystem.android.util.HttpHelper;
import org.votingsystem.android.util.ServerPaths;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.votingsystem.android.model.AndroidContextVS.USER_CERT_ALIAS;

public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = "VoteSender";

    private EventVS event;
    private char[] password;
    private Context context = null;
    private String serviceURL = null;
    private AndroidContextVS androidContextVS = null;
    private byte[] keyStoreBytes = null;

    public VoteSender(EventVS event, byte[] keyStoreBytes, char[] password, Context context) {
        this.event = event;
        this.keyStoreBytes = keyStoreBytes;
        this.password = password;
        this.context = context;
        androidContextVS = AndroidContextVS.getInstance(context);
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".processVote(...)", " - call - event subject: " + event.getSubject());
        ResponseVS responseVS = null;
        try {
            event.initVoteData();
            String serviceURL = ServerPaths.getURLVoto(event.getControlCenter().getServerURL());
            String subject = context.getString(R.string.request_msg_subject, event.getEventVSId());
            String userVS = null;
            if (androidContextVS.getUserVS() != null) userVS = androidContextVS.getUserVS().getNif();

            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
            Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
            X509Certificate userCert = (X509Certificate) chain[0];
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    privateKey, chain, AndroidContextVS.SIGNATURE_ALGORITHM);
            String signedContent = event.getAccessRequestJSON().toString();
            SMIMEMessageWrapper solicitudAcceso = signedMailGenerator.genMimeMessage(
                    userVS, androidContextVS.getAccessControlVS().getNameNormalized(),
                    signedContent, subject, null);
            AccessRequestor accessRequestor = new AccessRequestor(solicitudAcceso,
                    event, androidContextVS.getAccessControlVS().getCertificate(),
                    ServerPaths.getURLSolicitudAcceso(androidContextVS.getAccessControlURL()),context);
            responseVS = accessRequestor.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            String votoJSON = event.getVoteJSON().toString();
            PKCS10WrapperClient pkcs10WrapperClient = accessRequestor.getPKCS10WrapperClient();
            SMIMEMessageWrapper signedVote = pkcs10WrapperClient.genSignedMessage(
                    event.getHashCertVoteBase64(), event.getControlCenter().getNameNormalized(),
                    votoJSON, context.getString(R.string.vote_msg_subject), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedVote, context);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                cancelAccessRequest(signedMailGenerator, userVS);
                return responseVS;
            }
            signedVote = timeStamper.getSmimeMessage();
            String documentContentType = ContentTypeVS.SIGNED_AND_ENCRYPTED;
            byte[] messageToSend = Encryptor.encryptSMIME(signedVote,
                    event.getControlCenter().getCertificate());
            HttpResponse response  = HttpHelper.sendData(messageToSend,
                    documentContentType, serviceURL);
            if(ResponseVS.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                SMIMEMessageWrapper votoValidado = Encryptor.decryptSMIMEMessage(
                        responseBytes, pkcs10WrapperClient.getKeyPair().getPublic(),
                        pkcs10WrapperClient.getKeyPair().getPrivate());
                VoteReceipt receipt = new VoteReceipt(ResponseVS.SC_OK, votoValidado, event);
                byte[] base64EncodedKey = Base64.encode(
                        pkcs10WrapperClient.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(base64EncodedKey, userCert);
                receipt.setPkcs10WrapperClient(pkcs10WrapperClient);
                receipt.setEncryptedKey(encryptedKey);
                responseVS.setData(receipt);
            } else {
                cancelAccessRequest(signedMailGenerator, userVS);
                responseVS = new ResponseVS(response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
            }
            /* problema -> javax.activation.UnsupportedDataTypeException:
             * no object DCH for MIME type application/pkcs7-signature
            MimeMessage solicitudAccesoMimeMessage = dnies.gen(userVS,
                    Aplicacion.getAccessControlVS().getNameNormalized(),
                    signedContent, subject, null, SignedMailGenerator.Type.USER);
            Object content = solicitudAccesoMimeMessage.getContent();
            MimeMultipart mimeMultipart = null;
            if (content.getClass().isAssignableFrom(MimeMultipart.class)) {
                mimeMultipart = (MimeMultipart) content;
            }
            SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);*/
            /*Tambien se puede get el digest
            SMIMESignedGenerator gen = dnies.getSMIMESignedGenerator();
            byte[] contentDigestBytes = (byte[])gen.getGeneratedDigests().get(SMIMESignedGenerator.DIGEST_SHA1);
            String contentDigest = Base64.encodeToString(contentDigestBytes, Base64.DEFAULT);
            Log.d(TAG + ".getSolicitudAcceso(...)", " - contentDigest: " + contentDigest);*/
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, context.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
        }
        return responseVS;
    }


    private ResponseVS cancelAccessRequest(SignedMailGenerator signedMailGenerator, String userVS) {
        Log.d(TAG + ".cancelAccessRequest(...)", " - cancelAccessRequest ");
        try {
            String subject = context.getString(R.string.cancel_vote_msg_subject);
            String serviceURL = ServerPaths.getURLAnulacionVoto(androidContextVS.getAccessControlURL());
            boolean isEncryptedResponse = true;
            SMIMEMessageWrapper cancelAccessRequest = signedMailGenerator.genMimeMessage(
                    userVS, androidContextVS.getAccessControlVS().getNameNormalized(),
                    event.getCancelVoteData(), subject, null);
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL, event.getCancelVoteData(),
                    subject, isEncryptedResponse, keyStoreBytes, password,
                    androidContextVS.getAccessControlVS().getCertificate(), context);
            return smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

}