package org.votingsystem.election;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.util.Base64;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishElection extends BaseTest {

    private static final Logger log = Logger.getLogger(PublishElection.class.getName());

    private static String QR_CODE = "eid=https://192.168.1.5:8443/voting-service;uid=1e5f6656-39cc-4370-945b-0fff92d95008;";
    private static String KEYSTORE = "certs/fake_7553172H.jks";
    private static String KEYSTORE_PASSWORD = org.votingsystem.util.Constants.PASSW_DEMO;


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
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), "application/json",
                OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID()));

        log.info("electionXML: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        QRResponseDto qrResponseDto = JSON.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);
        //QRResponseDto qrResponseDto = XML.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);

        log.info("dataBase64: " + new String(Base64.getDecoder().decode(qrResponseDto.getBase64Data())));

        //ElectionDto electionDto = qrResponseDto.getDataXml(ElectionDto.class);
        ElectionDto electionDto = qrResponseDto.getDataJson(ElectionDto.class);
        byte[] dataToSign = XML.getMapper().writeValueAsBytes(electionDto);
        AbstractSignatureTokenConnection signingToken = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(KEYSTORE).openStream(), KEYSTORE_PASSWORD);
        byte[] signatureBytes =  XAdESSignature.sign(dataToSign, signingToken,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));

        log.info("signedXML: " + new String(signatureBytes));
        responseDto = HttpConn.getInstance().doPostRequest(signatureBytes, MediaType.XML,
                OperationType.PUBLISH_ELECTION.getUrl(qrMessageDto.getSystemEntityID()));

        log.info("Publish result: " + responseDto.getStatusCode() + " - msg: " + responseDto.getMessage());
    }
}
