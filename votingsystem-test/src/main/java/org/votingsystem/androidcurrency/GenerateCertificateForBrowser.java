package org.votingsystem.androidcurrency;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.androidcurrency.xml.XmlReader;
import org.votingsystem.androidcurrency.xml.XmlWriter;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.xml.SignatureAlgorithm;
import org.votingsystem.crypto.xml.SignatureBuilder;
import org.votingsystem.crypto.xml.XAdESUtils;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.BrowserCertificationDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
import org.votingsystem.util.NifUtils;
import org.votingsystem.util.XMLUtils;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class GenerateCertificateForBrowser extends BaseTest {

    private static final Logger log = Logger.getLogger(GenerateCertificateForBrowser.class.getName());

    private static String CSR = "-----BEGIN CERTIFICATE REQUEST-----\n" +
            "MIIBbjCB2AIBADAvMS0wKwYDVQQFEyQ0YjhkN2Q0Mi00ZjJiLTQwNjgtOTM0ZS0y\n" +
            "ZTk2YjI2OGRlNWMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAJVPe6JZIZHY\n" +
            "Piji1L/mgwSoOzIvv3B/sCVxV4jk70NymUKZIkjnk9ZUaL4SWqgc/PZ48GhoeNKG\n" +
            "I01RGdg0JjbuwD8+nSrwSL+wj7wiI5olXegalUZyYHQXDH2QuVaxE21tJIHmhuQu\n" +
            "rqlkMTnhuL1XKUnU/BUuyQah5Jccf3V9AgMBAAGgADANBgkqhkiG9w0BAQUFAAOB\n" +
            "gQCG9LFtrTVyHmKFvzXhG6Vg4feEa3tBLucvX2eTbDKXv2JyLnNCSy0lBVO4GxBh\n" +
            "UorbR7f9DT0lzr8/4MRGFuGoapNxzKyRDc2KV6NvxzuPgBgvFLOrOudrFqUSXRJy\n" +
            "ejWhgs75D1enO3jr6dxY22dn8VDftMaPnfJu7SP8FSOtZA==\n" +
            "-----END CERTIFICATE REQUEST-----";

    private static final String QR_CODE = "eid=https://votingsystem.ddns.net/currency-server;op=0;uid=36f68a30-1e90-42e8-b705-d1401bdc8175;";


    public static void main(String[] args) throws Exception {
        //new GenerateCertificateForBrowser().generateFromHttp();
        new GenerateCertificateForBrowser().generate(CSR, null);
        System.exit(0);
    }

    public GenerateCertificateForBrowser() {}

    private void generateFromHttp() throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        String serviceURL = qrMessageDto.getSystemEntityID() + "/api/currency-qr/browser-certificate";
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), MediaType.JSON,
                serviceURL);
        log.info("QRResponseDto: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        BrowserCertificationDto csrRequest = JSON.getMapper().readValue(responseDto.getMessageBytes(),
                BrowserCertificationDto.class);
        generate(csrRequest.getBrowserCsr(), csrRequest.getUserUUID());
    }

    private void generate(String csrRequest, String userUUID) throws Exception {
        BrowserCertificationDto csrRequestDto = new BrowserCertificationDto();
        csrRequestDto.setBrowserCsr(csrRequest).setUserUUID(userUUID);

        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrRequestDto.getBrowserCsr().getBytes());


        String mobileHTTPSessionUUID = UUID.randomUUID().toString();

        CertExtensionDto certExtension = new CertExtensionDto(NifUtils.getNif(12345), "TestGivenName", "TestSurname")
                .setUUID(mobileHTTPSessionUUID);
        CertificationRequest certificationRequest = CertificationRequest.getUserRequest(certExtension);
        csrRequestDto.setMobileCsr(new String(certificationRequest.getCsrPEM()));

        log.info("csr: " + csr);

        byte[] csrRequestBytes = XmlWriter.write(csrRequestDto);
        //log.info("csrRequestBytes: " + new String(csrRequestBytes));
        String textToSign = XMLUtils.prepareRequestToSign(csrRequestBytes);

        byte[] signatureBytes = new SignatureBuilder(textToSign.getBytes(), XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(), new MockDNIe("08888888D"),
                Constants.TIMESTAMP_SERVICE_URL).build();
        log.info("signatureBytes: " + new String(signatureBytes));

        ResponseDto response = HttpConn.getInstance().doPostRequest(signatureBytes, MediaType.XML,
                CurrencyOperation.BROWSER_CERTIFICATION.getUrl(Constants.ID_PROVIDER_ENTITY_ID));
        log.info("statusCode: " + response.getStatusCode() + " - msg: " + response.getMessage());

        if(ResponseDto.SC_OK != response.getStatusCode()) {
            log.info("statusCode: " + response.getStatusCode() + " - message: " + response.getMessage());
            System.exit(0);
        }

        BrowserCertificationDto browserCertification = XmlReader.getUserCertificationRequest(response.getMessageBytes());

        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(response.getMessageBytes(), MediaType.XML,
                CurrencyOperation.INIT_BROWSER_SESSION.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));


        log.info("StatusCode: " + responseDto.getStatusCode() + " - Response: " + responseDto.getMessage());
    }

}
