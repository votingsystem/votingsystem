package org.votingsystem.test.ocsp;

import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

public class Test {

    //private static final String OCSP_SERVER = Constants.OCSP_DNIE_URL;
    private static final String OCSP_SERVER = "https://voting.ddns.net/idprovider/ocsp";

    //private static final String rootCertPath = "certs/dnie_raiz.pem";
    private static final String rootCertPath = "certs/fakeRootDNIe.pem";
    //private static final String certPath = "certs/dnie.pem";
    private static final String certPath = "certs/fake_08888888D.pem";

    private static X509Certificate cert;
    private static X509Certificate intermediateCert;

    public static void main(String[] args) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        byte[] intermediateCertBytes = FileUtils.getBytesFromStream(
                Thread.currentThread().getContextClassLoader().getResource(rootCertPath).openStream());
        byte[] certBytes = FileUtils.getBytesFromStream(
                Thread.currentThread().getContextClassLoader().getResource(certPath).openStream());
        cert = PEMUtils.fromPEMToX509Cert(certBytes);
        intermediateCert = PEMUtils.fromPEMToX509Cert(intermediateCertBytes);
        //checkOCSPWithHttpURLConnection();
        checkOCSPWithVotingSystemAPI(OCSP_SERVER);
        System.exit(0);
    }

    private static void checkOCSPWithHttpURLConnection() throws Exception {
        System.out.println("checkOCSPWithHttpURLConnection - certState: " + org.votingsystem.ocsp.OCSPUtils.validateCert(
                new X509CertificateHolder(intermediateCert.getEncoded()),
                cert.getSerialNumber(), new Date()));
    }

    private static void checkOCSPWithVotingSystemAPI(String ocspServer) throws Exception {
        HttpConn.init(HttpConn.HTTPS_POLICY.ALL, null);
        X509CertificateHolder intermediateCertHolder = new X509CertificateHolder(intermediateCert.getEncoded());
        DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        CertificateID id = new CertificateID(digCalcProv.get(CertificateID.HASH_SHA1), intermediateCertHolder,
                cert.getSerialNumber());
        OCSPReqBuilder gen = new OCSPReqBuilder();
        gen.addRequest(id);
        OCSPReq ocspReq = gen.build();
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(ocspReq.getEncoded(),
                "application/ocsp-request", ocspServer);
        OCSPResp ocspResponse = new OCSPResp(new ByteArrayInputStream(responseDto.getMessageBytes()));
        BasicOCSPResp basicOCSPResp ;
        String certificateState = null;
        if(ocspResponse.getStatus() == OCSPResponseStatus.SUCCESSFUL) {
            certificateState = null;
            basicOCSPResp = (BasicOCSPResp) ocspResponse.getResponseObject();
            for(SingleResp singleResponse : basicOCSPResp.getResponses()){
                Object stat = singleResponse.getCertStatus();
                if (stat == CertificateStatus.GOOD) {
                    certificateState = "GOOD";
                } else if (stat instanceof RevokedStatus) {
                    Date revocationDate = ((RevokedStatus)stat).getRevocationTime();
                    CRLReason crlReason = CRLReason.lookup(((RevokedStatus)stat).getRevocationReason());
                    System.out.println("RevokedStatus - revocationDate: " + revocationDate + " - crlReason: " + crlReason.toString());
                    certificateState = "REVOKED";
                } else if (stat instanceof UnknownStatus) {
                    certificateState = "UNKNOWN";
                }
            }
        }
        System.out.println("certificateState: " + certificateState);
    }

}