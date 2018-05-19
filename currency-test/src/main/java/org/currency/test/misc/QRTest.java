package org.currency.test.misc;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.votingsystem.dto.QRMessageDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
import org.votingsystem.xml.XML;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRTest extends BaseTest {

    private static final Logger log = Logger.getLogger(QRTest.class.getName());
    //invalidated
    //private static String QR_CODE = "eid=https://voting.ddns.net/currency-server;op=0;uid=eb3edf71-55ac-49a9-890a-9208910e3f2e;";
    private static String QR_CODE = "eid=https://voting.ddns.net/currency-server;op=0;uid=d50ae206-c1c4-4477-b459-40fa06b519b4;";

    public static void main(String[] args) throws Exception {
        new QRTest().getInfo(MediaType.JSON);
        System.exit(0);
    }

    public QRTest() {
        super();
    }

    public void getInfo(String reqContentType) throws Exception {
        log.info("reqContentType: " + reqContentType + " - QR_CODE: " + QR_CODE);
        QRMessageDto qrMessageDto = QRMessageDto.FROM_QR_CODE(QR_CODE);
        log.info(qrMessageDto.getSystemEntityID());
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("UUID", qrMessageDto.getUUID()));
        urlParameters.add(new BasicNameValuePair("operation", qrMessageDto.getOperation()));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                CurrencyOperation.QR_INFO.getUrl(qrMessageDto.getSystemEntityID()), urlParameters);

        log.info("QRresponse: " + responseDto.getMessage());
        if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
            log.info("bad request - msg: " + responseDto.getMessage());
            System.exit(0);
        }
        switch (reqContentType) {
            case MediaType.JSON:
                switch (qrMessageDto.getOperation()) {
                    case QRUtils.GEN_BROWSER_CERTIFICATE:
                        SessionCertificationDto sessionCertificationDto = new JSON().getMapper().readValue(
                                responseDto.getMessageBytes(), SessionCertificationDto.class);
                        buildSessionCertificates(sessionCertificationDto);
                        break;
                }
                break;
            case MediaType.XML:
                switch (qrMessageDto.getOperation()) {
                    case QRUtils.GEN_BROWSER_CERTIFICATE:
                        SessionCertificationDto sessionCertificationDto = new XML().getMapper().readValue(
                                responseDto.getMessageBytes(), SessionCertificationDto.class);
                        break;
                }
                break;
        }

    }

    private void buildSessionCertificates(SessionCertificationDto sessionCertificationDto) {
        log.info("BrowserCsr: " + sessionCertificationDto.getBrowserCsr());
    }

}
