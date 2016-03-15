package org.votingsystem.test.util;

import org.votingsystem.dto.ActorDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TestUtils {

    private static Logger log =  Logger.getLogger(TestUtils.class.getName());

    private static ConcurrentHashMap<Long, UserDto> userMap = new ConcurrentHashMap<>();


    public static CurrencyServer fetchCurrencyServer(String currencyServerURL) throws Exception {
        if(ContextVS.getInstance().getCurrencyServer() == null) {
            ActorDto actorDto = HttpHelper.getInstance().getData(ActorDto.class,
                    Actor.getServerInfoURL(currencyServerURL), MediaType.JSON);
            ContextVS.getInstance().setCurrencyServer((CurrencyServer) actorDto.getActor());
        }
        return ContextVS.getInstance().getCurrencyServer();
    }

    public static UserDto getUser(Long userId, Actor server) throws Exception {
        if(userMap.get(userId) != null) return userMap.get(userId);
        UserDto dto = HttpHelper.getInstance().getData(UserDto.class, server.getUserURL(userId),
                MediaType.JSON);
        userMap.put(userId, dto);
        return dto;
    }

}
