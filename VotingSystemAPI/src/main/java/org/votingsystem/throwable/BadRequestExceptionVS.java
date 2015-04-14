package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class BadRequestExceptionVS extends ExceptionVS {

    public BadRequestExceptionVS(String message) {
        super(message);
    }

    public BadRequestExceptionVS(String message, MessageDto messageDto) {
        super(message, messageDto);
    }
}
