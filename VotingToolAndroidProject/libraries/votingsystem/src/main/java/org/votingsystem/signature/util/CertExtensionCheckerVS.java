package org.votingsystem.signature.util;

import org.bouncycastle2.asn1.x509.X509Extensions;
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
public class CertExtensionCheckerVS extends PKIXCertPathChecker {

    public enum ExtensionVS {VOTE(ContextVS.VOTE_OID), REPRESENTATIVE_VOTE(ContextVS.REPRESENTATIVE_VOTE_OID),
        ANONYMOUS_REPRESENTATIVE_DELEGATION(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID),
        VICKET(ContextVS.VICKET_OID);

        String OID = null;
        ExtensionVS(String OID) { this.OID = OID; }

        public String getOID() { return OID; }

        public static ExtensionVS getExtensionVS(String extensionVS_OID) {
            if(extensionVS_OID == null) return null;
            if(ContextVS.VOTE_OID.equals(extensionVS_OID)) return VOTE;
            if(ContextVS.REPRESENTATIVE_VOTE_OID.equals(extensionVS_OID)) return REPRESENTATIVE_VOTE;
            if(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID.equals(extensionVS_OID))
                return ANONYMOUS_REPRESENTATIVE_DELEGATION;
            if(ContextVS.VICKET_OID.equals(extensionVS_OID)) return VICKET;
            return null;
        }

    }

	private Set<String> supportedExtensions;
    private Set<ExtensionVS> extensionsVS;
	
	public CertExtensionCheckerVS() {
		supportedExtensions = new HashSet<String>();
        extensionsVS = new HashSet<ExtensionVS>();
		supportedExtensions.add(X509Extensions.ExtendedKeyUsage.toString());
        supportedExtensions.add(ExtensionVS.VOTE.getOID());
        supportedExtensions.add(ExtensionVS.REPRESENTATIVE_VOTE.getOID());
        supportedExtensions.add(ExtensionVS.ANONYMOUS_REPRESENTATIVE_DELEGATION.getOID());
        supportedExtensions.add(ExtensionVS.VICKET.getOID());
	}
	
	public void init(boolean forward) throws CertPathValidatorException {
	 //To change body of implemented methods use File | Settings | File Templates.
    }

	public boolean isForwardCheckingSupported(){
		return true;
	}

    public Set<ExtensionVS> getSupportedExtensionsVS()	{
        return extensionsVS;
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
                ExtensionVS extensionVS = ExtensionVS.getExtensionVS(ext);
                if(extensionVS != null) extensionsVS.add(extensionVS);
            }
        }
	}

}