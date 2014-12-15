package org.votingsystem.test.cooin

import org.apache.log4j.Logger
import org.votingsystem.signature.util.AESParams
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.test.util.TestUtils

Logger log = TestUtils.init(AESParams.class)

AESParams aesParams = new AESParams()
String encryptedText =  Encryptor.encryptAES("Tests cipher", aesParams)
System.out.println("encryptedText: " + encryptedText)
String decryptedText =  Encryptor.decryptAES(encryptedText, aesParams)
System.out.println("decryptedText: " + decryptedText)