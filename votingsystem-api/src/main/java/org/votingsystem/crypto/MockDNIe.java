package org.votingsystem.crypto;

import eu.europa.esig.dss.token.JKSSignatureToken;
import org.votingsystem.util.Constants;

import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MockDNIe {

    private X509Certificate x509Certificate;
    private Certificate[] certificateChain;
    private PrivateKey privateKey;
    private KeyStore keyStore;
    private String keyAlias;
    private String password;

    public MockDNIe(String nif) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException,
            UnrecoverableEntryException {
        URL res = Thread.currentThread().getContextClassLoader().getResource("certs/fake_" + nif + ".jks");
        this.keyStore = KeyStore.getInstance("JKS");
        keyStore.load(res.openStream(), Constants.PASSW_DEMO.toCharArray());
        loadKeyStore(keyStore, Constants.PASSW_DEMO.toCharArray(), Constants.USER_CERT_ALIAS);
    }

    public MockDNIe(KeyStore keyStore, char[] password, String keyAlias) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException {
        loadKeyStore(keyStore, password, keyAlias);
    }

    private void loadKeyStore(KeyStore keyStore, char[] password, String keyAlias) throws KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {
        this.keyStore = keyStore;
        this.keyAlias = keyAlias;
        certificateChain = keyStore.getCertificateChain(keyAlias);
        x509Certificate = (X509Certificate)keyStore.getCertificate(keyAlias);
        privateKey = (PrivateKey) keyStore.getKey(keyAlias, password);
        this.password = new String(password);
    }

    public MockDNIe(PrivateKey privateKey, X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        this.privateKey = privateKey;
    }

    public MockDNIe(String certURL, String certPassw) throws IOException, KeyStoreException, CertificateException,
            NoSuchAlgorithmException,
            UnrecoverableEntryException {
        URL res = Thread.currentThread().getContextClassLoader().getResource(certURL);
        this.keyStore = KeyStore.getInstance("JKS");
        keyStore.load(res.openStream(), certPassw.toCharArray());
        certificateChain = keyStore.getCertificateChain(Constants.USER_CERT_ALIAS);
        x509Certificate = (X509Certificate)keyStore.getCertificate(Constants.USER_CERT_ALIAS);
        privateKey = (PrivateKey) keyStore.getKey(Constants.USER_CERT_ALIAS, Constants.PASSW_DEMO.toCharArray());
    }

    public static JKSSignatureToken getJKSSignatureToken(String nif) throws IOException {
        URL res = Thread.currentThread().getContextClassLoader().getResource("certs/USER_" + nif + ".jks");
        return new JKSSignatureToken(res.openStream(), Constants.PASSW_DEMO);
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public List<X509Certificate> getX509CertificateChain() {
        List<X509Certificate> result = new ArrayList<>();
        for(Certificate cert : certificateChain) {
            result.add((X509Certificate)cert);
        }
        return result;
    }

    public Certificate[] getCertificateChain() {
        return certificateChain;
    }

    public MockDNIe setCertificateChain(Certificate[] certificateChain) {
        this.certificateChain = certificateChain;
        return this;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
