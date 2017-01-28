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
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureGenerator extends BaseTest {

    private static final Logger log = Logger.getLogger(SignatureGenerator.class.getName());


    private static String SIGNER_KEYSTORE = "certs/fake_08888888D.jks";
    private static Boolean WITH_TIMESTAMP_VALIDATION = Boolean.FALSE;


    public SignatureGenerator() {
        super();
    }

    public static void main(String[] args) throws Exception {
        //FileUtils.getBytesFromStream(Thread.currentThread().getContextClassLoader().getResource("").openStream())
        SignatureGenerator signatureGenerator = new SignatureGenerator();
        byte[] signedDocumentBytes = signatureGenerator.signDocument();
        signatureGenerator.sendToServer(signedDocumentBytes);
        System.exit(0);
    }


    public byte[] signDocument() throws Exception {
        MetadataDto metadataDto = new MetadataDto();
        metadataDto.setEntity(new SystemEntityDto("SystemEntityId", SystemEntityType.VOTING_SERVICE_PROVIDER))
                .setOperation(new OperationTypeDto(OperationType.GET_METADATA, null));
        byte[] xmlToSign = XML.getMapper().writeValueAsBytes(metadataDto);
        log.info("xmlToSign: " + new String(xmlToSign));
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource(SIGNER_KEYSTORE).openStream();
        AbstractSignatureTokenConnection signingToken = new JKSSignatureToken(inputStream,
                org.votingsystem.util.Constants.PASSW_DEMO);
        byte[] signedDocumentBytes = org.votingsystem.xades.XAdESSignature.sign(xmlToSign, signingToken,
                new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        log.info("signatureBytes: " + new String(signedDocumentBytes));
        validateSignatureWithAndroid(signedDocumentBytes);
        return signedDocumentBytes;
    }

    public void validateSignatureWithAndroid(byte[] signedDocumentBytes) throws Exception {
        Set<XmlSignature> signatures = new SignatureValidator(signedDocumentBytes).validate();
        log.info("num. signatures: " + signatures.size());
    }

    public void validateSignedDocument(byte[] signedDocumentBytes) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signedDocumentBytes)));
        urlParameters.add(new BasicNameValuePair("withTimeStampValidation", WITH_TIMESTAMP_VALIDATION.toString()));
        ResponseDto responseDto = HttpConn.getInstance().doPostFormRequest(
                OperationType.VALIDATE_SIGNED_DOCUMENT.getUrl(Constants.ID_PROVIDER_ENTITY_ID), urlParameters);
        log.info("response: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }

    public void sendToServer(byte[] signedXML) {
        ResponseDto response = HttpConn.getInstance().doPostRequest(signedXML, MediaType.XML,
                "http://votingsystem.ddns.net/currency-server/api/test/cms-document1");
        log.info("statusCode: " + response.getStatusCode() + " - " + response.getMessage());
    }

}
