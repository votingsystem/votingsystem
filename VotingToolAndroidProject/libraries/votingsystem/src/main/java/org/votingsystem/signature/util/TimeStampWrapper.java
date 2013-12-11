package org.votingsystem.signature.util;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;

public interface TimeStampWrapper {

    public TimeStampToken getTimeStampToken();
    public Attribute getTimeStampTokenAsAttribute();
    public AttributeTable getTimeStampTokenAsAttributeTable();
}
