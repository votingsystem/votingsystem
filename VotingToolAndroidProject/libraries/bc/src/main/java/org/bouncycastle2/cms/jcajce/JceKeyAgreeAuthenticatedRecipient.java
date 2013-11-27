package org.bouncycastle2.cms.jcajce;

import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.RecipientOperator;
import org.bouncycastle2.jcajce.io.MacOutputStream;
import org.bouncycastle2.operator.GenericKey;
import org.bouncycastle2.operator.MacCalculator;

import javax.crypto.Mac;
import java.io.OutputStream;
import java.security.Key;
import java.security.PrivateKey;

public class JceKeyAgreeAuthenticatedRecipient
    extends JceKeyAgreeRecipient
{
    public JceKeyAgreeAuthenticatedRecipient(PrivateKey recipientKey)
    {
        super(recipientKey);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentMacAlgorithm, SubjectPublicKeyInfo senderPublicKey, ASN1OctetString userKeyingMaterial, byte[] encryptedContentKey)
        throws CMSException
    {
        final Key secretKey = extractSecretKey(keyEncryptionAlgorithm, contentMacAlgorithm, senderPublicKey, userKeyingMaterial, encryptedContentKey);

        final Mac dataMac = contentHelper.createContentMac(secretKey, contentMacAlgorithm);

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
