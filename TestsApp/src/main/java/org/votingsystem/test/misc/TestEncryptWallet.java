package org.votingsystem.test.misc;

import org.votingsystem.model.UserVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestEncryptWallet {

    private static Logger log =  Logger.getLogger(TestEncryptWallet.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        File fileToEncrypt = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("plainWallet"));
        //Map<String, Object> dataMap = JSON.getMapper().readValue(fileToEncrypt, new TypeReference<HashMap<String, Object>>() {});
        byte[] encryptedBytes = signatureService.encryptToCMS(FileUtils.getBytesFromFile(fileToEncrypt),
                signatureService.getCertSigner());
        File encryptedFile = new File (ContextVS.APPDIR + File.separator + ContextVS.WALLET_FILE_NAME);
        encryptedFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(encryptedBytes), encryptedFile);
        byte[] decryptedBytes = signatureService.decryptCMS(FileUtils.getBytesFromFile(encryptedFile));
        log.info("Decrypted message:" + new String(decryptedBytes));
        System.exit(0);
    }
}


