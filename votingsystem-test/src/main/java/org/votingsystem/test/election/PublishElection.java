package org.votingsystem.test.election;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.test.Constants;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.security.KeyStore;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishElection extends BaseTest {

    private static final Logger log = Logger.getLogger(PublishElection.class.getName());

    private static String QR_CODE = "eid=https://voting.ddns.net/voting-service;uid=4b33a837-946a-4812-a1c3-dcfd59be3b69;";
    private static String SIGNER_KEYSTORE = "certs/fake_08888888D.jks";
    private static String KEYSTORE_PASSWORD = Constants.PASSW_DEMO;


    public static void main(String[] args) throws Exception {
        new PublishElection().publish();
        System.exit(0);
    }

    public PublishElection() {
        super();
    }

    public void publish() throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        log.info(qrMessageDto.getSystemEntityID());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), "application/xml",
                OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID()));

        log.info("electionXML: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        OperationDto operation = new XML().getMapper().readValue(responseDto.getMessageBytes(), OperationDto.class);

        log.info("dataBase64: " + new String(operation.getBase64DataDecoded()));

        //ElectionDto electionDto = qrResponseDto.getDataXml(ElectionDto.class);
        AbstractSignatureTokenConnection signingToken = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(SIGNER_KEYSTORE).openStream(),
                new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()));
        byte[] signatureBytes = new XAdESSignature().sign(responseDto.getMessageBytes(), signingToken,
                new TSPHttpSource(org.votingsystem.test.Constants.TIMESTAMP_SERVICE_URL));

        log.info("signedXML: " + new String(signatureBytes));
        responseDto = HttpConn.getInstance().doPostRequest(signatureBytes, MediaType.XML,
                OperationType.PUBLISH_ELECTION.getUrl(qrMessageDto.getSystemEntityID()));

        log.info("Publish result: " + responseDto.getStatusCode() + " - msg: " + responseDto.getMessage());
    }
}
