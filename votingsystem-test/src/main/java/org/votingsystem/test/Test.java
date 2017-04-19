package org.votingsystem.test;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.testlib.xml.SignatureAlgorithm;
import org.votingsystem.testlib.xml.XAdESUtils;
import org.votingsystem.testlib.xml.XMLSignatureBuilder;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) throws Exception {
        new Test().testHttp();
        System.exit(0);
    }

    public void testHttp() throws Exception {
        ResponseDto response = HttpConn.getInstance().doGetRequest(
                OperationType.GET_METADATA.getUrl(org.votingsystem.test.Constants.CURRENCY_SERVICE_ENTITY_ID), MediaType.XML);
        log.info("Message: " + response.getMessage());
    }

    public void testCert() throws Exception {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("UUID", UUID.randomUUID().toString()));
        urlParameters.add(new BasicNameValuePair("operation", "0"));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                "https://voting.ddns.net/currency-server/api/currency-qr/info", urlParameters);

        //doPostForm(String targetURL, List<NameValuePair> urlParameters)
        log.info("responseDto: " + responseDto.getMessage());
    }

    public void run() throws Exception {
        String textToSign = XMLUtils.prepareRequestToSign("<dummy>test</dummy>".getBytes());

        byte[] signatureBytes = new XMLSignatureBuilder(textToSign.getBytes(), XAdESUtils.XML_MIME_TYPE,
                SignatureAlgorithm.RSA_SHA_256.getName(), new MockDNIe("08888888D"),
                Constants.TIMESTAMP_SERVICE_URL).build();
        ResponseDto response = HttpConn.getInstance().doPostRequest(signatureBytes, MediaType.XML,
                "https://voting.ddns.net/currency-server/api/test-signed");
        log.info("response: " + response.getMessage());
    }

    public static String dtoJSON(String identityServiceEntity, String votingServiceEntity, String revocationHashBase64
            , String electionUUID) throws Exception {
        return "{\"identityServiceEntity\":\"" + identityServiceEntity + "\",\"votingServiceEntity\":\"" +
                votingServiceEntity + "\"," + "\"revocationHashBase64\":\"" + revocationHashBase64 +
                "\",\"electionUUID\":\"" + electionUUID + "\"}";
    }

    public static void test() throws Exception {
        CertVoteExtensionDto dto = new CertVoteExtensionDto("indentityServiceEntity", "votingServiceEntity",
                "revocationHashBase64", "electionUUID");
        log.info("dto: " + JSON.getMapper().writeValueAsString(dto));
    }

}
