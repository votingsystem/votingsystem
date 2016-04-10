package org.votingsystem.test.misc;


import org.votingsystem.dto.AESParamsDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.Encryptor;

import java.util.logging.Logger;

public class AESParams {

    private static Logger log =  Logger.getLogger(AESParams.class.getName());


    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
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
