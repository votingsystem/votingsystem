package org.votingsystem.test.misc;

import org.votingsystem.test.util.SignatureService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.crypto.KeyStoreUtil;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MockDNIeGenerator {

    private static Logger log =  Logger.getLogger(MockDNIeGenerator.class.getName());

    //90000000B - 00111222V - 08888888D - 03455543T

    //private static String subjectDN = "CN=Voting System Certificate Authority, OU=Certs";
    //private static String subjectDN = "CN=Mutua Madrileña - ROOT, OU=Certs";
    private static String subjectDN = "CN=FAKE ROOT DNIe CA, OU=Certs";
    private static String nif = "03455543T";
    private static String givenName = "Name-" + nif;
    private static String surname = "Surname-" + nif;
    private static String keyAlias = "keyMM";
    private static String password = "passwMM";


    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        //if(true) createRootKeyStore();
        KeyStore rootKeyStore = SignatureService.loadKeyStore("certs/RootKeyStore.jks", password);
        X509Certificate rootCert = (X509Certificate) rootKeyStore.getCertificate(keyAlias);
        PrivateKey rootPrivateKey = (PrivateKey)rootKeyStore.getKey(keyAlias, password.toCharArray());
        KeyStore userKeyStore = generateKeysStore(givenName, surname, nif, keyAlias, password.toCharArray(),
                rootCert, rootPrivateKey);
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(userKeyStore, password.toCharArray());
        File outputFile = FileUtils.copyBytesToFile(keyStoreBytes, new File(System.getProperty("user.home") +
                "/" + nif + ".jks"));
        log.info("KeyStore path: " + outputFile.getAbsolutePath());
        System.exit(0);
    }

    public static void createRootKeyStore() throws Exception {
        KeyStore rootKeyStore = generateRootKeyStore(subjectDN, keyAlias, password.toCharArray());
        byte[] keyStoreBytes = KeyStoreUtil.getBytes(rootKeyStore, password.toCharArray());
        File outputFile = FileUtils.copyBytesToFile(keyStoreBytes, new File(System.getProperty("user.home") +
                "/RootKeyStore.jks"));
        log.info("KeyStore path: " + outputFile.getAbsolutePath());
        System.exit(0);
    }

    public static KeyStore generateKeysStore(String givenName, String surname, String nif, String keyAlias, char[] password,
                                X509Certificate rootCert, PrivateKey rootPrivateKey) throws Exception {
        log.info("generateKeysStore - nif: " + nif);
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(rootCert,
                rootPrivateKey, keyAlias);
        String testUserDN = null;
        if(surname == null) testUserDN = format("GIVENNAME={0}, SERIALNUMBER={1}", givenName, nif);
        else testUserDN = format("GIVENNAME={0}, SURNAME={1} , SERIALNUMBER={2}", givenName, surname, nif);
        //String strSubjectDN = "CN=Voting System Cert Authority , OU=VotingSystem"
        //KeyStore rootCAKeyStore = KeyStoreUtil.createRootKeyStore (validFrom.getTime(), (validTo.getTime() - validFrom.getTime()),
        //        userPassword.toCharArray(), keyAlias, strSubjectDN);
        //X509Certificate certSigner = (X509Certificate)rootCAKeyStore.getX509Certificate(keyAlias);
        //PrivateKey privateKeySigner = (PrivateKey)rootCAKeyStore.getKey(keyAlias, userPassword.toCharArray());
        //X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKeySigner,  keyAlias);
        return KeyStoreUtil.createUserKeyStore(validFrom.getTime(),
                (validTo.getTime() - validFrom.getTime()), password, ContextVS.KEYSTORE_USER_CERT_ALIAS,
                rootCAPrivateCredential, testUserDN);
    }

    private static KeyStore generateRootKeyStore(String subjectDN, String keyAlias, char[] password) throws Exception {
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();

        java.security.KeyStore keyStore = KeyStoreUtil.createRootKeyStore(validFrom, validTo, password, keyAlias,
                subjectDN);
        return keyStore;
    }


}

