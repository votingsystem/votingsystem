package org.bouncycastle2.cert.crmf;

import org.bouncycastle2.asn1.DERBitString;
import org.bouncycastle2.asn1.crmf.PKMACValue;
import org.bouncycastle2.asn1.crmf.POPOSigningKey;
import org.bouncycastle2.asn1.crmf.POPOSigningKeyInput;
import org.bouncycastle2.asn1.x509.GeneralName;
import org.bouncycastle2.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle2.operator.ContentSigner;

public class ProofOfPossessionSigningKeyBuilder
{
    private SubjectPublicKeyInfo pubKeyInfo;
    private GeneralName name;
    private PKMACValue publicKeyMAC;

    public ProofOfPossessionSigningKeyBuilder(SubjectPublicKeyInfo pubKeyInfo)
    {
        this.pubKeyInfo = pubKeyInfo;
    }

    public ProofOfPossessionSigningKeyBuilder setSender(GeneralName name)
    {
        this.name = name;

        return this;
    }

    public ProofOfPossessionSigningKeyBuilder setPublicKeyMac(PKMACValueGenerator generator, char[] password)
        throws CRMFException
    {
        this.publicKeyMAC = generator.generate(password, pubKeyInfo);

        return this;
    }

    public POPOSigningKey build(ContentSigner signer)
    {
        if (name != null && publicKeyMAC != null)
        {
            throw new IllegalStateException("name and publicKeyMAC cannot both be set.");
        }

        POPOSigningKeyInput popo;

        if (name != null)
        {
            popo = new POPOSigningKeyInput(name, pubKeyInfo);
        }
        else
        {
            popo = new POPOSigningKeyInput(publicKeyMAC, pubKeyInfo);
        }

        CRMFUtil.derEncodeToStream(popo, signer.getOutputStream());

        return new POPOSigningKey(popo, signer.getAlgorithmIdentifier(), new DERBitString(signer.getSignature()));
    }
}
