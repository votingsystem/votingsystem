package org.votingsystem.test.misc;


import org.apache.http.cookie.Cookie;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.net.URI;
import java.util.logging.Logger;

public class Cookies {

    private static Logger log =  Logger.getLogger(Cookies.class.getName());


    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        ResponseVS responseVS = null;
        String url = "https://192.168.1.5/CurrencyServer/rest/serverInfo";
        for(int i = 0; i < 3; i++) {
            responseVS = HttpHelper.getInstance().getData(url, ContentType.JSON);
        }
        URI uri = new URI(url);
        String domain = uri.getHost();
        log.info("domain: " + (domain.startsWith("www.") ? domain.substring(4) : domain));
        Cookie cookie = HttpHelper.getInstance().getCookie(domain);
        log.info("cookie value: " + cookie.getValue());
        log.info("sessionId: " + HttpHelper.getInstance().getSessionId(domain));
        System.exit(0);
    }


}
