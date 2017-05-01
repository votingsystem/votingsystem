package org.currency.test.operation;

import org.currency.test.Constants;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.pkcs7.CMSSignatureBuilder;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class RegisterCertificatesTest extends BaseTest {

    private static final Logger log = Logger.getLogger(RegisterCertificatesTest.class.getName());

    public static void main(String[] args) throws Exception {
        new RegisterCertificatesTest().test();
        System.exit(0);
    }

    public void test() throws Exception {
        MockDNIe mockDNIe = new MockDNIe("09999999J");
        CMSSignatureBuilder signatureService = new CMSSignatureBuilder(mockDNIe);
        String resourcesFolder = "certs";
        List<String> resourcesFolderFiles = getResourceFiles(resourcesFolder);
        StringBuilder sb = new StringBuilder();
        for(String fileName : resourcesFolderFiles) {
            if(fileName.toLowerCase().endsWith(".pem") && fileName.toLowerCase().startsWith("fake_")) {
                URL res = Thread.currentThread().getContextClassLoader().getResource(resourcesFolder + "/" + fileName);
                log.info("" + new String( FileUtils.getBytesFromStream(res.openStream())));
                sb.append(new String( FileUtils.getBytesFromStream(res.openStream())));
            }
        };
        OperationDto operationDto = new OperationDto(new OperationTypeDto(
                OperationType.CERT_USER_CHECK, Constants.ID_PROVIDER_ENTITY_ID));
        operationDto.setMessage(sb.toString());
        byte[] contentToSign = JSON.getMapper().writeValueAsBytes(operationDto);
        CMSSignedMessage signedMessage = signatureService.signDataWithTimeStamp(contentToSign, Constants.TIMESTAMP_SERVICE_URL);
        ResponseDto response = HttpConn.getInstance().doPostRequest(signedMessage.toPEM(), MediaType.PKCS7_SIGNED,
                Constants.ID_PROVIDER_ENTITY_ID + "/api/cert-issuer/register-certs");
        log.info("statusCode: " + response.getStatusCode() + " -message: " + response.getMessage());
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try(
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while( (resource = br.readLine()) != null ) {
                filenames.add( resource );
            }
        }
        log.info("filenames: " + filenames);
        return filenames;
    }

}
