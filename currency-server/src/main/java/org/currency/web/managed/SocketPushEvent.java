package org.currency.web.managed;

import java.util.Set;

public class SocketPushEvent {

    public enum Type {ALL_USERS, TO_USER, TO_GROUP}

    private final Type type;
    private String sessionUUID;
    private final String message;
    private Set<String> userSet;

    public SocketPushEvent(String message, Type type) {
        this.message = message;
        this.type = type;
    }

    public SocketPushEvent(String message, String sessionUUID, Type type) {
        this.message = message;
        this.sessionUUID = sessionUUID;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public Set<String> getUserSet() {
        return userSet;
    }

    public SocketPushEvent setUserSet(Set<String> userSet) {
        this.userSet = userSet;
        return this;
    }

    public String getSessionUUID() {
        return sessionUUID;
    }

    public SocketPushEvent setSessionUUID(String sessionUUID) {
        this.sessionUUID = sessionUUID;
        return this;
    }

}
