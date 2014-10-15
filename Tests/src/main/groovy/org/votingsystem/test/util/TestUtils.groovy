package org.votingsystem.test.util

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TestUtils {

    private static Logger log


    public static Logger init(Class clazz) {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("log4jTests.properties"),
                Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
        log =  Logger.getLogger(TestUtils.class);
        return Logger.getLogger(clazz);
    }

    public static VicketServer fetchVicketServer(String vicketServerURL) throws ExceptionVS {
        VicketServer vicketServer = null
        if(ContextVS.getInstance().getVicketServer() == null) {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(vicketServerURL),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    vicketServer = (VicketServer) ActorVS.parse(JSONSerializer.toJSON(responseVS.getMessage()));
                    ContextVS.getInstance().setVicketServer(vicketServer);
                } catch(Exception ex) { throw new ExceptionVS("Error fetching Vicket server: " + ex.getMessage(), ex);}
            } else throw new ExceptionVS("Error fetching Vicket server: " + responseVS.getMessage())
        }
        return vicketServer
    }

    public static File getFileFromResources(String resource) {
        InputStream input =  Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)
        byte[] fileBytes = FileUtils.getBytesFromInputStream(input)
        return FileUtils.getFileFromBytes(fileBytes)
    }

    public static JSONObject getGroupVSData(String groupVSURL) throws ExceptionVS {
        ResponseVS responseVS = HttpHelper.getInstance().getData(groupVSURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            JSONObject dataJSON = JSONSerializer.toJSON(responseVS.getMessage())
            JSONObject groupDataJSON =  dataJSON.getJSONObject("userVS")
            JSONObject representativeDataJSON = groupDataJSON.getJSONObject("representative")
            //{"operation":,"groupvs":{"id":4,"name":"NombreGrupo","representative":{"id":2,"nif":"07553172H"}}}
            JSONObject subscriptionData = new JSONObject([operation:"VICKET_GROUP_SUBSCRIBE"])
            JSONObject groupDataJSON1 = new JSONObject([id:groupDataJSON.getLong("id"), name:groupDataJSON.getString("name")])
            JSONObject representativeDataJSON1 = new JSONObject([id:representativeDataJSON.getLong("id"),
                                                                 nif:representativeDataJSON.getString("nif")])
            groupDataJSON1.put("representative", representativeDataJSON1)
            subscriptionData.put("groupvs", groupDataJSON1)
            return subscriptionData;
        } else throw new ExceptionVS(responseVS.getMessage())
    }
}
