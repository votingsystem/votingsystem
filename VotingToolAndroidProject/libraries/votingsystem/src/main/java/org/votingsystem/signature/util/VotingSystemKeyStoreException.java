package org.votingsystem.signature.util;

public class VotingSystemKeyStoreException extends Exception {

	private static final long serialVersionUID = 1L;

    public VotingSystemKeyStoreException(String msg) {
        super(new Exception(msg));
    }

	public VotingSystemKeyStoreException(Exception exception) {
		super(exception);
	}
	
}
