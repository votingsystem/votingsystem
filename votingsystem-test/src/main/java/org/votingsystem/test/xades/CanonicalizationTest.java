package org.votingsystem.test.xades;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.test.Constants;
import org.votingsystem.test.xml.ValidatorTest;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.testlib.xml.SignatureAlgorithm;
import org.votingsystem.testlib.xml.XAdESUtils;
import org.votingsystem.testlib.xml.XMLSignatureBuilder;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XAdESSignature;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

public class CanonicalizationTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(ValidatorTest.class);

    private static String SIGNER_KEYSTORE = "certs/fake_08888888D.jks";

    public static void main(String[] args) throws Exception {
        new CanonicalizationTest().signAndroid();
        System.exit(0);
    }

    public void signAndroid() throws Exception {
        byte[] xmlToSign = FileUtils.getBytesFromStream(Thread.currentThread().getContextClassLoader().getResource("temp1.xml").openStream());
        String textToSign = XMLUtils.prepareRequestToSign(xmlToSign);

        byte[] signedDocumentBytes = new XMLSignatureBuilder(textToSign.getBytes(), XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(), new MockDNIe("08888888D"),
                Constants.TIMESTAMP_SERVICE_URL).build();

        log.info("signedDocumentBytes: " + new String(signedDocumentBytes));
        validateSignedDocument(signedDocumentBytes);
    }

    public void signXADES() throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource(SIGNER_KEYSTORE).openStream();
        byte[] xmlToSign = FileUtils.getBytesFromStream(Thread.currentThread().getContextClassLoader().getResource("temp1.xml").openStream());
        AbstractSignatureTokenConnection signingToken = new JKSSignatureToken(inputStream,
                new KeyStore.PasswordProtection(org.votingsystem.util.Constants.PASSW_DEMO.toCharArray()));
        byte[] signedDocumentBytes = new XAdESSignature().sign(xmlToSign, signingToken,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        log.info("signedDocumentBytes: " + new String(signedDocumentBytes));
        validateSignedDocument(signedDocumentBytes);
    }

    public void validateSignedDocument(byte[] signedDocumentBytes) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signedDocumentBytes)));
        urlParameters.add(new BasicNameValuePair("withTimeStampValidation", Boolean.TRUE.toString()));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                OperationType.VALIDATE_SIGNED_DOCUMENT.getUrl(Constants.ID_PROVIDER_ENTITY_ID), urlParameters);
        log.info("response: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }
}
