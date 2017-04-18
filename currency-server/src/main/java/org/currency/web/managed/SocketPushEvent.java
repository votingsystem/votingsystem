package org.currency.web.managed;

import java.util.Set;

public class SocketPushEvent {

    public enum Type {ALL_USERS, TO_USER, TO_GROUP}

    private final Type type;
    private String userUUID;
    private final String message;
    private Set<String> userSet;

    public SocketPushEvent(String message, Type type) {
        this.message = message;
        this.type = type;
    }

    public SocketPushEvent(String message, String userUUID, Type type) {
        this.message = message;
        this.userUUID = userUUID;
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

    public String getUserUUID() {
        return userUUID;
    }

    public SocketPushEvent setUserUUID(String userUUID) {
        this.userUUID = userUUID;
        return this;
    }

}
