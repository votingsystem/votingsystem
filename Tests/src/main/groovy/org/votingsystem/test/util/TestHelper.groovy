package org.votingsystem.test.util

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.util.HttpHelper


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TestHelper {

    public static Logger init(Class clazz) {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("log4jTests.properties"),
                Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
        return Logger.getLogger(clazz);
    }

    public static VicketServer loadVicketServer() {
        VicketServer vicketServer = null
        if(ContextVS.getInstance().getVicketServer() == null) {
            String vicketServerURL = ContextVS.getInstance().config.vicketServerURL
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(vicketServerURL),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    vicketServer = (VicketServer) ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
                    ContextVS.getInstance().setVicketServer(vicketServer);
                } catch(Exception ex) {ex.printStackTrace();}
            }
        }
        return vicketServer
    }
}
