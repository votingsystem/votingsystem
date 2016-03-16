package org.votingsystem.test.misc;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.model.User;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;


public class PKCS10Csr {

    private static Logger log =  Logger.getLogger(PKCS10Csr.class.getName());

    public static String CSR_REQUEST = "-----BEGIN CERTIFICATE REQUEST-----\n" +
            "MIIBjTCB9wIBADBOMRIwEAYDVQQFEwkwODg4ODg4OEQxGjAYBgNVBAQTEXN1cm5h\n" +
            "bWVfMDg4ODg4ODhEMRwwGgYDVQQqExNnaXZlbk5hbWVfMDg4ODg4ODhEMIGfMA0G\n" +
            "CSqGSIb3DQEBAQUAA4GNADCBiQKBgQCidwMrCPfrJaWuY6+xQxzfyIp/0bgj1TyG\n" +
            "zDdyNxfu4+NlWXxNK7Jjk9nP6sFtNZ4ioAKF++ocrI2RauVVdxu30PwaDK3EkLov\n" +
            "s5k5ppfNVpCe4PzpaV8itTx/52BobNkz7pBmqegPXUG8sDAYwz4ZlMxt2VyhrVVS\n" +
            "ItN2E0Z6kwIDAQABoAAwDQYJKoZIhvcNAQEFBQADgYEAN+ont3epawMWbmhGJaw9\n" +
            "y3V9oTk9qX9PqbHqNAf50CJHimBp7FKqQowW5KZZvBk7xSgO6VGneqbR+R7MaHGB\n" +
            "x39+ULnryyCD4VvZ+HrhHrgBVmDVL81UjdL4OWdFUst3b7CTr/NmACn2W3qv0nE2\n" +
            "iSQpEDcDcfZoXt05Z8u/T9k=\n" +
            "-----END CERTIFICATE REQUEST-----\n";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.load("08888888D");
        log.info(PEMUtils.getPEMEncodedStr(signatureService.getX509Certificate()));

        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(CSR_REQUEST.getBytes());
        log.info(csr.toString());
        Date validFrom = new Date();
        Date validTo = DateUtils.addDays(validFrom, 1).getTime(); //one year
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, signatureService.getPrivateKey(),
                signatureService.getX509Certificate(), validFrom, validTo);
        log.info(PEMUtils.getPEMEncodedStr(issuedCert));
    }

}
