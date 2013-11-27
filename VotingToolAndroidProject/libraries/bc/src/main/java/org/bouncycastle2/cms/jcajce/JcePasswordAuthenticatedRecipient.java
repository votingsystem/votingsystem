package org.bouncycastle2.cms.jcajce;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.RecipientOperator;
import org.bouncycastle2.jcajce.io.MacOutputStream;
import org.bouncycastle2.operator.GenericKey;
import org.bouncycastle2.operator.MacCalculator;

import javax.crypto.Mac;
import java.io.OutputStream;
import java.security.Key;

public class JcePasswordAuthenticatedRecipient
    extends JcePasswordRecipient
{
    public JcePasswordAuthenticatedRecipient(char[] password)
    {
        super(password);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentMacAlgorithm, byte[] derivedKey, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        final Key secretKey = extractSecretKey(keyEncryptionAlgorithm, contentMacAlgorithm, derivedKey, encryptedContentEncryptionKey);

        final Mac dataMac = helper.createContentMac(secretKey, contentMacAlgorithm);

        return new RecipientOperator(new MacCalculator()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentMacAlgorithm;
            }

            public GenericKey getKey()
            {
                return new GenericKey(secretKey);
            }

            public OutputStream getOutputStream()
            {
                return new MacOutputStream(dataMac);
            }

            public byte[] getMac()
            {
                return dataMac.doFinal();
            }
        });
    }
}
