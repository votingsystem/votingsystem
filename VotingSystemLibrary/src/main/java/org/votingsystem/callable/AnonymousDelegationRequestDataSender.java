package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;

import javax.mail.Header;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AnonymousDelegationRequestDataSender implements Callable<ResponseVS> {

    private static Logger logger = Logger.getLogger(AnonymousDelegationRequestDataSender.class);

    private SMIMEMessageWrapper smimeMessage;
    private CertificationRequestVS certificationRequest;
    private X509Certificate destinationCert;

    public AnonymousDelegationRequestDataSender(SMIMEMessageWrapper smimeMessage, String weeksOperationActive,
                    String hashCertVS) throws Exception {
        this.smimeMessage = smimeMessage;
        this.destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        this.certificationRequest = CertificationRequestVS.getAnonymousDelegationRequest(KEY_SIZE, SIG_NAME,
                VOTE_SIGN_MECHANISM, ContextVS.PROVIDER, ContextVS.getInstance().getAccessControl().getServerURL(),
                hashCertVS, weeksOperationActive);
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - accessServiceURL: " +
                ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
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
            Header header = new Header("votingSystemMessageType", "anonymousDelegationCsr");
            byte[] encryptedCSRBytes = Encryptor.encryptMessage(certificationRequest.getCsrPEM(),destinationCert,header);
            byte[] delegationEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED.getName();
            String representativeDataFileName = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(representativeDataFileName, delegationEncryptedBytes);
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, certificationRequest.getPublicKey(),
                        certificationRequest.getPrivateKey());
                certificationRequest.initSigner(decryptedData);
                responseVS.setData(certificationRequest);
            } else {
                responseVS.setStatus(new StatusVS() {});
                responseVS.setData(null);
            }
        }
        return responseVS;
    }

}