package org.votingsystem.test.misc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.votingsystem.crypto.KeyGenerator;
import org.votingsystem.crypto.KeyStoreUtils;
import org.votingsystem.test.ejb.EJBClient;
import org.votingsystem.util.Constants;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Logger;

public class KeyStoreGenerator {

    private static final Logger log =  Logger.getLogger(KeyStoreGenerator.class.getName());

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {
            KeyGenerator.INSTANCE.init(Constants.SIG_NAME, Constants.PROVIDER, Constants.KEY_SIZE, Constants.ALGORITHM_RNG);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        generateRootKeyStore();
    }

    private static void generateRootKeyStore() throws Exception {
        String passw = "local-demo";
        String givenName = "FAKE ROOT DNIe CA";
        LocalDateTime from = LocalDateTime.now();
        Date dateBegin = Date.from(from.atZone(ZoneId.systemDefault()).toInstant());
        Date dateFinish = Date.from(from.plusYears(2).withHour(0).withMinute(0).withSecond(0)
                .atZone(ZoneId.systemDefault()).toInstant());
        log.info("dateBegin: " + dateBegin + " - dateFinish: " + dateFinish);
        KeyStore keyStore = KeyStoreUtils.generateRootKeyStore(dateBegin, dateFinish, passw.toCharArray(),
                "appkey", givenName);
        byte[] keyStoreBytes = KeyStoreUtils.toByteArray(keyStore, passw.toCharArray());
        String outputFilePath = System.getProperty("user.home") + "/" + givenName + ".jks";
        FileUtils.copyBytesToFile(keyStoreBytes, new File(outputFilePath));
        log.info("KeyStore stored at: " + outputFilePath);
    }

}
