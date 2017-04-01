package org.votingsystem.okhttp3;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.*;


public final class RecordingCookieJar implements CookieJar {

    private final Deque<List<Cookie>> requestCookies = new ArrayDeque<>();
    private final Deque<List<Cookie>> responseCookies = new ArrayDeque<>();

    public void enqueueRequestCookies(Cookie... cookies) {
        requestCookies.add(Arrays.asList(cookies));
    }

    public List<Cookie> takeResponseCookies() {
        return responseCookies.removeFirst();
    }

    public void assertResponseCookies(String... cookies) {
        List<Cookie> actualCookies = takeResponseCookies();
        List<String> actualCookieStrings = new ArrayList<>();
        for (Cookie cookie : actualCookies) {
            actualCookieStrings.add(cookie.toString());
        }
    }

    @Override public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        responseCookies.add(cookies);
    }

    @Override public List<Cookie> loadForRequest(HttpUrl url) {
        if (requestCookies.isEmpty()) return Collections.emptyList();
        return requestCookies.removeFirst();
    }

}
