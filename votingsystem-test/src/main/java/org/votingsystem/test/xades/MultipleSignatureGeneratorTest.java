package org.votingsystem.test.xades;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.test.Constants;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.xml.SignatureValidator;
import org.votingsystem.testlib.xml.XmlSignature;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MultipleSignatureGeneratorTest extends BaseTest {

    private static final Logger log = Logger.getLogger(MultipleSignatureGeneratorTest.class.getName());

    //private static String TEST_ENTITY = "https://voting.ddns.net/idprovider";
    private static String TEST_ENTITY = "https://voting.ddns.net/voting-service";


    private static String SIGNER1_KEYSTORE = "certs/fake_08888888D.jks";
    private static String SIGNER2_KEYSTORE = "certs/fake_00012345V.jks";
    private static Boolean WITH_TIMESTAMP_VALIDATION = Boolean.TRUE;


    public MultipleSignatureGeneratorTest() {
        super();
    }

    public static void main(String[] args) throws Exception {
        MultipleSignatureGeneratorTest signatureGenerator = new MultipleSignatureGeneratorTest();
        signatureGenerator.signDocument();
    }


    public void signDocument() throws Exception {
        AbstractSignatureTokenConnection signingToken1 = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(SIGNER1_KEYSTORE).openStream(),
                new KeyStore.PasswordProtection(Constants.PASSW_DEMO.toCharArray()));
        AbstractSignatureTokenConnection signingToken2 = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(SIGNER2_KEYSTORE).openStream(),
                new KeyStore.PasswordProtection(Constants.PASSW_DEMO.toCharArray()));

        MetadataDto metadataDto = new MetadataDto();
        metadataDto.setEntity(new SystemEntityDto("SystemEntityId", SystemEntityType.VOTING_SERVICE_PROVIDER));
        byte[] xmlToSign = XML.getMapper().writeValueAsBytes(metadataDto);
        log.info("xmlToSign: " + new String(xmlToSign));


        byte[] signedDocumentBytes = new XAdESSignature().sign(xmlToSign, signingToken1,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        log.info("1 signer: " + new String(signedDocumentBytes));
        signedDocumentBytes = new XAdESSignature().sign(signedDocumentBytes, signingToken2,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        log.info("2 signer: " + new String(signedDocumentBytes));
        validateSignedDocument(signedDocumentBytes);
        //validateSignatureWithAndroid(signedDocumentBytes);
        System.exit(0);
    }

    public void validateSignatureWithAndroid(byte[] signedDocumentBytes) throws Exception {
        Set<XmlSignature> signatures = new SignatureValidator(signedDocumentBytes).validate();
        log.info("signatures.size: " + signatures.size());
    }

    public void validateSignedDocument(byte[] signedDocumentBytes) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signedDocumentBytes)));
        urlParameters.add(new BasicNameValuePair("withTimeStampValidation", WITH_TIMESTAMP_VALIDATION.toString()));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                OperationType.VALIDATE_SIGNED_DOCUMENT.getUrl(TEST_ENTITY), urlParameters);
        log.info("response: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }
}
