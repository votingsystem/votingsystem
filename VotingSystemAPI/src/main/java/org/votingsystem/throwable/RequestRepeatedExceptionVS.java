package org.votingsystem.throwable;


import org.votingsystem.dto.MessageDto;

public class RequestRepeatedExceptionVS extends ExceptionVS {

    public RequestRepeatedExceptionVS(String message) {
        super(message);
    }

    public RequestRepeatedExceptionVS(String message, MessageDto messageDto) {
        super(message, messageDto);
    }

}
