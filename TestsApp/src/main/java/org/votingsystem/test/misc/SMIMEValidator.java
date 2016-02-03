package org.votingsystem.test.misc;

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
import java.util.logging.Level;
import java.util.logging.Logger;


public class SMIMEValidator {

    private static Logger log =  Logger.getLogger(SMIMEValidator.class.getName());

    public static void main(String[] args) throws Exception {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("smime_dnie.p7s");
        SMIMEMessage smimeMessage = new SMIMEMessage(FileUtils.getBytesFromStream(inputStream));
        Set<UserVS> signersVS = smimeMessage.getSigners();
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        InputStreamReader sReader = new InputStreamReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("./validationCerts"));
        BufferedReader bReader = new BufferedReader(sReader);
        bReader.lines().forEach(line -> {
            try {
                InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("./validationCerts/" + line);
                trustAnchors.add(new TrustAnchor(CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(input)), null));
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        for(UserVS userVS: signersVS) {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    trustAnchors, false, Arrays.asList(userVS.getCertificate()));
            log.info(validatorResult.getResult().toString());
        }
        log.info("signature content: " + smimeMessage.getSignedContent());
    }

}
