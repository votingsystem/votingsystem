package org.votingsystem.test.android;

import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.test.Constants;
import org.votingsystem.test.android.xml.XmlReader;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.testlib.xml.SignatureAlgorithm;
import org.votingsystem.testlib.xml.SignatureValidator;
import org.votingsystem.testlib.xml.XAdESUtils;
import org.votingsystem.testlib.xml.XMLSignatureBuilder;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishElection extends BaseTest {

    private static final Logger log = Logger.getLogger(PublishElection.class.getName());

    private static final String TAG = PublishElection.class.getSimpleName();

    private static final String QR_CODE = "eid=https://voting.ddns.net/voting-service;uid=ad7bfe33-37c2-49cb-9994-ed16e6cad1c3;";

    public PublishElection() {
        super();
    }


    public static void main(String[] args) throws Exception {
        new PublishElection().publish();
        System.exit(0);
    }

    public void publish() throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        String serviceURL = OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), null, serviceURL);

        log.info("responseDto: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }

        OperationDto operation = new XML().getMapper().readValue(responseDto.getMessageBytes(), OperationDto.class);
        ElectionDto electionDto = XmlReader.readElection(operation.getBase64DataDecoded());
        log.info("Operation decoded base64 data: " + new String(operation.getBase64DataDecoded()));
        String textToSign = XMLUtils.prepareRequestToSign(responseDto.getMessageBytes());

        byte[] signatureBytes = new XMLSignatureBuilder(textToSign.getBytes(), XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(), new MockDNIe("08888888D"),
                Constants.TIMESTAMP_SERVICE_URL).build();

        log.info("signatureBytes: " + new String(signatureBytes));
        new SignatureValidator(signatureBytes).validate();
        log.info("Signature OK");

        responseDto = HttpConn.getInstance().doPostRequest(signatureBytes, null,
                OperationType.PUBLISH_ELECTION.getUrl(qrMessageDto.getSystemEntityID()));
        log.info("StatusCode: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }


}