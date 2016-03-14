package org.votingsystem.test.misc;


import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.Encryptor;
import org.votingsystem.util.crypto.PEMUtils;

import java.util.logging.Logger;

public class PKCS7EncryptedData {

    private static Logger log =  Logger.getLogger(PKCS7EncryptedData.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D");
        log.info("privateKey" + new String(PEMUtils.getPEMEncoded(signatureService.getPrivateKey())));
        log.info("cert" + new String(PEMUtils.getPEMEncoded(signatureService.getCertSigner())));
        signAndEncrypt(signatureService);
    }

    private static byte[] encrypt(SignatureService signatureService) throws Exception {
        log.info("signAndEncrypt");
        byte[] encryptedBytes = Encryptor.encryptToCMS("text to encrypt".getBytes(), signatureService.getCertSigner());
        log.info("encryptedBytes: " + new String(encryptedBytes));
        log.info("Decrypted text: " + new String(Encryptor.decryptCMS(encryptedBytes, signatureService.getPrivateKey())));
        return encryptedBytes;
    }

    private static void signAndEncrypt(SignatureService signatureService) throws Exception {
        log.info("signAndEncrypt");
        CMSSignedMessage cmsSignedMessage = signatureService.signData("Hello text signed and encrypted");
        byte[] encryptedBytes = Encryptor.encryptToCMS(cmsSignedMessage.getEncoded(),
                signatureService.getCertSigner());
        byte[] decryptedBytes = Encryptor.decryptCMS(encryptedBytes, signatureService.getPrivateKey());
        CMSSignedMessage signedData = new CMSSignedMessage(decryptedBytes);
        log.info("SignedContent: " + signedData.getSignedContentStr());
    }

}
