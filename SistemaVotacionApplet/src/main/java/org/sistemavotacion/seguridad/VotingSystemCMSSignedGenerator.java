package org.sistemavotacion.seguridad;

import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSSignedData;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface VotingSystemCMSSignedGenerator {
    
    public CMSSignedData genSignedData(byte[] signatureHash, 
            CMSAttributeTableGenerator unsAttr) throws Exception ;
    
}
