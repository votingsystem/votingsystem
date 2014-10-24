package org.votingsystem.util;

import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.vicket.model.Vicket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.votingsystem.model.ContextVS.getMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletUtils {

    private static Logger log = Logger.getLogger(StringUtils.class);

    public static void saveVicketsToWallet(Collection<Vicket> vicketCollection, String walletPath) throws Exception {
        for(Vicket vicket : vicketCollection) {
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            new File(walletPath).mkdirs();
            String vicketPath = walletPath + UUID.randomUUID().toString() + ".servs";
            File vicketFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(vicketSerialized), new File(vicketPath));
            log.debug("Stored vicket: " + vicketFile.getAbsolutePath());
        }

    }

    public static List<Map> getSerializedVicketList(Collection<Vicket> vicketCollection) throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<>();
        for(Vicket vicket : vicketCollection) {
            Map vicketDataMap = vicket.getCertSubject().getDataMap();
            vicketDataMap.put("isTimeLimited", vicket.getIsTimeLimited());
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            vicketDataMap.put("object", new String(vicketSerialized, "UTF-8"));
            result.add(vicketDataMap);
        }
        return result;
    }

    public static JSON getWallet(String password) throws ExceptionVS {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        if(CryptoTokenVS.DNIe.toString().equals(tokenType)) throw new ExceptionVS(
                ContextVS.getMessage("dnieNotSupportedMsg"));
        File walletFile = null;
        try {
            walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.WALLET_FILE_NAME);
        } catch(Exception ex) {
            throw new ExceptionVS(ContextVS.getMessage("walletNotFoundErrorMsg"), ex);
        }
        try {
            KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS, password.toCharArray());
            byte[] walletBytes = Encryptor.decryptCMS(FileUtils.getBytesFromFile(walletFile), privateKey);
            return JSONSerializer.toJSON(new String(walletBytes, "UTF-8"));
        } catch(Exception ex) {
            throw new ExceptionVS(getMessage("cryptoTokenPasswordErrorMsg"), ex);
        }
    }

    public static void saveWallet(JSON walletJSON, String password) throws Exception {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        if(CryptoTokenVS.DNIe.toString().equals(tokenType)) throw new ExceptionVS(
                ContextVS.getMessage("dnieNotSupportedMsg"));
        File walletFile = new File(ContextVS.APPDIR + File.separator + ContextVS.WALLET_FILE_NAME);
        walletFile.createNewFile();
        try {
            KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
            X509Certificate x509UserCert = (X509Certificate) keyStore.getCertificate(ContextVS.KEYSTORE_USER_CERT_ALIAS);
            byte[] encryptedWallet = Encryptor.encryptToCMS(walletJSON.toString().getBytes(), x509UserCert);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(encryptedWallet), walletFile);
        } catch(Exception ex) {
            throw new ExceptionVS(getMessage("cryptoTokenPasswordErrorMsg"), ex);
        }
    }


}
