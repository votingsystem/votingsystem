package org.votingsystem.signature.util;

import org.bouncycastle2.openssl.PasswordFinder;

public class DefaultPasswordFinder implements PasswordFinder{

	char[] keyPassword;
	
	public DefaultPasswordFinder(char[] keyPassword) {
		this.keyPassword = keyPassword;
	}
	
	@Override
	public char[] getPassword() {
		return keyPassword;
	}

}
