package org.votingsystem.xades;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.JKSSignatureToken;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.BaseTest;
import org.votingsystem.Constants;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.crypto.xml.SignatureValidator;
import org.votingsystem.crypto.xml.XmlSignature;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MultipleSignatureGenerator extends BaseTest {

    private static final Logger log = Logger.getLogger(MultipleSignatureGenerator.class.getName());

    private static String TEST_ENTITY = "https://votingsystem.ddns.net/idprovider";


    private static String SIGNER1_KEYSTORE = "certs/fake_08888888D.jks";
    private static String SIGNER2_KEYSTORE = "certs/fake_00012345V.jks";
    private static Boolean WITH_TIMESTAMP_VALIDATION = Boolean.TRUE;


    public MultipleSignatureGenerator() {
        super();
    }

    public static void main(String[] args) throws Exception {
        MultipleSignatureGenerator signatureGenerator = new MultipleSignatureGenerator();
        signatureGenerator.signDocument();
    }


    public void signDocument() throws Exception {
        AbstractSignatureTokenConnection signingToken1 = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(SIGNER1_KEYSTORE).openStream(),
                org.votingsystem.util.Constants.PASSW_DEMO);
        AbstractSignatureTokenConnection signingToken2 = new JKSSignatureToken(
                Thread.currentThread().getContextClassLoader().getResource(SIGNER2_KEYSTORE).openStream(),
                org.votingsystem.util.Constants.PASSW_DEMO);

        MetadataDto metadataDto = new MetadataDto();
        metadataDto.setEntity(new SystemEntityDto("SystemEntityId", SystemEntityType.VOTING_SERVICE_PROVIDER));
        byte[] xmlToSign = XML.getMapper().writeValueAsBytes(metadataDto);
        log.info("xmlToSign: " + new String(xmlToSign));


        byte[] signedDocumentBytes = XAdESSignature.sign(xmlToSign, signingToken1,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        log.info("1 signer: " + new String(signedDocumentBytes));
        signedDocumentBytes = XAdESSignature.sign(signedDocumentBytes, signingToken2,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        log.info("2 signer: " + new String(signedDocumentBytes));
        //validateSignedDocument(signedDocumentBytes);
        validateSignatureWithAndroid(signedDocumentBytes);
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
        ResponseDto responseDto = HttpConn.getInstance().doPostFormRequest(
                OperationType.VALIDATE_SIGNED_DOCUMENT.getUrl(TEST_ENTITY), urlParameters);
        log.info("response: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }
}
