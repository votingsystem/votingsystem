package org.votingsystem.throwable;

import org.votingsystem.dto.MessageDto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidationException extends ExceptionVS {

    public ValidationException(MessageDto messageDto) {
        super(messageDto);
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, String metaInf) {
        super(message, metaInf);
    }

    public ValidationException(String message, String metaInf, Throwable cause) {
        super(message, metaInf, cause);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
