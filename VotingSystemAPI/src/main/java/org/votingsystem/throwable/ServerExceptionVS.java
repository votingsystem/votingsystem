package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class ServerExceptionVS extends ExceptionVS {

    public ServerExceptionVS(String message) {
        super(message);
    }

    public ServerExceptionVS(String message, MessageDto messageDto) {
        super(message, messageDto);
    }
}
