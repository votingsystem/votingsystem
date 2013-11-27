package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.*;
import org.bouncycastle2.asn1.cms.KEKIdentifier;
import org.bouncycastle2.asn1.cms.KEKRecipientInfo;
import org.bouncycastle2.asn1.cms.RecipientInfo;
import org.bouncycastle2.asn1.kisa.KISAObjectIdentifiers;
import org.bouncycastle2.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle2.asn1.ntt.NTTObjectIdentifiers;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

class KEKIntRecipientInfoGenerator
    implements IntRecipientInfoGenerator
{
    private SecretKey keyEncryptionKey;
    private KEKIdentifier kekIdentifier;

    // Derived
    private AlgorithmIdentifier keyEncryptionAlgorithm;

    KEKIntRecipientInfoGenerator()
    {
    }

    void setKeyEncryptionKey(SecretKey keyEncryptionKey)
    {
        this.keyEncryptionKey = keyEncryptionKey;
        this.keyEncryptionAlgorithm = determineKeyEncAlg(keyEncryptionKey);
    }

    void setKEKIdentifier(KEKIdentifier kekIdentifier)
    {
        this.kekIdentifier = kekIdentifier;
    }

    public RecipientInfo generate(SecretKey contentEncryptionKey, SecureRandom random,
            Provider prov) throws GeneralSecurityException
    {
        Cipher keyEncryptionCipher = CMSEnvelopedHelper.INSTANCE.createSymmetricCipher(
            keyEncryptionAlgorithm.getObjectId().getId(), prov);
        keyEncryptionCipher.init(Cipher.WRAP_MODE, keyEncryptionKey, random);
        byte[] encryptedKeyBytes = keyEncryptionCipher.wrap(contentEncryptionKey);
        ASN1OctetString encryptedKey = new DEROctetString(encryptedKeyBytes);

        return new RecipientInfo(new KEKRecipientInfo(kekIdentifier, keyEncryptionAlgorithm, encryptedKey));
    }

    private static AlgorithmIdentifier determineKeyEncAlg(SecretKey key)
    {
        String algorithm = key.getAlgorithm();

        if (algorithm.startsWith("DES"))
        {
            return new AlgorithmIdentifier(new DERObjectIdentifier(
                    "1.2.840.113549.1.9.16.3.6"), new DERNull());
        }
        else if (algorithm.startsWith("RC2"))
        {
            return new AlgorithmIdentifier(new DERObjectIdentifier(
                    "1.2.840.113549.1.9.16.3.7"), new DERInteger(58));
        }
        else if (algorithm.startsWith("AES"))
        {
            int length = key.getEncoded().length * 8;
            DERObjectIdentifier wrapOid;

            if (length == 128)
            {
                wrapOid = NISTObjectIdentifiers.id_aes128_wrap;
            }
            else if (length == 192)
            {
                wrapOid = NISTObjectIdentifiers.id_aes192_wrap;
            }
            else if (length == 256)
            {
                wrapOid = NISTObjectIdentifiers.id_aes256_wrap;
            }
            else
            {
                throw new IllegalArgumentException("illegal keysize in AES");
            }

            return new AlgorithmIdentifier(wrapOid); // parameters absent
        }
        else if (algorithm.startsWith("SEED"))
        {
            // parameters absent
            return new AlgorithmIdentifier(
                    KISAObjectIdentifiers.id_npki_app_cmsSeed_wrap);
        }
        else if (algorithm.startsWith("Camellia"))
        {
            int length = key.getEncoded().length * 8;
            DERObjectIdentifier wrapOid;

            if (length == 128)
            {
                wrapOid = NTTObjectIdentifiers.id_camellia128_wrap;
            }
            else if (length == 192)
            {
                wrapOid = NTTObjectIdentifiers.id_camellia192_wrap;
            }
            else if (length == 256)
            {
                wrapOid = NTTObjectIdentifiers.id_camellia256_wrap;
            }
            else
            {
                throw new IllegalArgumentException(
                        "illegal keysize in Camellia");
            }

            return new AlgorithmIdentifier(wrapOid); // parameters must be
                                                     // absent
        }
        else
        {
            throw new IllegalArgumentException("unknown algorithm");
        }
    }
}
