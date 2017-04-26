package org.currency.test.operation;

import org.currency.test.Constants;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.OperationDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.util.UUID;
import java.util.logging.Logger;


public class RegisterUserTest extends BaseTest {

    private static final Logger log = Logger.getLogger(RegisterUserTest.class.getName());

    public static void main(String[] args) throws Exception {
        new RegisterUserTest().test();
        System.exit(0);
    }

    public void test() throws Exception {
        MockDNIe mockDNIe = new MockDNIe("08888888D");
        OperationDto operationDto = new OperationDto(new OperationTypeDto(CurrencyOperation.REGISTER_USER,
                Constants.CURRENCY_SERVICE_ENTITY_ID)).setUUID(UUID.randomUUID().toString());
        byte[] signedBytes =  XAdESSignature.sign(XML.getMapper().writeValueAsBytes(operationDto),
                mockDNIe.getJksSignatureToken(), new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(signedBytes,
                MediaType.XML, CurrencyOperation.REGISTER_USER.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("StatusCode: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }

}
