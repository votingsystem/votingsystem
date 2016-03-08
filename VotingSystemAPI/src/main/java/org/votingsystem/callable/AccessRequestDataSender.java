package org.votingsystem.callable;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.crypto.CMSUtils;
import org.votingsystem.util.crypto.CertificationRequestVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static org.votingsystem.util.ContextVS.VOTE_SIGN_MECHANISM;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class AccessRequestDataSender implements Callable<ResponseVS> {

    private static Logger log = Logger.getLogger(AccessRequestDataSender.class.getName());

    private CMSSignedMessage cmsMessage;
    private CertificationRequestVS certificationRequest;
    private X509Certificate receiverCert;

    public AccessRequestDataSender(CMSSignedMessage cmsMessage, AccessRequestDto accessRequest,
                                   String hashCertVSBase64) throws Exception {
        this.cmsMessage = cmsMessage;
        this.receiverCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        this.certificationRequest = CertificationRequestVS.getVoteRequest(VOTE_SIGN_MECHANISM,
                ContextVS.PROVIDER, ContextVS.getInstance().getAccessControl().getServerURL(),
                accessRequest.getEventId(), hashCertVSBase64);
    }

    @Override public ResponseVS call() throws Exception {
        log.info("doInBackground - accessServiceURL: " +  ContextVS.getInstance().getAccessControl().getAccessServiceURL());
        TimeStampRequest timeStampRequest = cmsMessage.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.getInstance().getAccessControl().getTimeStampCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
            timeStampToken.validate(timeStampSignerInfoVerifier);
            CMSSignedData timeStampedSignedData = CMSUtils.addTimeStamp(cmsMessage, timeStampToken);
            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.CSR_FILE_NAME, certificationRequest.getCsrPEM());
            mapToSend.put(ContextVS.ACCESS_REQUEST_FILE_NAME, PEMUtils.getPEMEncoded(timeStampedSignedData.toASN1Structure()));
            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAccessServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                certificationRequest.initSigner(responseVS.getMessageBytes());
                responseVS.setData(certificationRequest);
            } else {
                responseVS.setData(null);
            }
        }
        return responseVS;
    }

}