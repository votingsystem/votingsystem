package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DEROctetString;
import org.bouncycastle2.asn1.cms.KEKIdentifier;
import org.bouncycastle2.asn1.cms.KEKRecipientInfo;
import org.bouncycastle2.asn1.cms.RecipientInfo;
import org.bouncycastle2.operator.GenericKey;
import org.bouncycastle2.operator.OperatorException;
import org.bouncycastle2.operator.SymmetricKeyWrapper;

public abstract class KEKRecipientInfoGenerator
    implements RecipientInfoGenerator
{
    private final KEKIdentifier kekIdentifier;

    protected final SymmetricKeyWrapper wrapper;

    protected KEKRecipientInfoGenerator(KEKIdentifier kekIdentifier, SymmetricKeyWrapper wrapper)
    {
        this.kekIdentifier = kekIdentifier;
        this.wrapper = wrapper;
    }

    public final RecipientInfo generate(GenericKey contentEncryptionKey)
        throws CMSException
    {
        try
        {
            ASN1OctetString encryptedKey = new DEROctetString(wrapper.generateWrappedKey(contentEncryptionKey));

            return new RecipientInfo(new KEKRecipientInfo(kekIdentifier, wrapper.getAlgorithmIdentifier(), encryptedKey));
        }
        catch (OperatorException e)
        {
            throw new CMSException("exception wrapping content key: " + e.getMessage(), e);
        }
    }
}