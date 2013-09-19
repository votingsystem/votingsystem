package org.bouncycastle2.asn1.smime;

import org.bouncycastle2.asn1.DERSequence;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.Attribute;

public class SMIMECapabilitiesAttribute
    extends Attribute
{
    public SMIMECapabilitiesAttribute(
        SMIMECapabilityVector capabilities)
    {
        super(SMIMEAttributes.smimeCapabilities,
                new DERSet(new DERSequence(capabilities.toASN1EncodableVector())));
    }
}
