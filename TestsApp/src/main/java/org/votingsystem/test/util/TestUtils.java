package org.votingsystem.test.util;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestUtils {

    private static Logger log =  Logger.getLogger(TestUtils.class.getName());

    private static ConcurrentHashMap<Long, UserVSDto> userVSMap = new ConcurrentHashMap<>();


    public static CurrencyServer fetchCurrencyServer(String currencyServerURL) throws Exception {
        if(ContextVS.getInstance().getCurrencyServer() == null) {
            ActorVSDto actorVSDto = HttpHelper.getInstance().getData(ActorVSDto.class,
                    ActorVS.getServerInfoURL(currencyServerURL), MediaTypeVS.JSON);
            ContextVS.getInstance().setCurrencyServer((CurrencyServer) actorVSDto.getActorVS());
        }
        return ContextVS.getInstance().getCurrencyServer();
    }

    public static UserVSDto getUserVS(Long userId, ActorVS server) throws Exception {
        if(userVSMap.get(userId) != null) return userVSMap.get(userId);
        UserVSDto dto = HttpHelper.getInstance().getData(UserVSDto.class, server.getUserVSURL(userId),
                MediaTypeVS.JSON);
        userVSMap.put(userId, dto);
        return dto;
    }

}
