package org.votingsystem.signature.util;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingSystemException extends Exception {

    public VotingSystemException(Throwable cause) {
        super(cause);
    }

    public VotingSystemException(String message) {
        super(message);
    }
}
