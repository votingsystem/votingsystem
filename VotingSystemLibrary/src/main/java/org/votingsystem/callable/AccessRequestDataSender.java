package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PKCS10WrapperClient;
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
public class AccessRequestDataSender implements Callable<ResponseVS> {

    private static Logger logger = Logger.getLogger(AccessRequestDataSender.class);

    private VoteVS voteVS;
    private SMIMEMessageWrapper smimeMessage;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert;

    public AccessRequestDataSender(SMIMEMessageWrapper smimeMessage, VoteVS voteVS) throws Exception {
        this.smimeMessage = smimeMessage;
        this.voteVS = voteVS;
        this.destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        this.pkcs10WrapperClient = new PKCS10WrapperClient(KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, ContextVS.PROVIDER,
                ContextVS.getInstance().getAccessControl().getServerURL(), voteVS.getEventVS().getId().toString(),
                voteVS.getHashCertVoteHex());
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - accessServiceURL: " +  ContextVS.getInstance().getAccessControl().getAccessServiceURL());
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY.getName(), ContextVS.getInstance().getAccessControl().getTimeStampServerURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.getInstance().getAccessControl().getTimeStampCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);
            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] encryptedCSRBytes = Encryptor.encryptMessage(pkcs10WrapperClient.getCsrPEM(),destinationCert,header);
            byte[] accessRequestEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED.getName();
            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(accessRequestFileName, accessRequestEncryptedBytes);
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, pkcs10WrapperClient.getPublicKey(),
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
                responseVS.setData(pkcs10WrapperClient);
            } else {
                responseVS.setStatus(new StatusVS() {});
                responseVS.setData(null);
            }
        }
        return responseVS;
    }

}