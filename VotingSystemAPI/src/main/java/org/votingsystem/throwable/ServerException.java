package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class ServerException extends ExceptionVS {

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, MessageDto messageDto) {
        super(message, messageDto);
    }
}
