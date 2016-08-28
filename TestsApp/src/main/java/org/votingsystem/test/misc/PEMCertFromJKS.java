package org.votingsystem.test.misc;

import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PEMCertFromJKS {

    private static Logger log =  Logger.getLogger(PEMCertFromJKS.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");

        String file = "Cert_BANK_03455543T";
        String keyStorePath="./certs/" + file + ".jks";
        String keyAlias="UserTestKeysStore";
        String keyPassword="ABCDE";
        byte[] pemCertBytes = getPemCertChainFromKeyStore(keyStorePath, keyAlias, keyPassword);
        File pemcertFile = new File(file + ".pem");
        pemcertFile.createNewFile();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(pemCertBytes), pemcertFile);
        log.info("Pem file path: " + pemcertFile.getAbsolutePath());
        System.exit(0);
    }

    private static byte[] getPemCertFromKeyStore(String keyStorePath, String keyAlias, String keyPassword) throws Exception {
        KeyStore keyStore = SignatureService.loadKeyStore(keyStorePath, keyPassword);
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        return PEMUtils.getPEMEncoded(certSigner);
    }

    private static byte[] getPemCertChainFromKeyStore(String keyStorePath, String keyAlias, String keyPassword) throws Exception {
        KeyStore keyStore = SignatureService.loadKeyStore(keyStorePath, keyPassword);
        Certificate[] certificateChain = keyStore.getCertificateChain(keyAlias);
        List<X509Certificate> certificateChainList = new ArrayList<>();
        for(Certificate certificate : certificateChain) {
            certificateChainList.add((X509Certificate) certificate);
        }
        return PEMUtils.getPEMEncoded(certificateChainList);
    }

}