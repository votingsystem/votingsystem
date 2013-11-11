package org.bouncycastle2.cert.cmp;

import org.bouncycastle2.asn1.cmp.CertConfirmContent;
import org.bouncycastle2.asn1.cmp.CertStatus;
import org.bouncycastle2.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle2.operator.DigestAlgorithmIdentifierFinder;

public class CertificateConfirmationContent
{
    private DigestAlgorithmIdentifierFinder digestAlgFinder;
    private CertConfirmContent content;

    public CertificateConfirmationContent(CertConfirmContent content)
    {
        this(content, new DefaultDigestAlgorithmIdentifierFinder());
    }

    public CertificateConfirmationContent(CertConfirmContent content, DigestAlgorithmIdentifierFinder digestAlgFinder)
    {
        this.digestAlgFinder = digestAlgFinder;
        this.content = content;
    }

    public CertConfirmContent toASN1Structure()
    {
        return content;
    }

    public CertificateStatus[] getStatusMessages()
    {
        CertStatus[] statusArray = content.toCertStatusArray();
        CertificateStatus[] ret = new CertificateStatus[statusArray.length];

        for (int i = 0; i != ret.length; i++)
        {
            ret[i] = new CertificateStatus(digestAlgFinder, statusArray[i]);
        }

        return ret;
    }
}
