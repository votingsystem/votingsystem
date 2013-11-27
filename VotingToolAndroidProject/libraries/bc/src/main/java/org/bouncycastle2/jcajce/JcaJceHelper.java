package org.bouncycastle2.jcajce;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.operator.AsymmetricKeyUnwrapper;
import org.bouncycastle2.operator.SymmetricKeyUnwrapper;

import javax.crypto.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public interface JcaJceHelper
{
    Cipher createCipher(
        String algorithm)
        throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException;

    Mac createMac(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    KeyAgreement createKeyAgreement(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    AlgorithmParameterGenerator createAlgorithmParameterGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    AlgorithmParameters createAlgorithmParameters(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    KeyGenerator createKeyGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    KeyFactory createKeyFactory(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    KeyPairGenerator createKeyPairGenerator(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    MessageDigest createDigest(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    Signature createSignature(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException;

    CertificateFactory createCertificateFactory(String algorithm)
        throws NoSuchAlgorithmException, NoSuchProviderException, CertificateException;

    AsymmetricKeyUnwrapper createAsymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, PrivateKey keyEncryptionKey);

    SymmetricKeyUnwrapper createSymmetricUnwrapper(AlgorithmIdentifier keyEncryptionAlgorithm, SecretKey keyEncryptionKey);
}
