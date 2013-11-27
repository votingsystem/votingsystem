package org.bouncycastle2.crypto.tls;

import org.bouncycastle2.crypto.CryptoException;
import org.bouncycastle2.crypto.Signer;
import org.bouncycastle2.crypto.params.AsymmetricKeyParameter;

import java.security.SecureRandom;

interface TlsSigner
{
    byte[] calculateRawSignature(SecureRandom random, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException;

    Signer createVerifyer(AsymmetricKeyParameter publicKey);

    boolean isValidPublicKey(AsymmetricKeyParameter publicKey);
}
