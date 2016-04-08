package org.votingsystem.test.util;

import org.votingsystem.dto.UserDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.ResponseVS;
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


    public static UserDto getUser(Long userId, Actor server) throws Exception {
        if(userMap.get(userId) != null) return userMap.get(userId);
        UserDto dto = HttpHelper.getInstance().getData(UserDto.class, server.getUserURL(userId),
                MediaType.JSON);
        userMap.put(userId, dto);
        return dto;
    }

    public static Actor fetchServer(String serverURL) throws Exception {
        ResponseVS responseVS = ContextVS.getInstance().checkServer(serverURL);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw responseVS.getException();
        else return (Actor) responseVS.getData();
    }

    public static CurrencyServer fetchCurrencyServer(String serverURL) throws Exception {
        ResponseVS responseVS = ContextVS.getInstance().checkServer(serverURL);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw responseVS.getException();
        else return  (CurrencyServer) responseVS.getData();
    }

    public static CurrencyServer fetchCurrencyServer() throws Exception {
        return fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
    }

}
