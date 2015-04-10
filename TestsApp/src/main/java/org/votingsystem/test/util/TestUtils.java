package org.votingsystem.test.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestUtils {

    private static Logger log;

    private static ConcurrentHashMap<Long, UserVS> userVSMap = new ConcurrentHashMap<>();
    private static SimulationData simulationData;
    private static Class initClass;

    public static Logger init(Class clazz) throws Exception {
        init(clazz, "./TestDir");
        return log;
    }

    public static Logger init(Class clazz, String testDir) throws Exception {
        initClass = clazz;
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), testDir);
        log =  Logger.getLogger(TestUtils.class.getSimpleName());
        return Logger.getLogger(clazz.getSimpleName());
    }


    public static Logger init(Class clazz, SimulationData simulationData) throws Exception {
        Logger log = init(clazz);
        simulationData.init(System.currentTimeMillis());
        return log;
    }

    public static Map<String, MockDNI> getUserVSMap(List<MockDNI> userList) {
        Map<String, MockDNI> result = new HashMap<>();
        for(MockDNI mockDNI:userList) {
            result.put(mockDNI.getNif(), mockDNI);
        }
        return result;
    }

    public static SimulationData getSimulationData() {return simulationData;}

    public static CurrencyServer fetchCurrencyServer(String currencyServerURL) throws ExceptionVS {
        CurrencyServer currencyServer = null;
        if(ContextVS.getInstance().getCurrencyServer() == null) {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(currencyServerURL),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    currencyServer = (CurrencyServer) ((ActorVSDto)responseVS.getDto(ActorVSDto.class)).getActorVS();
                    ContextVS.getInstance().setCurrencyServer(currencyServer);
                } catch(Exception ex) { throw new ExceptionVS("Error fetching Currency server: " + ex.getMessage(), ex);}
            } else throw new ExceptionVS("Error fetching Currency server: " + responseVS.getMessage());
        }
        return currencyServer;
    }

    public static File getFileFromResources(String resource) throws IOException {
        InputStream input =  Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        byte[] fileBytes = FileUtils.getBytesFromStream(input);
        return FileUtils.getFileFromBytes(fileBytes);
    }

    public static UserVS getUserVS(Long userId, ActorVS server) throws Exception {
        if(userVSMap.get(userId) != null) return userVSMap.get(userId);
        ResponseVS responseVS = HttpHelper.getInstance().getData(server.getUserVSURL(userId), ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            Map dataMap = new ObjectMapper().readValue(responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
            UserVS userVS = UserVS.parse((Map) dataMap.get("userVS"));
            userVSMap.put(userId, userVS);
            return userVS;
        } else throw new ExceptionVS(responseVS.getMessage());
    }

    public static Map getGroupVSSubscriptionData(String groupVSURL) throws ExceptionVS, IOException {
        ResponseVS responseVS = HttpHelper.getInstance().getData(groupVSURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            Map groupDataMap = new ObjectMapper().readValue(responseVS.getMessage(), 
                    new TypeReference<HashMap<String, Object>>() {});
            Map representativeDataMap = (Map) groupDataMap.get("representative");
            //{"operation":,"groupvs":{"id":4,"name":"NombreGrupo","representative":{"id":2,"nif":"07553172H"}}}
            Map subscriptionData = new HashMap<>();
            subscriptionData.put("operation", "CURRENCY_GROUP_SUBSCRIBE");
            Map groupDataMap1 = new HashMap<>();
            groupDataMap1.put("id", groupDataMap.get("id"));
            groupDataMap1.put("name", groupDataMap.get("name"));
            HashMap representativeDataMap1 = new HashMap();
            representativeDataMap1.put("id", representativeDataMap.get("id"));
            representativeDataMap1.put("nif", representativeDataMap.get("nif"));
            groupDataMap1.put("representative", representativeDataMap1);
            subscriptionData.put("groupvs", groupDataMap1);
            return subscriptionData;
        } else throw new ExceptionVS(responseVS.getMessage());
    }

    public static void finish(String resultMessage) throws Exception {
        if(simulationData != null) simulationData.finish(ResponseVS.SC_OK, System.currentTimeMillis());
        log.info("----- finished - " + initClass.getSimpleName() + "-----");
        if(simulationData != null) log.info("Begin:" + DateUtils.getDateStr(simulationData.getBeginDate()) + " - " +
                "Duration:" + simulationData.getDurationStr());
        if(resultMessage != null) log.info(resultMessage);
        System.exit(0);
    }

    public static void finishWithError(String errorType, String resultMessage, Long numRequestCompleted) throws Exception {
        finish(errorType + ": " + resultMessage + "\n - Num. requests completed: " + numRequestCompleted);
    }
}
