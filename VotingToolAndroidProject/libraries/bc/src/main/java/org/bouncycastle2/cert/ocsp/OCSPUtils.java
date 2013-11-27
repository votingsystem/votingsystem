package org.bouncycastle2.cert.ocsp;

import org.bouncycastle2.asn1.DERGeneralizedTime;
import org.bouncycastle2.asn1.x509.X509Extensions;
import org.bouncycastle2.cert.X509CertificateHolder;

import java.util.*;

class OCSPUtils
{
    static final X509CertificateHolder[] EMPTY_CERTS = new X509CertificateHolder[0];

    static Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());
    static List EMPTY_LIST = Collections.unmodifiableList(new ArrayList());

    static Date extractDate(DERGeneralizedTime time)
    {
        try
        {
            return time.getDate();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("exception processing GeneralizedTime: " + e.getMessage());
        }
    }

    static Set getCriticalExtensionOIDs(X509Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getCriticalExtensionOIDs())));
    }

    static Set getNonCriticalExtensionOIDs(X509Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        // TODO: should probably produce a set that imposes correct ordering
        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getNonCriticalExtensionOIDs())));
    }

    static List getExtensionOIDs(X509Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_LIST;
        }

        return Collections.unmodifiableList(Arrays.asList(extensions.getExtensionOIDs()));
    }
}
