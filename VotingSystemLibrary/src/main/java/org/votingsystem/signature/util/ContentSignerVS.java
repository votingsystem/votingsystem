package org.votingsystem.signature.util;

import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSSignedData;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface ContentSignerVS {
    
    public CMSSignedData genSignedData(byte[] signatureHash, CMSAttributeTableGenerator unsAttr) throws Exception ;
    
}