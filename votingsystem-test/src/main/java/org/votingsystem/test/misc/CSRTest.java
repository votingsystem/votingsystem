package org.votingsystem.test.misc;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.crypto.*;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CSRTest extends BaseTest {

    private static final Logger log = Logger.getLogger(CSRTest.class.getName());

    private static String ADMIN_KEYSTORE = "certs/votingsystem-idprovider.jks";
    private static String ADMIN_KEYSTORE_PASSWORD = Constants.PASSW_DEMO;


    public static void main(String[] args) throws Exception {
        new CSRTest().signCSRVote();
        System.exit(0);
    }

    public CSRTest() {
        super();
    }

    public void signCSRVote() throws Exception {
        MockDNIe adminCert = new MockDNIe(org.votingsystem.test.Constants.ADMIN_KEYSTORE, org.votingsystem.test.Constants.ADMIN_KEYSTORE_PASSWORD);
        String electionUUID = UUID.randomUUID().toString();
        String originRevocationHash = UUID.randomUUID().toString();
        String revocationHash = HashUtils.getHashBase64(originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        CertificationRequest certificationRequest = CertificationRequest.getVoteRequest(
                org.votingsystem.test.Constants.ID_PROVIDER_ENTITY_ID,
                org.votingsystem.test.Constants.VOTING_SERVICE_ENTITY_ID, electionUUID, revocationHash);

        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(certificationRequest.getCsrPEM());

        X509Certificate voteCert = CertificateUtils.signCSR(csr, org.votingsystem.test.Constants.ID_PROVIDER_ENTITY_ID,
                adminCert.getPrivateKey(), adminCert.getX509Certificate(), LocalDateTime.now(), LocalDateTime.now(),
                org.votingsystem.test.Constants.OCSP_SERVER_URL);
        log.info("voteCert: " + voteCert);
        FileUtils.copyBytesToFile(PEMUtils.getPEMEncoded(voteCert), new File("voteCert.crt"));
    }

}