package org.votingsystem.test.misc

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.UserVS
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.FileUtils


Logger log = TestUtils.init(TestEncryptWallet.class, "./TestEncryptWallet")

SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER)



File fileToEncrypt =  TestUtils.getFileFromResources("plainWallet")
//JSONObject walletJSON = JSONSerializer.toJSON(fileToEncrypt.text)


byte[] encryptedBytes = signatureService.encryptToCMS(fileToEncrypt.getBytes(), signatureService.certSigner)
File encryptedFile = new File (ContextVS.APPDIR + File.separator + ContextVS.WALLET_FILE_NAME)
encryptedFile.createNewFile()
FileUtils.copyStreamToFile(new ByteArrayInputStream(encryptedBytes), encryptedFile)

byte[] decryptedBytes = signatureService.decryptCMS(encryptedFile.getBytes())
log.debug("Decrypted message:" + new String(decryptedBytes))


TestUtils.finish("OK")
