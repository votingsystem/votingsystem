package org.votingsystem.test.misc

import org.apache.log4j.Logger
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.test.util.SignatureService
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.FileUtils

import java.security.KeyStore
import java.security.cert.X509Certificate


Logger log = TestUtils.init(PEMCertFromJKS.class)

String keyStorePath="./certs/AccessControl.jks"
String keyAlias="AccessControlKeys"
String keyPassword="PemPass"


byte[] pemCertBytes = getPemCertFromKeyStore(keyStorePath, keyAlias, keyPassword)
File pemcertFile = new File("AccessControl.pem")
pemcertFile.createNewFile()
FileUtils.copyStreamToFile(new ByteArrayInputStream(pemCertBytes), pemcertFile)

log.debug("Pem file path: " + pemcertFile.absolutePath)
TestUtils.finish("OK")




private byte[] getPemCertFromKeyStore(String keyStorePath, String keyAlias, String keyPassword) {
    KeyStore keyStore = SignatureService.loadKeyStore(keyStorePath, keyAlias, keyPassword)
    X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
    return CertUtils.getPEMEncoded(certSigner)
}
