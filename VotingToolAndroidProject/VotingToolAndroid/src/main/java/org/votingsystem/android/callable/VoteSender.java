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
import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.util.HttpHelper;
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

import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

public class VoteSender implements Callable<ResponseVS> {

    public static final String TAG = "VoteSender";

    private EventVS event;
    private char[] password;
    private Context context = null;
    private String serviceURL = null;
    private ContextVS contextVS = null;
    private byte[] keyStoreBytes = null;

    public VoteSender(EventVS event, byte[] keyStoreBytes, char[] password, Context context) {
        this.event = event;
        this.keyStoreBytes = keyStoreBytes;
        this.password = password;
        this.context = context;
        contextVS = ContextVS.getInstance(context);
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".processVote(...)", " - call - event subject: " + event.getSubject());
        ResponseVS responseVS = null;
        try {
            event.initVoteData();
            String serviceURL = event.getControlCenter().getVoteServiceURL();
            String subject = context.getString(R.string.request_msg_subject, event.getEventVSId());
            String userVS = null;
            if (contextVS.getUserVS() != null) userVS = contextVS.getUserVS().getNif();

            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
            Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
            X509Certificate userCert = (X509Certificate) chain[0];
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    privateKey, chain, ContextVS.SIGNATURE_ALGORITHM);
            String signedContent = event.getAccessRequestJSON().toString();
            SMIMEMessageWrapper solicitudAcceso = signedMailGenerator.genMimeMessage(
                    userVS, contextVS.getAccessControlVS().getNameNormalized(),
                    signedContent, subject, null);
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(solicitudAcceso,
                    event, contextVS.getAccessControlVS().getCertificate(),
                    contextVS.getAccessControlVS().getAccessServiceURL(),context);
            responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            String votoJSON = event.getVoteJSON().toString();
            PKCS10WrapperClient pkcs10WrapperClient = accessRequestDataSender.getPKCS10WrapperClient();
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
            byte[] messageToSend = Encryptor.encryptSMIME(signedVote,
                    event.getControlCenter().getCertificate());
            responseVS  = HttpHelper.sendData(messageToSend,
                    ContentTypeVS.SIGNED_AND_ENCRYPTED.getName(), serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessageWrapper voteReceipt = Encryptor.decryptSMIMEMessage(
                        responseVS.getMessageBytes(), pkcs10WrapperClient.getKeyPair().getPublic(),
                        pkcs10WrapperClient.getKeyPair().getPrivate());
                VoteVS receipt = new VoteVS(ResponseVS.SC_OK, voteReceipt, event);
                byte[] base64EncodedKey = Base64.encode(
                        pkcs10WrapperClient.getPrivateKey().getEncoded());
                byte[] encryptedKey = Encryptor.encryptMessage(base64EncodedKey, userCert);
                receipt.setPkcs10WrapperClient(pkcs10WrapperClient);
                receipt.setEncryptedKey(encryptedKey);
                responseVS.setData(receipt);
            } else return responseVS;
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
            String serviceURL = contextVS.getAccessControlVS().getCancelVoteServiceURL();
            boolean isEncryptedResponse = true;
            SMIMEMessageWrapper cancelAccessRequest = signedMailGenerator.genMimeMessage(
                    userVS, contextVS.getAccessControlVS().getNameNormalized(),
                    event.getCancelVoteData(), subject, null);
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(serviceURL, event.getCancelVoteData(),
                    subject, isEncryptedResponse, keyStoreBytes, password,
                    contextVS.getAccessControlVS().getCertificate(), context);
            return smimeSignedSender.call();
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }

}