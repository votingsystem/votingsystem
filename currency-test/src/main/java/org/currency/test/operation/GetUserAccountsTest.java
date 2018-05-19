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
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import java.util.UUID;
import java.util.logging.Logger;


public class GetUserAccountsTest extends BaseTest {

    private static final Logger log = Logger.getLogger(GetUserAccountsTest.class.getName());

    public static void main(String[] args) throws Exception {
        new GetUserAccountsTest().test();
        System.exit(0);
    }

    public void test() throws Exception {
        MockDNIe mockDNIe = new MockDNIe("08888888D");
        CMSSignatureBuilder signatureService = new CMSSignatureBuilder(mockDNIe);
        OperationDto operationDto = new OperationDto(Constants.CURRENCY_SERVICE_ENTITY_ID,
                CurrencyOperation.GET_USER_ACCOUNTS).setUUID(UUID.randomUUID().toString());
        CMSSignedMessage cmsSignedMessage = signatureService.signDataWithTimeStamp(
                new JSON().getMapper().writeValueAsBytes(operationDto), Constants.TIMESTAMP_SERVICE_URL);
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(cmsSignedMessage.toPEM(),
                MediaType.PKCS7_SIGNED, CurrencyOperation.GET_USER_ACCOUNTS.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("StatusCode: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }

}
