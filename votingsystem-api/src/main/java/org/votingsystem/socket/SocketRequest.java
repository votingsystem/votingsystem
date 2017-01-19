package org.votingsystem.socket;

import org.votingsystem.dto.MessageDto;

import javax.websocket.Session;
/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SocketRequest {

    private MessageDto socketMessage;
    private String body;
    private Session session;


    public SocketRequest() {}

    public SocketRequest(MessageDto socketMessage, String body) {
        this.socketMessage = socketMessage;
        this.body = body;
    }

    public MessageDto getDto() {
        return socketMessage;
    }

    public SocketRequest setDto(MessageDto socketMessage) {
        this.socketMessage = socketMessage;
        return this;
    }

    public String getBody() {
        return body;
    }

    public SocketRequest setBody(String body) {
        this.body = body;
        return this;
    }

    public Session getSession() {
        return session;
    }

    public SocketRequest setSession(Session session) {
        this.session = session;
        return this;
    }

}
