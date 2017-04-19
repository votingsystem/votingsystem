package org.votingsystem.test.misc;

import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.QRResponseDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.xml.XML;

import java.util.Base64;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRTest extends BaseTest {

    private static final Logger log = Logger.getLogger(QRTest.class.getName());

    private static String QR_CODE = "eid=https://voting.ddns.net/idprovider;uid=75b8889a-3bd7-47ee-bec3-1bcde186ed56;";

    public static void main(String[] args) throws Exception {
        //String reqContentType = "application/xml";
        String reqContentType = "application/json";
        new QRTest().getInfo(reqContentType);
        System.exit(0);
    }

    public QRTest() {
        super();
    }

    public void getInfo(String reqContentType) throws Exception {
        log.info("reqContentType: " + reqContentType);
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        log.info(qrMessageDto.getSystemEntityID());
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(qrMessageDto.getUUID().getBytes(), reqContentType,
                OperationType.GET_QR_INFO.getUrl(qrMessageDto.getSystemEntityID()));

        log.info("QRresponse: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        QRResponseDto qrResponseDto = null;
        ElectionDto electionDto = null;
        switch (reqContentType) {
            case "application/json":
                qrResponseDto = JSON.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);
                electionDto = qrResponseDto.getDataJson(ElectionDto.class);
                break;
            case "application/xml":
                qrResponseDto = XML.getMapper().readValue(responseDto.getMessageBytes(), QRResponseDto.class);
                electionDto = qrResponseDto.getDataXml(ElectionDto.class);
                break;
        }
        log.info("dataBase64: " + new String(Base64.getDecoder().decode(qrResponseDto.getBase64Data())));
        log.info("electionDto.getEntityId: " + electionDto.getEntityId());
    }

}
