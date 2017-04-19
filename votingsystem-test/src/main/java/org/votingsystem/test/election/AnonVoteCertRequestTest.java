package org.votingsystem.test.election;

import org.apache.commons.collections.map.HashedMap;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.Constants;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonVoteCertRequestTest extends BaseTest {

    private static final Logger log = Logger.getLogger(AnonVoteCertRequestTest.class.getName());

    private static String QR_CODE = "eid=https://voting.ddns.net/idprovider;uid=14fa9b79-6fd9-4773-a8e4-13496609c34c;";
    private static String KEYSTORE = "certs/fake_08888888D.jks";
    private static String KEYSTORE_PASSWORD = Constants.PASSW_DEMO;

    public static void main(String[] args) throws Exception {
        //String reqContentType = "application/xml";
        String reqContentType = "application/json";
        new AnonVoteCertRequestTest().sendRequest(reqContentType);
        System.exit(0);
    }

    public AnonVoteCertRequestTest() {
        super();
    }

    public void sendRequest(String reqContentType) throws Exception {
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        log.info(qrMessageDto.getSystemEntityID());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), "application/json",
                OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID()));

        log.info("electionXML: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        QRResponseDto qrResponseDto = JSON.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);

        Map<String, byte[]> fileMap = new HashedMap();
        fileMap.put("test1", "test1Value".getBytes());
        fileMap.put("test2", "test2Value".getBytes());

        //String serviceURL = "https://voting.ddns.net/idprovider/accessRequest";
        String serviceURL = OperationType.ANON_VOTE_CERT_REQUEST.getUrl(qrMessageDto.getSystemEntityID());

        responseDto = HttpConn.getInstance().doPostMultipartRequest(fileMap, serviceURL);

        log.info("electionXML: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }

    }

}
