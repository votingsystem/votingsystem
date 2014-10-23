package org.votingsystem.test.misc

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils

Logger log = TestUtils.init(TestEncrypt.class, "./TestEncrypt")

SignatureService signatureService = SignatureService.getUserVSSignatureService("00111222V", UserVS.Type.USER)
/*File fileToEncrypt =  TestUtils.getFileFromResources("plainWallet")
JSONObject walletJSON = JSONSerializer.toJSON(fileToEncrypt.text)*/


ResponseVS response = signatureService.encryptToCMS(fileToEncrypt.getBytes(), signatureService.certSigner)
//if(ResponseVS.SC_OK != response.statusCode) throw new ExceptionVS(response.getMessage())
File encryptedFile = new File (ContextVS.APPDIR + "/encryptedWallet")
//FileUtils.copyStreamToFile(new ByteArrayInputStream(response.messageBytes), encryptedFile)

byte[] decryptedBytes = signatureService.decryptCMS(encryptedFile.getBytes(), Locale.getDefault())
log.debug("Decrypted message:" + new String(decryptedBytes))


TestUtils.finish("OK")
