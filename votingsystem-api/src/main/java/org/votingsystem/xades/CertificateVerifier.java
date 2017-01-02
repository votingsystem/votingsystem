package org.votingsystem.xades;


import eu.europa.esig.dss.client.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.x509.CertificateSource;
import org.votingsystem.http.MediaType;
import org.votingsystem.ocsp.DNIeOCSPDataLoader;

public class CertificateVerifier {

    public static CommonCertificateVerifier create(final CertificateSource trustedCertSource) {
        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
        OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
        onlineOCSPSource.setDataLoader(new DNIeOCSPDataLoader(MediaType.OCSP_REQUEST));
        verifier.setOcspSource(onlineOCSPSource);
        verifier.setTrustedCertSource(trustedCertSource);
        return verifier;
    }

}
