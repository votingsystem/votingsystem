package org.votingsystem.client.util;

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

    public enum CryptoToken {MOBILE, DNIe, JKS_KEYSTORE}

    public static SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, String textToSign,
             char[] password, String subject, Header header) throws Exception {
        String  cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoToken.DNIe.toString());
        logger.debug("genMimeMessage - CryptoToken: " + cryptoTokenStr);
        CryptoToken cryptoToken = CryptoToken.valueOf(cryptoTokenStr);
        switch(cryptoToken) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getUserKeyStore(password);
                java.security.cert.Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
                SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyStore,
                        ContextVS.KEYSTORE_USER_CERT_ALIAS, password, ContextVS.DNIe_SIGN_MECHANISM);
                return signedMailGenerator.genMimeMessage(fromUser, toUser, textToSign, subject, header);
            case DNIe:
                return DNIeContentSigner.genMimeMessage(fromUser,
                        toUser, textToSign, password, subject, header);
            case MOBILE:
                return null;
            default: return null;
        }
    }

    public static String getPasswordRequestMsg() {
        String  cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoToken.DNIe.toString());
        CryptoToken cryptoToken = CryptoToken.valueOf(cryptoTokenStr);
        switch(cryptoToken) {
            case MOBILE:
                return ContextVS.getMessage("passwordDialogMobileMsg");
            case JKS_KEYSTORE:
                return ContextVS.getMessage("passwordDialogKeyStoreMsg");
            case DNIe:
                return ContextVS.getMessage("passwordDialogDNIeMsg");
            default:
                return null;
        }
    }

}
