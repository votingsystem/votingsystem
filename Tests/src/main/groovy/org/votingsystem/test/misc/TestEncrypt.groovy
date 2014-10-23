package org.votingsystem.test.misc

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.h2.util.IOUtils
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.test.util.H2Utils
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

Logger log = TestUtils.init(TestEncrypt.class, "./TestEncrypt")

SignatureService signatureService = SignatureService.getUserVSSignatureService("00111222V", UserVS.Type.USER)
File fileToEncrypt =  TestUtils.getFileFromResources("plainWallet")
JSONObject walletJSON = JSONSerializer.toJSON(fileToEncrypt.text)
log.debug(walletJSON.toString(3))


ResponseVS response = signatureService.encryptToCMS(fileToEncrypt.getBytes(), signatureService.certSigner)
if(ResponseVS.SC_OK != response.statusCode) throw new ExceptionVS(response.getMessage())
File encryptedFile = new File (ContextVS.APPDIR + "/encryptedWallet")
FileUtils.copyStreamToFile(new ByteArrayInputStream(response.messageBytes), encryptedFile)

byte[] decryptedBytes = signatureService.decryptCMS(response.messageBytes, Locale.getDefault())
log.debug("Decrypted message:" + new String(decryptedBytes))


TestUtils.finish("OK")
