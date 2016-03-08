package org.votingsystem.util.crypto;


import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Integer;

import java.math.BigInteger;

public class BasicConstraints extends org.bouncycastle.asn1.x509.BasicConstraints{

    private ASN1Boolean cA = ASN1Boolean.getInstance(false);
    private ASN1Integer pathLenConstraint = null;

    public BasicConstraints(boolean b) {
        super(b);
    }

    public BasicConstraints(boolean b, Integer pathLength) {
        super(b);
        this.cA = ASN1Boolean.getInstance(b);
        pathLenConstraint = new ASN1Integer(pathLength);
    }

    @Override
    public boolean isCA() {
        return this.cA != null && this.cA.isTrue();
    }

    @Override
    public BigInteger getPathLenConstraint() {
        return this.pathLenConstraint != null?this.pathLenConstraint.getValue():null;
    }
}
