package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.tsp.TimeStampRequest
import org.bouncycastle.tsp.TimeStampToken
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.StatusVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.PKCS10WrapperClient
import org.votingsystem.simulation.ContextService
import org.votingsystem.simulation.util.HttpHelper
import org.votingsystem.util.ApplicationContextHolder

import javax.mail.Header
import java.security.cert.X509Certificate
import java.util.concurrent.Callable

import static org.votingsystem.simulation.ContextService.*
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
    private ContextService contextService;

    public AccessRequestDataSender(SMIMEMessageWrapper smimeMessage, VoteVS voteVS) throws Exception {
        this.smimeMessage = smimeMessage;
        this.voteVS = voteVS;
        contextService = ApplicationContextHolder.getSimulationContext();
        this.destinationCert = contextService.getAccessControl().getX509Certificate();
        this.pkcs10WrapperClient = new PKCS10WrapperClient(KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, ContextVS.PROVIDER,
                contextService.getAccessControl().getServerURL(), voteVS.getEventVS().getId().toString(),
                voteVS.getHashCertVoteHex());
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - accessServiceURL: " +  contextService.getAccessControl().getAccessServiceURL());
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.getInstance().sendByteArray(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY, contextService.getAccessControl().getTimeStampServerURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = contextService.getAccessControl().getTimeStampCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);
            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] encryptedCSRBytes = Encryptor.encryptMessage(pkcs10WrapperClient.getCsrPEM(),destinationCert,header);
            byte[] accessRequestEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED;
            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" + ContentTypeVS.SIGNED_AND_ENCRYPTED;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(accessRequestFileName, accessRequestEncryptedBytes);
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    contextService.getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, pkcs10WrapperClient.getPublicKey(),
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
            } else {
                responseVS.setStatus(new StatusVS() {})
                pkcs10WrapperClient = null;
            }
        }
        return responseVS;
    }

    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }

}