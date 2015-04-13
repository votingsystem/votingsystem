package org.votingsystem.callable;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static org.votingsystem.util.ContextVS.*;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class AccessRequestDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(AccessRequestDataSender.class.getSimpleName());

    private VoteVS voteVS;
    private SMIMEMessage smimeMessage;
    private CertificationRequestVS certificationRequest;
    private X509Certificate destinationCert;

    public AccessRequestDataSender(SMIMEMessage smimeMessage, VoteVS voteVS) throws Exception {
        this.smimeMessage = smimeMessage;
        this.voteVS = voteVS;
        this.destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        this.certificationRequest = CertificationRequestVS.getVoteRequest(KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, ContextVS.getInstance().getAccessControl().getServerURL(),
                voteVS.getEventVS().getId().toString(), voteVS.getHashCertVSBase64());
    }

    @Override public ResponseVS call() throws Exception {
        log.info("doInBackground - accessServiceURL: " +  ContextVS.getInstance().getAccessControl().getAccessServiceURL());
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.getInstance().getAccessControl().getTimeStampCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);
            //byte[] encryptedCSRBytes = Encryptor.encryptMessage(certificationRequest.getCsrPEM(),destinationCert);
            //byte[] accessRequestEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, certificationRequest.getCsrPEM());
            mapToSend.put(ContextVS.ACCESS_REQUEST_FILE_NAME, smimeMessage.getBytes());
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                /*byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, certificationRequest.getPublicKey(),
                        certificationRequest.getPrivateKey());*/
                certificationRequest.initSigner(responseVS.getMessageBytes());
                responseVS.setData(certificationRequest);
            } else {
                responseVS.setData(null);
            }
        }
        return responseVS;
    }

}