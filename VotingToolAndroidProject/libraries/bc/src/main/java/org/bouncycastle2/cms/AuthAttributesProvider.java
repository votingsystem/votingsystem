package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.ASN1Set;

interface AuthAttributesProvider
{
    ASN1Set getAuthAttributes();
}
