package org.votingsystem.signature.smime;

import org.bouncycastle.operator.ContentSigner;
import java.security.cert.X509Certificate;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public interface DNIeContentSigner extends ContentSigner {

    public X509Certificate getUserCert();

}
