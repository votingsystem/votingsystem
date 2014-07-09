package org.votingsystem.signature.util;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class VotingSystemException extends Exception {

    public VotingSystemException(Throwable cause) {
        super(cause);
    }

    public VotingSystemException(String message) {
        super(message);
    }
}
