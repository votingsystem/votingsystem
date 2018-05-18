package org.votingsystem.test.xades;

import eu.europa.esig.dss.client.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import eu.europa.esig.dss.validation.reports.wrapper.DiagnosticData;
import eu.europa.esig.dss.x509.CertificateToken;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.http.MediaType;
import org.votingsystem.ocsp.DNIeOCSPDataLoader;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.FileUtils;

import java.security.cert.X509Certificate;

public class CertificateValidationTest extends BaseTest {

    public static void main(String[] args) throws Exception {
        new CertificateValidationTest().validateCertificate();
    }

    private void validateCertificate() throws Exception {
        X509Certificate x509Certificate = PEMUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(Thread.currentThread().getContextClassLoader()
                .getResource("certs/fake_08888888.cer").openStream()));
        CertificateToken token = new CertificateToken(x509Certificate);
        CertificateVerifier cv = new CommonCertificateVerifier();
        // We can inject several sources. eg: OCSP, CRL, AIA, trusted lists
        // Capability to download resources from AIA
        //cv.setDataLoader(new CommonsDataLoader());
        // Capability to request OCSP Responders

        OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
        onlineOCSPSource.setDataLoader(new DNIeOCSPDataLoader(MediaType.OCSP_REQUEST));
        //cv.setOcspSource(new OnlineOCSPSource());
        cv.setOcspSource(onlineOCSPSource);

        // Capability to download CRL
        //cv.setCrlSource(new OnlineCRLSource());
        // We now add trust anchors (trusted list, keystore,...)
        //cv.setTrustedCertSource(trustedCertSource);
        // We also can add missing certificates
        //cv.setAdjunctCertSource(adjunctCertSource);
        CertificateValidator validator = CertificateValidator.fromCertificate(token);
        validator.setCertificateVerifier(cv);
        CertificateReports certificateReports = validator.validate();
        // We have 3 reports
        // The diagnostic data which contains all used and static data
        DiagnosticData diagnosticData = certificateReports.getDiagnosticData();
        // The detailed report which is the result of the process of the diagnostic data and the validation policy
        //DetailedReport detailedReport = certificateReports.getDetailedReport();

        // The simple report is a summary of the detailed report or diagnostic data (more user-friendly)
        //SimpleCertificateReport simpleReport = certificateReports.getSimpleReport();
    }

}