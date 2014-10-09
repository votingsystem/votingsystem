package org.votingsystem.util;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.vicket.model.Vicket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletUtils {

    private static Logger logger = Logger.getLogger(StringUtils.class);

    public static void saveVicketsToWallet(Collection<Vicket> vicketCollection, String walletPath) throws Exception {
        for(Vicket vicket : vicketCollection) {
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            new File(walletPath).mkdirs();
            String vicketPath = walletPath + UUID.randomUUID().toString() + ".servs";
            File vicketFile = FileUtils.copyStreamToFile(new ByteArrayInputStream(vicketSerialized), new File(vicketPath));
            logger.debug("Stored vicket: " + vicketFile.getAbsolutePath());
        }

    }
}
