package org.bouncycastle2.asn1.crmf;

import org.bouncycastle2.asn1.ASN1Encodable;
import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1Sequence;
import org.bouncycastle2.asn1.ASN1TaggedObject;
import org.bouncycastle2.asn1.DERBitString;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERSequence;
import org.bouncycastle2.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle2.asn1.cmp.PBMParameter;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

/**
 * Password-based MAC value for use with POPOSigningKeyInput.
 */
public class PKMACValue
    extends ASN1Encodable
{
    private AlgorithmIdentifier  algId;
    private DERBitString        value;

    private PKMACValue(ASN1Sequence seq)
    {
        algId = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        value = DERBitString.getInstance(seq.getObjectAt(1));
    }

    public static PKMACValue getInstance(Object o)
    {
        if (o instanceof PKMACValue)
        {
            return (PKMACValue)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new PKMACValue((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public static PKMACValue getInstance(ASN1TaggedObject obj, boolean isExplicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, isExplicit));
    }

    /**
     * Creates a new PKMACValue.
     * @param params parameters for password-based MAC
     * @param value MAC of the DER-encoded SubjectPublicKeyInfo
     */
    public PKMACValue(
        PBMParameter params,
        DERBitString value)
    {
        this(new AlgorithmIdentifier(
                    CMPObjectIdentifiers.passwordBasedMac, params), value);
    }

    /**
     * Creates a new PKMACValue.
     * @param aid CMPObjectIdentifiers.passwordBasedMAC, with PBMParameter
     * @param value MAC of the DER-encoded SubjectPublicKeyInfo
     */
    public PKMACValue(
        AlgorithmIdentifier aid,
        DERBitString value)
    {
        this.algId = aid;
        this.value = value;
    }

    public AlgorithmIdentifier getAlgId()
    {
        return algId;
    }

    public DERBitString getValue()
    {
        return value;
    }

    /**
     * <pre>
     * PKMACValue ::= SEQUENCE {
     *      algId  AlgorithmIdentifier,
     *      -- algorithm value shall be PasswordBasedMac 1.2.840.113533.7.66.13
     *      -- parameter value is PBMParameter
     *      value  BIT STRING }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(algId);
        v.add(value);

        return new DERSequence(v);
    }
}
