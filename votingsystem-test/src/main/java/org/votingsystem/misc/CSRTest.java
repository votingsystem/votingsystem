package org.votingsystem.misc;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.BaseTest;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HashUtils;

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
    private static String ADMIN_KEYSTORE_PASSWORD = org.votingsystem.util.Constants.PASSW_DEMO;


    public static void main(String[] args) throws Exception {
        new CSRTest().signCSRVote();
        System.exit(0);
    }

    public CSRTest() {
        super();
    }

    public void signCSRVote() throws Exception {
        MockDNIe adminCert = new MockDNIe(org.votingsystem.Constants.ADMIN_KEYSTORE, org.votingsystem.Constants.ADMIN_KEYSTORE_PASSWORD);
        String electionUUID = UUID.randomUUID().toString();
        String originRevocationHash = UUID.randomUUID().toString();
        String revocationHashBase64 = HashUtils.getHashBase64(originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        CertificationRequest certificationRequest = CertificationRequest.getVoteRequest(
                org.votingsystem.Constants.ID_PROVIDER_SERVICE_ENTITY_ID,
                org.votingsystem.Constants.VOTING_SERVICE_SERVICE_ENTITY_ID, electionUUID, revocationHashBase64);

        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(certificationRequest.getCsrPEM());


        X509Certificate voteCert = CertUtils.signCSR(csr, org.votingsystem.Constants.ID_PROVIDER_SERVICE_ENTITY_ID, adminCert.getPrivateKey(),
                adminCert.getX509Certificate(), LocalDateTime.now(), LocalDateTime.now(),
                org.votingsystem.Constants.OCSP_SERVER_URL);

        log.info("voteCert: " + voteCert);


        FileUtils.copyBytesToFile(PEMUtils.getPEMEncoded(voteCert), new File("/home/jgzornoza/temp/voteCert.crt"));

    }
}
