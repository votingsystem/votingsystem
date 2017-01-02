package org.votingsystem.android;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.android.xml.XmlReader;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.xml.SignatureAlgorithm;
import org.votingsystem.crypto.xml.SignatureBuilder;
import org.votingsystem.crypto.xml.SignatureValidator;
import org.votingsystem.crypto.xml.XAdESUtils;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.XMLUtils;
import org.votingsystem.xml.XML;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PublishElection extends BaseTest {

    private static final Logger log = Logger.getLogger(PublishElection.class.getName());

    private static final String TAG = PublishElection.class.getSimpleName();

    private static final String QR_CODE = "eid=https://192.168.1.5/voting-service;uid=26ce4bcd-4765-4630-bb21-8f8f74d43e51;";

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

        log.info("QRResponseDto: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }

        QRResponseDto qrResponseDto = XML.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);
        log.info("Election: " + new String(qrResponseDto.getData()));

        ElectionDto electionDto = XmlReader.readElection(qrResponseDto.getData());
        String textToSign = XMLUtils.prepareRequestToSign(qrResponseDto.getData());

        byte[] signatureBytes = new SignatureBuilder(textToSign.getBytes(), XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(), new MockDNIe("7553172H"),
                Constants.TIMESTAMP_SERVICE_URL).build();

        log.info("signatureBytes: " + new String(signatureBytes));
        new SignatureValidator(signatureBytes).validate();
        log.info("Signature OK");

        responseDto = HttpConn.getInstance().doPostRequest(signatureBytes, null,
                OperationType.PUBLISH_ELECTION.getUrl(qrMessageDto.getSystemEntityID()));
        log.info("StatusCode: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }

    private void validateSignature(byte[] signatureBytes, String entityId) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signatureBytes)));
        urlParameters.add(new BasicNameValuePair("withTimeStampValidation", Boolean.TRUE.toString()));
        ResponseDto responseDto = HttpConn.getInstance().doPostFormRequest(OperationType.VALIDATE_SIGNED_DOCUMENT
                        .getUrl(entityId), urlParameters);
        log.info("Publish result: " + responseDto.getStatusCode() + " - msg: " + responseDto.getMessage());
    }

}