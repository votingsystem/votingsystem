package org.votingsystem.test.util

import net.sf.json.JSONObject
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.signature.util.KeyGeneratorVS
import org.votingsystem.util.FileUtils

Logger log = TestUtils.init(Test.class, [:])
String passw = "password"

Encryptor.EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(passw, "Text to Encrypt".getBytes("UTF-8"))
JSONObject bundleJSON = bundle.toJSON()
log.debug("bundleJSON: " + bundleJSON.toString())

bundle = Encryptor.EncryptedBundle.parse(bundleJSON)
byte[] decryptedBytes = Encryptor.pbeAES_Decrypt(passw, bundle)
log.debug("decryptedtext: " + new String(decryptedBytes))

System.exit(0)

