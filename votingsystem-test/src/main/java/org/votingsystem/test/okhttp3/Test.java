package org.votingsystem.test.okhttp3;

import okhttp3.*;
import org.votingsystem.testlib.BaseTest;

import java.util.List;
import java.util.logging.Logger;

public class Test extends BaseTest {

    private static final Logger log = Logger.getLogger(Test.class.getName());

    private static final String ENDPOINT = "https://voting.ddns.net/timestamp-server";

    public static void main(String[] args) throws Exception {
        new Test().testCert();
        System.exit(0);
    }

    public void testCert() throws Exception {
        RecordingCookieJar cookieJar = new RecordingCookieJar();
        OkHttpClient client = new OkHttpClient().newBuilder().cookieJar(cookieJar).build();
        Request request = new Request.Builder().url(ENDPOINT).build();
        Response response = client.newCall(request).execute();
        log.info("response: " + response.body().string());
        List<Cookie> cookies = cookieJar.takeResponseCookies();
        for(Cookie cookie : cookies) {
            log.info("response: " + cookie.domain() + " - cookie: " + cookie.name() + " - cookie: " + cookie.value());
        }
        // Deserialize HTTP response to concrete type.
        ResponseBody body = response.body();
        body.close();
        response = client.newCall(request).execute();
        log.info("response: " + response.body().string());
        cookies = cookieJar.takeResponseCookies();
        for(Cookie cookie : cookies) {
            log.info("response: " + cookie.domain() + " - cookie: " + cookie.name() + " - cookie: " + cookie.value());
        }
        body = response.body();
        body.close();
    }

}