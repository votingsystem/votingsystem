package org.votingsystem.client.webextension.util;

import org.votingsystem.client.webextension.dto.InboxMessageDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.TypeVS;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessage<T> {

    private static Logger log = Logger.getLogger(InboxMessage.class.getSimpleName());

    public enum State {LAPSED, PENDING, PROCESSED, REMOVED}

    private SocketMessageDto webSocketMessage;
    private TypeVS typeVS;
    private String message;
    private String from;
    private Date date;
    private State state = State.PENDING;
    private String UUID;
    private String messageID = java.util.UUID.randomUUID().toString();
    private T data;
    private Boolean isEncrypted = false;
    private Boolean isTimeLimited = false;

    public InboxMessage() { }

    public InboxMessage(String from, Date date) {
        this.from = from;
        this.date = date;
        this.UUID = java.util.UUID.randomUUID().toString();
    }

    public InboxMessage(SocketMessageDto webSocketMessage) {
        load(webSocketMessage);
    }

    public String getMessageID() {
        return messageID;
    }

    private void load(SocketMessageDto webSocketMessage) {
        this.webSocketMessage = webSocketMessage;
        this.typeVS = webSocketMessage.getOperation();
        this.message = webSocketMessage.getMessage();
        this.from = webSocketMessage.getFrom();
        this.isEncrypted = webSocketMessage.isEncrypted();
        this.isTimeLimited = webSocketMessage.isTimeLimited();
        if(webSocketMessage.getDate() == null) webSocketMessage.setDate(new Date());
        this.date = webSocketMessage.getDate();
        this.UUID = webSocketMessage.getUUID();
    }

    public InboxMessage(InboxMessageDto dto) throws ParseException {
        typeVS = dto.getTypeVS();
        typeVS = dto.getOperation();
        date = dto.getDate();
        messageID = dto.getMessageID();
        load(dto.getWebSocketMessage());
        message = dto.getMessage();
        UUID = dto.getUUID();
    }

    public Date getDate() {
        return date;
    }

    public InboxMessage setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public InboxMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public InboxMessage setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
        return this;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public State getState() {
        return state;
    }

    public InboxMessage setState(State state) {
        this.state = state;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public InboxMessage setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public boolean isTimeLimited() {
        return isTimeLimited;
    }

    public SocketMessageDto getWebSocketMessage() {
        return webSocketMessage;
    }

    public void setWebSocketMessage(SocketMessageDto webSocketMessage) {
        this.webSocketMessage = webSocketMessage;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        webSocketMessage.decryptMessage(privateKey);
        load(webSocketMessage);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


}
