package org.bouncycastle2.crypto.tls;

import java.security.SecureRandom;

import org.bouncycastle2.crypto.CryptoException;
import org.bouncycastle2.crypto.Signer;
import org.bouncycastle2.crypto.params.AsymmetricKeyParameter;

interface TlsSigner
{
    byte[] calculateRawSignature(SecureRandom random, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException;

    Signer createVerifyer(AsymmetricKeyParameter publicKey);

    boolean isValidPublicKey(AsymmetricKeyParameter publicKey);
}
