package org.votingsystem.test.misc;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.TrustAnchor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SMIMEValidator {

    private static Logger log =  Logger.getLogger(SMIMEValidator.class.getName());

    public static void main(String[] args) throws Exception {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("smime_dnie.p7s");
        SMIMEMessage smimeMessage = new SMIMEMessage(FileUtils.getBytesFromStream(inputStream));
        Set<UserVS> signersVS = smimeMessage.getSigners();
        for(UserVS signer : signersVS) {
            log.log(Level.INFO, signer.getCertificate().getSubjectDN().toString());
            UserVS userVS1 = signer.getUserVS(signer.getCertificate().getSubjectDN().toString());


            final X509Principal principal = PrincipalUtil.getSubjectX509Principal(signer.getCertificate());
            log.info("===== " + principal.toString());

            final String nif = (String) principal.getValues(new DERObjectIdentifier("2.5.4.5")).get(0);
            log.info("===== " + nif);
            final String Surname = (String) principal.getValues(new DERObjectIdentifier("2.5.4.4")).get(0);
            log.info("===== " + Surname);
            final String name = (String) principal.getValues(new DERObjectIdentifier("2.5.4.42")).get(0);
            log.info("===== " + name);


            log.log(Level.INFO, signer.getCertificate().getSubjectDN().getName());
        }
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        InputStreamReader sReader = new InputStreamReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("./validationCerts"));
        BufferedReader bReader = new BufferedReader(sReader);
        bReader.lines().forEach(line -> {
            try {
                InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("./validationCerts/" + line);
                TrustAnchor trustAnchor = new TrustAnchor(CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(input)), null);
                log.info(trustAnchor.getTrustedCert().getIssuerDN().toString() + " - " + trustAnchor.getTrustedCert().getSerialNumber());
                trustAnchors.add(trustAnchor);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        for(UserVS userVS: signersVS) {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    trustAnchors, false, Arrays.asList(userVS.getCertificate()));
            //log.info(validatorResult.getResult().toString());
        }
        log.info("signature content: " + smimeMessage.getSignedContent());
    }

}
