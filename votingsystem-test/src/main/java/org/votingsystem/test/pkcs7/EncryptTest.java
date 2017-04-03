package org.votingsystem.test.pkcs7;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.test.Constants;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.socket.SocketOperation;
import  org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class EncryptTest extends BaseTest {

    private static Logger log =  Logger.getLogger(EncryptTest.class.getName());


    private static String FORGE_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDVjCCAj6gAwIBAgIIKfehUVGilC0wDQYJKoZIhvcNAQELBQAwLDEaMBgGA1UE\n" +
            "AwwRRkFLRSBST09UIEROSWUgQ0ExDjAMBgNVBAsMBUNlcnRzMB4XDTE3MDExOTE0\n" +
            "MTUyM1oXDTE3MDExOTIzMDAwMFowTTEcMBoGA1UECwwTYnJvd3Nlci1jZXJ0aWZp\n" +
            "Y2F0ZTEtMCsGA1UEBRMkYjgwMjQ4N2YtNzk5ZC00NmZjLThmY2EtNzZjN2EyODNh\n" +
            "N2M4MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCR5e3dfSSKp4V1FAp2Jtgs\n" +
            "viWrOB8Un8XTb/rrNo7Ky5mBfMR6d/aJRbHQAv3QstXDT+YDYKPc93f5pCG5JPI7\n" +
            "OYqzsKfxT54d2LeJRbEryGJYp4eT8f9ZpXn2PknRC27NCSFslFaJWpb0C1GUA8sr\n" +
            "EKXqQfFWsNk0C399wE5MZwIDAQABo4HeMIHbMFsGA1UdIwRUMFKAFD2Y24ezQwtg\n" +
            "wwMwPWJqsmvQvFwwoTCkLjAsMRowGAYDVQQDDBFGQUtFIFJPT1QgRE5JZSBDQTEO\n" +
            "MAwGA1UECwwFQ2VydHOCCG/MMPi8INTqMD8GCCsGAQUFBwEBBDMwMTAvBggrBgEF\n" +
            "BQcwAYYjaHR0cHM6Ly8xOTIuMTY4LjEuNS9pZHByb3ZpZGVyL29jc3AwHQYDVR0O\n" +
            "BBYEFIHlPiSx4jDOuPDWVVF7+B2SqX81MAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/\n" +
            "BAQDAgWgMA0GCSqGSIb3DQEBCwUAA4IBAQB/hO+f2ReRoQoH0zyfLCX1pPliCquS\n" +
            "wvCUUKwM4Y1HUKJoIJI6ZGBQRjw/4xHbc9JscGEzrxmLRtn/E7v8YIkzLJ50XtoQ\n" +
            "xi5r+nUvhK98h4j7nTekK98p2E9E2tqaHwOlarprII3jcyRvaY9NVy509fi9U2Q5\n" +
            "3FMJe/IWMjdE084zbaOpls4hEmcMGV42Tc3pqcOYJRjsI0jntWQavKPTZgRRkBo8\n" +
            "eLst+d74JGbcLFOaE8zsPA8xAqElpbXycjlvlp1uM2krPknAwjORurlOOeAEZS6Z\n" +
            "f3FZx3WkC6r+IZwg/lukS6uwqQwqGg9tsbSEmpMUZFGOG3dq9wwfzkNC\n" +
            "-----END CERTIFICATE-----";

    private static String FORGE_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIICXgIBAAKBgQCR5e3dfSSKp4V1FAp2JtgsviWrOB8Un8XTb/rrNo7Ky5mBfMR6\n" +
            "d/aJRbHQAv3QstXDT+YDYKPc93f5pCG5JPI7OYqzsKfxT54d2LeJRbEryGJYp4eT\n" +
            "8f9ZpXn2PknRC27NCSFslFaJWpb0C1GUA8srEKXqQfFWsNk0C399wE5MZwIDAQAB\n" +
            "AoGAGH8+3T2x8QYHxozC5OvIyFp1ALa/oTrigIoA25WhjDN7Zt6ILvgNkSb+oCqg\n" +
            "a3Zbphu4R0DmwqdaobQJZYjqkv2nQ57YeXanyHaAXDxsIYTYIW0EWzfngXYACOwk\n" +
            "eNX4F7ZPMKF+D4D40etu4LxI37tDgWCU7pC9AGlqvSQalAECQQDdsNXkDQhgah+2\n" +
            "KfWL3eP1Js9ZWAX43YQL9A2ZrNG27NQBahJ2Jvz2XsQVsQr7PX9MZZl+qdauF+CL\n" +
            "vN91xLyBAkEAqHpEZGEFbhqb0EV8uPqxDMvoeeGROV8Ivv2DHRb48ggfOIDwM/3V\n" +
            "L0MxoO013CCzZOKvZlrIXsx3XOIyvn405wJBANxDZdDnLgp1hrp8qA5m0aZzABNa\n" +
            "BN0GYrtpqdWlQtzII8Cf/mXMSQwUjiirNij4KjHixIZ4AugIqz7L0w51AYECQQCI\n" +
            "mxLnXS+89gBO4HjfuA1k9dUbNkW9ggwiaIYuSRkzjlhaRVn+nhuEhfQwqwYX5b/v\n" +
            "1kooMQX0r8881gAVK1oTAkEAxvDLP3r8IHPCzo+x59K7K6KojRZeQecrmARG0Evb\n" +
            "saweBzZ2Nty9+HvQWBDDY/Ziq2z3BlLXUhve0fMmjbqODA==\n" +
            "-----END RSA PRIVATE KEY-----";

    private static String ENCRYPTED_CONTENT = "-----BEGIN PKCS7-----\n" +
            "MIIBOwYJKoZIhvcNAQcDoIIBLDCCASgCAQAxgdIwgc8CAQAwODAsMRowGAYDVQQD\n" +
            "DBFGQUtFIFJPT1QgRE5JZSBDQTEOMAwGA1UECwwFQ2VydHMCCCn3oVFRopQtMA0G\n" +
            "CSqGSIb3DQEBAQUABIGADzpUuiAJazrVOdhrN2pBGHLApVpm94HOGcW1pfHtuXgf\n" +
            "GA65VnXBf0Ii3D88iI0cJkGbBhJr9/x+1aIqGfh/qF9HjBiPqjMYUVcipg7pPY2t\n" +
            "I9VGIINIahMupmgMyx/PIRlRwu/JOmmTDkNcAbzkR8FRfprD4H4mwMbuieJtJ+0w\n" +
            "TgYJKoZIhvcNAQcBMB0GCWCGSAFlAwQBKgQQAlP9aUkAhf81HaSa+AxgS6AiBCBO\n" +
            "9Vp6pabXgrdi71IQVPUYKyugXytFNUkrFC4CUYeO8A==\n" +
            "-----END PKCS7-----";


    public static void main(String[] args) throws Exception {
        //CMSSignatureBuilder signatureService = new CMSSignatureBuilder(new MockDNIe("08888888D"));
        //log.info("privateKey" + new String(PEMUtils.getPEMEncoded(signatureService.getPrivateKey())));
        //log.info("cert" + new String(PEMUtils.getPEMEncoded(signatureService.getX509Certificate())));
        //Security.insertProviderAt(new BouncyCastleProvider(),1);
        //signAndEncrypt(signatureService);
        new EncryptTest().sendMessageToBrowser();
        System.exit(0);
    }

    public EncryptTest() {
        super();
    }

    private static byte[] encrypt(CMSSignatureBuilder signatureService) throws Exception {
        log.info("signAndEncrypt");
        byte[] encryptedBytes = Encryptor.encryptToCMS("text to encrypt".getBytes(), signatureService.getX509Certificate());
        log.info("encryptedBytes: " + new String(encryptedBytes));
        log.info("Decrypted text: " + new String(Encryptor.decryptCMS(encryptedBytes, signatureService.getPrivateKey())));
        return encryptedBytes;
    }

    private static void signAndEncrypt(CMSSignatureBuilder signatureService) throws Exception {
        log.info("signAndEncrypt");
        CMSSignedMessage cmsSignedMessage = signatureService.signData("Hello text signed and encrypted".getBytes());
        byte[] encryptedBytes = Encryptor.encryptToCMS(cmsSignedMessage.getEncoded(),
                signatureService.getX509Certificate());

        log.info("X509Certificate: " + signatureService.getX509Certificate());

        log.info("PKCS7 signed and encrypted: " + new String(encryptedBytes));
        byte[] decryptedBytes = Encryptor.decryptCMS(encryptedBytes, signatureService.getPrivateKey());
        CMSSignedMessage signedData = new CMSSignedMessage(decryptedBytes);
        signedData.isValidSignature();
        log.info("SignedContent: " + signedData.getSignedContentStr());
    }

    private static void decryptPEM() throws Exception {
        PrivateKey privateKey = PEMUtils.fromPEMToRSAPrivateKey(FORGE_PRIVATE_KEY);
        byte[] decryptedBytes = Encryptor.decryptCMS(ENCRYPTED_CONTENT.getBytes(), privateKey);
        log.info("decryptedBytes: " + new String(decryptedBytes));
    }

    private static final String QR_CODE = "eid=https://voting.ddns.net/currency-server;op=0;uid=0657d937-515f-411a-a15b-7eb265b4aa11;";

    private void sendMessageToBrowser() throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("UUID", qrMessageDto.getUUID()));
        urlParameters.add(new BasicNameValuePair("operation", qrMessageDto.getOperation()));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                CurrencyOperation.QR_INFO.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID), urlParameters);
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("status: " + responseDto.getStatusCode() + " - responseDto: " + responseDto.getMessage());
        }
        SessionCertificationDto sessionCertificationDto = JSON.getMapper().readValue(responseDto.getMessageBytes(),
                SessionCertificationDto.class);
        PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(sessionCertificationDto.getPublicKeyPEM());

        MessageDto messageDto = new MessageDto();
        messageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE).setDeviceToUUID(qrMessageDto.getUUID());

        MessageDto messageContent = new MessageDto();
        messageContent.setOperation(new OperationTypeDto(CurrencyOperation.SESSION_CERTIFICATION, null)).setMessage("");
        SessionCertificationDto sessionCertification = new SessionCertificationDto();
        sessionCertification.setPrivateKeyPEM("setPrivateKeyPEM");
        String base64Data = Base64.getEncoder().encodeToString(
                JSON.getMapper().writeValueAsString(sessionCertification).getBytes());
        messageContent.setBase64Data(base64Data);

        byte[] encryptedMessage = Encryptor.encryptToCMS(JSON.getMapper().writeValueAsBytes(messageContent), publicKey);
        messageDto.setEncryptedMessage(new String(encryptedMessage));

        responseDto = HttpConn.getInstance().doPostRequest(JSON.getMapper().writeValueAsBytes(messageDto), null,
                CurrencyOperation.MSG_TO_DEVICE.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));

        log.info("message: " + responseDto.getMessage());
    }


}