package org.votingsystem.signature.util;

import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSSignedData;

import java.security.cert.Certificate;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public interface ContentSignerVS {
    
    public CMSSignedData genSignedData(byte[] signatureHash, CMSAttributeTableGenerator unsAttr) throws Exception ;

    public Certificate[] getCertificateChain();
    
}