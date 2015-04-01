package org.votingsystem.test.misc;

import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PEMCertFromJKS {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(PEMCertFromJKS.class);
        String keyStorePath="./certs/AccessControl.jks";
        String keyAlias="AccessControlKeys";
        String keyPassword="PemPass";
        byte[] pemCertBytes = getPemCertFromKeyStore(keyStorePath, keyAlias, keyPassword);
        File pemcertFile = new File("AccessControl.pem");
        pemcertFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(pemCertBytes), pemcertFile);
        log.info("Pem file path: " + pemcertFile.getAbsolutePath());
        TestUtils.finish("OK");
    }

    private static byte[] getPemCertFromKeyStore(String keyStorePath, String keyAlias, String keyPassword) throws Exception {
        KeyStore keyStore = SignatureService.loadKeyStore(keyStorePath, keyAlias, keyPassword);
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        return CertUtils.getPEMEncoded(certSigner);
    }

}