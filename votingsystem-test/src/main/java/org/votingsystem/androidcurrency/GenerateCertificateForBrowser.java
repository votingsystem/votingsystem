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
import org.votingsystem.dto.indentity.SessionCertificationDto;
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
            "MIIBbjCB2AIBADAvMS0wKwYDVQQFEyQ0Y2U5Yzg0My02MTdlLTQ3NWEtYTM0Ny0x\n" +
            "NTNhYTJiNzhkZTUwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMH/oN1U/YZH\n" +
            "h+3aUdZPNIPxb/p0IHJ97AwZKyNYw/ziyrL/V7u+tOcZ+mOIvCD5+ZO1hYODoS8n\n" +
            "o8U++W54KlygnD4x0kURxSDO1Z0tEdWcIqkLhKKuP+RasBxsZo/b5fxfwItVmHiP\n" +
            "4D+I8OFxIvqDEzyY5uJRLjgNSWEUTP33AgMBAAGgADANBgkqhkiG9w0BAQUFAAOB\n" +
            "gQASo3EWI1iAOq9SZSKTjJVIxnxwTPqVCGgS09H2l/1tCUH27vGacqnLXDhVdXaj\n" +
            "njWjjLHXkDivTfyX+RIiIzWbp0Am8KDvhKNP1fnOjbjxkiiWlK16FKzWyY3NRVGk\n" +
            "61zKGg3tSVzhNsSv/rdfQt75dADiKO0vpR8u1tRTgN3WtQ==\n" +
            "-----END CERTIFICATE REQUEST-----";

    private static final String QR_CODE = "eid=https://votingsystem.ddns.net/currency-server;op=0;uid=aa1cbbb8-a41e-402b-85a2-5992cc9b93bd;";


    public static void main(String[] args) throws Exception {
        new GenerateCertificateForBrowser().generateFromHttp();
        //new GenerateCertificateForBrowser().generate(CSR, null);
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
        SessionCertificationDto csrRequest = JSON.getMapper().readValue(responseDto.getMessageBytes(),
                SessionCertificationDto.class);
        generate(csrRequest.getBrowserCsr(), csrRequest.getUserUUID());
    }

    private void generate(String csrRequest, String userUUID) throws Exception {
        SessionCertificationDto csrRequestDto = new SessionCertificationDto();
        csrRequestDto.setBrowserCsr(csrRequest).setUserUUID(userUUID);

        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(
                csrRequestDto.getBrowserCsr().getBytes());
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
                CurrencyOperation.SESSION_CERTIFICATION.getUrl(Constants.ID_PROVIDER_ENTITY_ID));
        log.info("statusCode: " + response.getStatusCode() + " - msg: " + response.getMessage());

        if(ResponseDto.SC_OK != response.getStatusCode()) {
            log.info("statusCode: " + response.getStatusCode() + " - message: " + response.getMessage());
            System.exit(0);
        }

        SessionCertificationDto browserCertification = XmlReader.getUserCertificationRequest(response.getMessageBytes());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(response.getMessageBytes(), MediaType.XML,
                CurrencyOperation.SESSION_CERTIFICATION_DATA.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("StatusCode: " + responseDto.getStatusCode() + " - Response: " + responseDto.getMessage());

    }

}