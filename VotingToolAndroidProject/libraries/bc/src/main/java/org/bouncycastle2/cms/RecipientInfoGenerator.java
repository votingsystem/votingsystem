package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.cms.RecipientInfo;
import org.bouncycastle2.operator.GenericKey;

public interface RecipientInfoGenerator
{
    RecipientInfo generate(GenericKey contentEncryptionKey)
        throws CMSException;
}
