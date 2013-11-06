package org.sistemavotacion.seguridad;

import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.asn1.x509.X509Extensions;

public class SVCertExtensionChecker extends PKIXCertPathChecker {
	
	Set supportedExtensions;
	
	SVCertExtensionChecker() {
		supportedExtensions = new HashSet();
		supportedExtensions.add(X509Extensions.ExtendedKeyUsage);
	}
	
	public void init(boolean forward) throws CertPathValidatorException {
	 //To change body of implemented methods use File | Settings | File Templates.
    }

	public boolean isForwardCheckingSupported(){
		return true;
	}

	public Set getSupportedExtensions()	{
		return null;
	}

	public void check(Certificate cert, Collection<String> unresolvedCritExts)
			throws CertPathValidatorException {
		for(String ext : unresolvedCritExts) {
			if(X509Extensions.ExtendedKeyUsage.toString().equals(ext)) {
				//log.debug("------------- ExtendedKeyUsage removed from validation");
				unresolvedCritExts.remove(ext);
			}
		}
	}
}
