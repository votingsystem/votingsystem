package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class NotFoundExceptionVS extends ExceptionVS {

    public NotFoundExceptionVS(String message) {
        super(message);
    }

    public NotFoundExceptionVS(String message, MessageDto messageDto) {
        super(message, messageDto);
    }

}
