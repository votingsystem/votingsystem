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

import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.HttpHelper;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

public class SMIMESignedSender implements Callable<ResponseVS> {

    public static final String TAG = "SMIMESignedSender";

    private SMIMEMessageWrapper smimeMessage = null;
    private X509Certificate destinationCert = null;
    private char[] password;
    private Context context = null;
    private String serviceURL = null;
    private String receiverName = null;
    private String subject = null;
    private String signatureContent = null;
    private byte[] keyStoreBytes = null;
    private ContentTypeVS contentType;

    public SMIMESignedSender(String receiverName, String serviceURL, String signatureContent,
            ContentTypeVS contentType, String subject, byte[] keyStoreBytes, char[] password,
            X509Certificate destinationCert, Context context) {
        this.subject = subject;
        this.receiverName = receiverName;
        this.signatureContent = signatureContent;
        this.contentType = contentType;
        this.keyStoreBytes = keyStoreBytes;
        this.password = password;
        this.context = context;
        this.serviceURL = serviceURL;
        this.destinationCert = destinationCert;
    }

    @Override public ResponseVS call() {
        Log.d(TAG + ".call", " - call - url: " + serviceURL);
        String userVS = null;
        if (ContextVS.getInstance(context).getUserVS() != null) userVS =
                ContextVS.getInstance(context).getUserVS().getNif();
        MessageTimeStamper timeStamper = null;
        ResponseVS responseVS = null;
        KeyPair keypair = null;
        try {
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    keyStoreBytes, USER_CERT_ALIAS, password, SIGNATURE_ALGORITHM);
            smimeMessage = signedMailGenerator.genMimeMessage(userVS, receiverName,
                    signatureContent, subject);
            timeStamper = new MessageTimeStamper(smimeMessage, context);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            smimeMessage = timeStamper.getSmimeMessage();
            byte[] messageToSend = null;
            if(contentType.isEncrypted())
                messageToSend = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            else messageToSend = smimeMessage.getBytes();
            responseVS  = HttpHelper.sendData(messageToSend, contentType, serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                if(responseVS.getContentType() != null &&
                        responseVS.getContentType().isEncrypted()) {
                    KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
                    PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
                    Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
                    PublicKey publicKey = ((X509Certificate)chain[0]).getPublicKey();
                    keypair = new KeyPair(publicKey, privateKey);
                    SMIMEMessageWrapper signedMessage = Encryptor.decryptSMIMEMessage(
                            responseVS.getMessageBytes(), keypair.getPublic(), keypair.getPrivate());
                    responseVS.setSmimeMessage(signedMessage);
                }
            }
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, context.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
        } finally {return responseVS;}
    }

    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

}