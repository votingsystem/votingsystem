package org.votingsystem.signature.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.votingsystem.model.ContextVS;

import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * To bypass id_kp_timeStamping ExtendedKeyUsage exception
 */
public class SVCertExtensionChecker extends PKIXCertPathChecker {
	
	private Set<String> supportedExtensions;
	
	public SVCertExtensionChecker() {
		supportedExtensions = new HashSet<String>();
		supportedExtensions.add(X509Extensions.ExtendedKeyUsage.toString());
        supportedExtensions.add(ContextVS.HASH_CERT_VOTE_OID);
        supportedExtensions.add(ContextVS.EVENT_ID_OID);
        supportedExtensions.add(ContextVS.ACCESS_CONTROL_OID);
        supportedExtensions.add(ContextVS.REPRESENTATIVE_URL_OID);
        supportedExtensions.add(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID);
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

	public void check(Certificate cert, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
        while(unresolvedCritExts.iterator().hasNext()) {
            String ext = unresolvedCritExts.iterator().next();
            if(supportedExtensions.contains(ext)) {
                //logger.debug("------------- ExtendedKeyUsage removed from validation");
                unresolvedCritExts.remove(ext);
            }
        }
	}

}
