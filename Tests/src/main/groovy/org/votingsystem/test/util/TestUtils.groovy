package org.votingsystem.test.util

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.test.model.SimulationData
import org.votingsystem.test.ui.TestsApp
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper

import java.util.concurrent.ConcurrentHashMap

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TestUtils {

    private static Logger log

    private static ConcurrentHashMap<Long, UserVS> userVSMap = new ConcurrentHashMap<>();
    private static SimulationData simulationData;
    private static Class initClass;

    public static Logger init(Class clazz) {
        initClass = clazz;
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("log4jTests.properties"),
                Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"),"TestDir");
        log =  Logger.getLogger(TestUtils.class);
        return Logger.getLogger(clazz);
    }

    public static Logger init(Class clazz, Map simulationDataMap) {
        Logger log = init(clazz)
        simulationData = SimulationData.parse(JSONSerializer.toJSON(simulationDataMap))
        simulationData.init(System.currentTimeMillis());
        return log;
    }

    public static Logger initWithUI(Class clazz, Map simulationDataMap) {
        Logger log = init(clazz)
        new Thread(new Runnable() {  @Override void run() { TestsApp.getInstance().init(simulationData)  } }).start()
        simulationData = SimulationData.parse(JSONSerializer.toJSON(simulationDataMap))
        simulationData.init(System.currentTimeMillis());
        return log;
    }

    public static Logger init(Class clazz, SimulationData simulationData) {
        Logger log = init(clazz)
        this.simulationData = simulationData;
        this.simulationData.init(System.currentTimeMillis());
        return log;
    }

    public static Map<String, MockDNI> getUserVSMap(List<MockDNI> userList) {
        Map<String, MockDNI> result = new HashMap<>();
        for(MockDNI mockDNI:userList) {
            result.put(mockDNI.getNif(), mockDNI);
        }
        return result
    }

    public static SimulationData getSimulationData() {return simulationData;}

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

    public static UserVS getUserVS(Long userId, ActorVS server) throws ExceptionVS {
        if(userVSMap.get(userId) != null) return userVSMap.get(userId);
        ResponseVS responseVS = HttpHelper.getInstance().getData(server.getUserVSURL(userId), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            JSONObject dataJSON = JSONSerializer.toJSON(responseVS.getMessage())
            UserVS userVS = UserVS.parse(dataJSON.getJSONObject("userVS"));
            userVSMap.put(userId, userVS);
            return userVS
        } else throw new ExceptionVS(responseVS.getMessage())
    }

    public static JSONObject getGroupVSSubscriptionData(String groupVSURL) throws ExceptionVS {
        ResponseVS responseVS = HttpHelper.getInstance().getData(groupVSURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            JSONObject groupDataJSON = JSONSerializer.toJSON(responseVS.getMessage())
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

    public static void finish(String resultMessage) {
        simulationData.finish(ResponseVS.SC_OK, System.currentTimeMillis());
        log.debug("------------------------------------------------");
        log.debug("--- ${initClass.getSimpleName()} finished ---");
        log.debug("Begin: ${DateUtils.getDateStr(simulationData.getBeginDate())} - Duration: ${simulationData.getDurationStr()}")
        if(resultMessage) log.debug(resultMessage)
        System.exit(0)
    }

    public static void finishWithError(String errorType, String resultMessage, Long numRequestCompleted) {
        finish(errorType + ": " + resultMessage + "\n" + " - Num. requests completed: " + numRequestCompleted)
    }
}
