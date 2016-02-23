package org.votingsystem.test.misc;

import org.votingsystem.dto.UserCertificationRequestDto;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.logging.Logger;


public class CMSEncryption {

    private static Logger log =  Logger.getLogger(CMSEncryption.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        String file = "Cert_USER_Currency_07553172H";
        String keyStorePath="./certs/" + file + ".jks";
        String keyAlias="UserTestKeysStore";
        String keyPassword="ABCDE";
        KeyStore keyStore = SignatureService.loadKeyStore(keyStorePath, keyPassword);
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey)keyStore.getKey(keyAlias, keyPassword.toCharArray());

        Encryptor encryptor = new Encryptor(certSigner, privateKey);
        byte[] encryptedCMS = encryptor.encryptToCMS(UUID.randomUUID().toString().getBytes(), certSigner);
        log.info("encryptedCMS: " + new String(encryptedCMS));
        UserCertificationRequestDto dto = new UserCertificationRequestDto(null, null, encryptedCMS);
        String dtoStr = JSON.getMapper().writeValueAsString(dto);
        log.info("UserCertificationRequestDto: " + dtoStr);
        dto = JSON.getMapper().readValue(dtoStr, UserCertificationRequestDto.class);
        log.info("decryptCMS: " + new String(encryptor.decryptCMS(dto.getToken())));
    }


}
