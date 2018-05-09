package org.votingsystem.crypto;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Class holding all Java KeyStore file access logic.
 *
 */
public class SignatureTokenConnection extends AbstractSignatureTokenConnection {

	private static final Logger logger = LoggerFactory.getLogger(SignatureTokenConnection.class);


	private Certificate[] chain;
	protected PrivateKey privateKey;

	/**
	 * Creates a SignatureTokenConnection
	 */
	public SignatureTokenConnection(PrivateKey privateKey, Certificate[] chain) {
		this.privateKey = privateKey;
		this.chain = chain;
	}

	@Override
	public void close() {
	}

	/**
	 * Retrieves all the available keys (private keys entries) from the Java KeyStore.
	 *
	 * @return
	 * @throws DSSException
	 */
	@Override
	public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
		return Arrays.asList(new KSPrivateKeyEntry("PrivateKey", new PrivateKeyEntry(privateKey, chain)));
	}
}
