package org.votingsystem.test.misc;

import org.votingsystem.model.UserVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestEncryptWallet {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(TestEncryptWallet.class, "./TestEncryptWallet");
        SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        File fileToEncrypt =  TestUtils.getFileFromResources("plainWallet");
        //Map<String, Object> dataMap = new ObjectMapper().readValue(fileToEncrypt, new TypeReference<HashMap<String, Object>>() {});
        byte[] encryptedBytes = signatureService.encryptToCMS(FileUtils.getBytesFromFile(fileToEncrypt),
                signatureService.getCertSigner());
        File encryptedFile = new File (ContextVS.APPDIR + File.separator + ContextVS.WALLET_FILE_NAME);
        encryptedFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(encryptedBytes), encryptedFile);
        byte[] decryptedBytes = signatureService.decryptCMS(FileUtils.getBytesFromFile(encryptedFile));
        log.info("Decrypted message:" + new String(decryptedBytes));
        TestUtils.finish("OK");
    }
}


