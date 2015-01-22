package org.votingsystem.client.util;

import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;

import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinStatusChecker implements Runnable{

    private static Logger log = Logger.getLogger(CooinStatusChecker.class);

    public interface Listener {
        public void processCooinStatus(Cooin cooin, Integer statusCode);
    }

    private List<Cooin> cooinList;
    private Listener listener;

    public CooinStatusChecker(List<Cooin> cooinList, Listener listener) {
        this.cooinList = cooinList;
        this.listener = listener;
    }

    @Override public void run() {
        try {
            CooinServer cooinServer;
            ResponseVS responseVS;
            for(Cooin cooin: cooinList) {
                responseVS = Utils.checkServer(cooin.getCooinServerURL());
                cooinServer = (CooinServer) responseVS.getData();
                responseVS = HttpHelper.getInstance().getData(
                        cooinServer.getCooinStateServiceURL(cooin.getHashCertVS()), null);
                listener.processCooinStatus(cooin, responseVS.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
