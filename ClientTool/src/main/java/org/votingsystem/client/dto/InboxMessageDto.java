package org.votingsystem.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.util.TypeVS;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboxMessageDto {

    private TypeVS typeVS;
    private TypeVS operation;
    private String from;
    private String message;
    private String messageID;
    private SocketMessageDto webSocketMessage;
    private Date date;
    private String UUID;

    public InboxMessageDto() {}

    public InboxMessageDto(InboxMessage inboxMessage) {
        this.typeVS = inboxMessage.getTypeVS();
        this.from = inboxMessage.getFrom();
        this.message = inboxMessage.getMessage();
        this.webSocketMessage = inboxMessage.getWebSocketMessage();
        this.date = inboxMessage.getDate();
        this.UUID = inboxMessage.getUUID();
        this.messageID = inboxMessage.getMessageID();
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
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

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public SocketMessageDto getWebSocketMessage() {
        return webSocketMessage;
    }

    public void setWebSocketMessage(SocketMessageDto webSocketMessage) {
        this.webSocketMessage = webSocketMessage;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }
}
