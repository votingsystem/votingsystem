package org.currency.test.operation;


import org.votingsystem.crypto.Encryptor;
import org.votingsystem.dto.AESParamsDto;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.JSON;
import java.util.logging.Logger;

public class AESTest extends BaseTest {

    private static Logger log =  Logger.getLogger(AESTest.class.getName());

    public AESTest() {}


    public static void main(String[] args) throws Exception {
        new AESTest().test();
        System.exit(0);
    }

    public void test() throws Exception {
        AESParamsDto aesParamsDto = AESParamsDto.CREATE();
        String aesStr = JSON.getMapper().writeValueAsString(aesParamsDto);
        log.info("aesParamsDtoStr: " + aesStr);
        aesParamsDto = JSON.getMapper().readValue(aesStr.getBytes(), AESParamsDto.class);
        log.info("aesParamsDto: " + aesParamsDto);
        String encryptedData = Encryptor.encryptAES("Message to encrypt", aesParamsDto);
        log.info("encryptedData: " + encryptedData);
        String plainData = Encryptor.decryptAES(encryptedData, aesParamsDto);
        log.info("plainData: " + plainData);
        System.exit(0);
    }

}
