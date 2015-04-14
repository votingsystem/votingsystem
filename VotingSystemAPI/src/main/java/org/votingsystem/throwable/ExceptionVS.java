package org.votingsystem.throwable;

import org.votingsystem.dto.MessageDto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ExceptionVS extends Exception {

    private String metInf;
    private MessageDto messageDto;

    public ExceptionVS(MessageDto messageDto) {
        super(messageDto.getMessage());
        this.messageDto = messageDto;
    }

    public ExceptionVS(String message) {
        super(message);
    }

    public ExceptionVS(String message, MessageDto messageDto) {
        super(message);
        this.messageDto = messageDto;
    }

    public ExceptionVS(String message, String metaInf) {
        super(message);
        this.metInf = metaInf;
    }

    public ExceptionVS(String message, String metaInf, Throwable cause) {
        super(message, cause);
        this.metInf = metaInf;
    }

    public ExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

    public String getMetInf() {
        return metInf;
    }

    public void setMetInf(String metInf) {
        this.metInf = metInf;
    }

    public MessageDto getMessageDto() {
        return messageDto;
    }

    public void setMessageDto(MessageDto messageDto) {
        this.messageDto = messageDto;
    }
}
