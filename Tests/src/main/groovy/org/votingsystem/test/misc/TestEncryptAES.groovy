package org.votingsystem.test.misc

import org.apache.log4j.Logger
import org.votingsystem.model.ContextVS
import org.votingsystem.model.UserVS
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.FileUtils

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.Key
import java.security.SecureRandom


Logger log = TestUtils.init(TestEncryptWallet.class, "./TestEncryptAES")
String messageToEncrypt = "Hello from messageToEncrypt"

SecureRandom random = new SecureRandom();
IvParameterSpec iv = new IvParameterSpec(random.generateSeed(16));


KeyGenerator kg = KeyGenerator.getInstance("AES");
kg.init(random);
kg.init(256); // 192 and 256 bits may not be available
Key key = kg.generateKey();
log.debug("key: " + key.algorithm)
String keyBase64 = Base64.getEncoder().encodeToString(key.getEncoded())
log.debug("keyBase64: " + keyBase64)

byte[] decodeKeyBytes = Base64.getDecoder().decode(keyBase64)
SecretKey key2 = new SecretKeySpec(decodeKeyBytes, 0, decodeKeyBytes.length, "AES");

Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
cipher.init(Cipher.ENCRYPT_MODE, key2, iv);
byte[] stringBytes = messageToEncrypt.getBytes();
byte[] encryptedMessage = cipher.doFinal(stringBytes);
String encryptedMessageBase64 = Base64.getEncoder().encodeToString(encryptedMessage)
log.debug("encryptedMessageBase64: " + encryptedMessageBase64)

cipher.init(Cipher.DECRYPT_MODE, key, iv);
encryptedMessage = Base64.getDecoder().decode(encryptedMessageBase64);
stringBytes = cipher.doFinal(encryptedMessage);
String clearText = new String(stringBytes, "UTF8");
log.debug("clearText: " + clearText)

TestUtils.finish("OK")
