package org.bouncycastle2.cert.crmf.jcajce;

import org.bouncycastle2.asn1.crmf.CertReqMsg;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle2.cert.crmf.CRMFException;
import org.bouncycastle2.cert.crmf.CertificateRequestMessage;
import org.bouncycastle2.jcajce.DefaultJcaJceHelper;
import org.bouncycastle2.jcajce.NamedJcaJceHelper;
import org.bouncycastle2.jcajce.ProviderJcaJceHelper;

import javax.security.auth.x500.X500Principal;
import java.security.Provider;
import java.security.PublicKey;

public class JcaCertificateRequestMessage
    extends CertificateRequestMessage
{
    private CRMFHelper helper = new CRMFHelper(new DefaultJcaJceHelper());

    public JcaCertificateRequestMessage(CertificateRequestMessage certReqMsg)
    {
        this(certReqMsg.toASN1Structure());
    }

    public JcaCertificateRequestMessage(CertReqMsg certReqMsg)
    {
        super(certReqMsg);
    }

    public JcaCertificateRequestMessage setProvider(String providerName)
    {
        this.helper = new CRMFHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public JcaCertificateRequestMessage setProvider(Provider provider)
    {
        this.helper = new CRMFHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public X500Principal getSubjectX500Principal()
    {
        X500Name subject = this.getCertTemplate().getSubject();

        if (subject != null)
        {
            return new X500Principal(subject.getDEREncoded());
        }

        return null;
    }

    public PublicKey getPublicKey()
        throws CRMFException
    {
        SubjectPublicKeyInfo subjectPublicKeyInfo = getCertTemplate().getPublicKey();

        if (subjectPublicKeyInfo != null)
        {
            return helper.toPublicKey(subjectPublicKeyInfo);
        }

        return null;
    }
}
