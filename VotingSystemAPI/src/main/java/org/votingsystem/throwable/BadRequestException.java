package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class BadRequestException extends ExceptionVS {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, MessageDto messageDto) {
        super(message, messageDto);
    }
}
