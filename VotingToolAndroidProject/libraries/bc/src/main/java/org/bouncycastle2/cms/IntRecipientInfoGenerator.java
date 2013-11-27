package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.cms.RecipientInfo;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

interface IntRecipientInfoGenerator
{
    /**
     * Generate a RecipientInfo object for the given key.
     * @param contentEncryptionKey the <code>SecretKey</code> to encrypt
     * @param random a source of randomness
     * @param prov the default provider to use
     * @return a <code>RecipientInfo</code> object for the given key
     * @throws GeneralSecurityException
     */
    RecipientInfo generate(SecretKey contentEncryptionKey, SecureRandom random,
        Provider prov) throws GeneralSecurityException;
}
