package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

public interface KeyTransRecipient
    extends Recipient
{
    RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncAlg, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentKey)
        throws CMSException;
}
