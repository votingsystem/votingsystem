package org.votingsystem.admintool.util;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;

import javax.mail.Header;
import java.security.KeyStore;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SMIMEContentSigner {

    private static Logger logger = Logger.getLogger(SMIMEContentSigner.class);

    public static SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, String textToSign,
             char[] password, String subject, Header header) throws Exception {
        Boolean withKeystore = ContextVS.getInstance().getBoolProperty(ContextVS.WITH_KEYSTORE_PROPERTY, false);
        if(withKeystore) {
            logger.debug("genMimeMessage - Signing with keystore");
            KeyStore keyStore = ContextVS.getUserKeyStore(password);
            java.security.cert.Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyStore,
                    ContextVS.KEYSTORE_USER_CERT_ALIAS, password, ContextVS.DNIe_SIGN_MECHANISM);
            return signedMailGenerator.genMimeMessage(fromUser, toUser, textToSign, subject, header);
        } else {
            logger.debug("genMimeMessage - Signing with DNIe");
            return DNIeContentSigner.genMimeMessage(fromUser,
                    toUser, textToSign, password, subject, header);
        }
    }
}
