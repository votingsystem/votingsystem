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
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.WARNING);
        /*System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");*/
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
