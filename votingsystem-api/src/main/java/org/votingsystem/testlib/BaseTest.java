package org.votingsystem.testlib;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.votingsystem.crypto.KeyGenerator;
import org.votingsystem.http.HttpConn;
import org.votingsystem.util.Constants;

import java.security.Security;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BaseTest {

    private static final Logger log = Logger.getLogger(BaseTest.class.getName());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public BaseTest() {
        try {
            String logFile = System.getProperty("org.votingsystem.test.logging.file");
            if(logFile == null){
                LogManager.getLogManager().readConfiguration(
                        BaseTest.class.getClassLoader().getResourceAsStream("logging.properties"));
            }
            HttpConn.init(HttpConn.HTTPS_POLICY.ALL, null);

            //DSSXMLUtils.getSecureTransformerFactory();

            org.apache.xml.security.Init.init();
            KeyGenerator.INSTANCE.init(Constants.SIG_NAME, Constants.PROVIDER,
                    Constants.KEY_SIZE, Constants.ALGORITHM_RNG);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}