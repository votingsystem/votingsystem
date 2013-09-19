package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

public interface PasswordRecipient
    extends Recipient
{
    public static final int PKCS5_SCHEME2 = 0;
    public static final int PKCS5_SCHEME2_UTF8 = 1;

    RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] derivedKey, byte[] encryptedEncryptedContentKey)
        throws CMSException;

    int getPasswordConversionScheme();

    char[] getPassword();
}
