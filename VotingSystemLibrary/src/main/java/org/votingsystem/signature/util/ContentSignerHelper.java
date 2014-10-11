package org.votingsystem.signature.util;

import iaik.pkcs.pkcs11.Mechanism;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.dnie.DNIePDFContentSigner;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;

import javax.mail.Header;
import java.security.KeyStore;
import java.security.PrivateKey;

import static org.votingsystem.model.ContextVS.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContentSignerHelper {

    private static Logger log = Logger.getLogger(ContentSignerHelper.class);

    public enum CryptoToken {DNIe, JKS_KEYSTORE}

    public static SMIMEMessage genMimeMessage(String fromUser, String toUser, String textToSign,
             char[] password, String subject, Header... headers) throws Exception {
        String  cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoToken.DNIe.toString());
        log.debug("genMimeMessage - CryptoToken: " + cryptoTokenStr);
        CryptoToken cryptoToken = CryptoToken.valueOf(cryptoTokenStr);
        switch(cryptoToken) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getUserKeyStore(password);
                java.security.cert.Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
                SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyStore,
                        ContextVS.KEYSTORE_USER_CERT_ALIAS, password, ContextVS.DNIe_SIGN_MECHANISM);
                return signedMailGenerator.genMimeMessage(fromUser, toUser, textToSign, subject, headers);
            case DNIe:
                return DNIeContentSigner.genMimeMessage(fromUser,
                        toUser, textToSign, password, subject, headers);
            default: return null;
        }
    }


    public static ContentSignerVS getContentSignerPDF(char[] password, Mechanism signatureMechanism) throws Exception {
        String  cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoToken.DNIe.toString());
        log.debug("signPDF - CryptoToken: " + cryptoTokenStr);
        CryptoToken cryptoToken = CryptoToken.valueOf(cryptoTokenStr);
        switch(cryptoToken) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getUserKeyStore(password);
                java.security.cert.Certificate[] signerCertChain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
                PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS, password);
                return new PDFContentSigner( signerPrivatekey, signerCertChain,
                        PDF_SIGNATURE_MECHANISM, PDF_SIGNATURE_DIGEST, PDF_DIGEST_OID);
            case DNIe:
                return DNIePDFContentSigner.getInstance(password, ContextVS.DNIe_SESSION_MECHANISM);
            default: return null;
        }
    }

    public static String getPasswordRequestMsg() {
        String  cryptoTokenStr = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoToken.DNIe.toString());
        CryptoToken cryptoToken = CryptoToken.valueOf(cryptoTokenStr);
        switch(cryptoToken) {
            case JKS_KEYSTORE:
                return ContextVS.getMessage("passwordDialogKeyStoreMsg");
            case DNIe:
                return ContextVS.getMessage("passwordDialogDNIeMsg");
            default:
                return null;
        }
    }

}
