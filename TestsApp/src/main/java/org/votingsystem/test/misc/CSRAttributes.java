package org.votingsystem.test.misc;

import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.CertificationRequestVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.logging.Logger;


public class CSRAttributes {

    private final static Logger log = Logger.getLogger(CSRAttributes.class.getName());

    public static final String csrRequest = "-----BEGIN CERTIFICATE REQUEST-----\n" +
            "MIICSzCCAbQCAQAwOzEPMA0GA1UEBBMGR0FSQ0lBMRQwEgYDVQQqEwtKT1NFIEpB\n" +
            "VklFUjESMBAGA1UEBRMJMDc1NTMxNzJIMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n" +
            "iQKBgQDUF1Ij4qeIj8MluCdKe2ZLb1CLhTkM1gfkPsZNq4xmKbt/dsGGDakkeUkd\n" +
            "8uis06fGPnML1KFyBDAiDnpWy7XazqjzRoxcAOUCeY9Huz71f8ru6C1Zy16IvkVk\n" +
            "g2BeNdHzh2K9PE0ajGkDbBsdOb/kWY3nHXSbDAXjua476fP+mwIDAQABoIHPMIHM\n" +
            "BgkAAAAAAAAAAAQxgb4Mgbt7ImRldmljZUlkIjoiMzU1MTM2MDU2OTkwMTQ5Iiwi\n" +
            "ZGV2aWNlTmFtZSI6IkxHRSBOZXh1cyA0IiwiZGV2aWNlVHlwZSI6Ik1PQklMRSIs\n" +
            "ImVtYWlsIjoiamd6b3Jub3phQGdnLmIiLCJnaXZlbm5hbWUiOiJKT1NFIEpBVklF\n" +
            "UiIsIm1vYmlsZVBob25lIjoiNjA5IiwibmlmIjoiMDc1NTMxNzJIIiwic3VybmFt\n" +
            "ZSI6IkdBUkNJQSJ9MA0GCSqGSIb3DQEBCwUAA4GBABp4TN+cC5VDhcUr6pclD5Tl\n" +
            "tJ9BUvByYS6lL6iPUGFJsPsnzUgUThG9reeC6Rb4ux23xSCRhD1tZSj+zXjA8g8N\n" +
            "X6peigr5+K4/DNG/Ec0Rnz4FQklrXubsYDHu/OydyfMHIboA4rq3f7JHzyO/UH69\n" +
            "UVQt1nOlsx3rkVcQXMeD\n" +
            "-----END CERTIFICATE REQUEST-----";

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrRequest.getBytes());
        CertExtensionDto dto = CertUtils.getCertExtensionData(CertExtensionDto.class, csr, ContextVS.DEVICE_OID);
        log.info("dto: " + dto.toString());
        PublicKey publicKey = CertUtils.getPublicKey(csr);
        SubjectKeyIdentifier subjectKeyIdentifier = new BcX509ExtensionUtils().createSubjectKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
        log.info("publicKey: " + subjectKeyIdentifier.toString());
    }

    public static void testCurrencyCSR() throws Exception {
        CertificationRequestVS certificationRequest = CertificationRequestVS.getCurrencyRequest(
                ContextVS.SIGNATURE_ALGORITHM, ContextVS.PROVIDER,
                "currencyServerURL", "hashCertVS", new BigDecimal(10), "EUR", false, "WILDTAG");
        byte[] csrPEM = certificationRequest.getCsrPEM();
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrPEM);
        CurrencyCertExtensionDto dto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class, csr, ContextVS.CURRENCY_OID);
        log.info("currency: " + dto.getHashCertVS());
    }

}