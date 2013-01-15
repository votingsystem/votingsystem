package org.sistemavotacion.seguridad;

import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle.tsp.TimeStampToken;

public interface TimeStampWrapper {

    public TimeStampToken getTimeStampToken();
    public Attribute getTimeStampTokenAsAttribute();
    public AttributeTable getTimeStampTokenAsAttributeTable();
}
