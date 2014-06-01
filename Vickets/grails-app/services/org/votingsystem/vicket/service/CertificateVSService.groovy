package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.model.CertificateVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils

import java.security.cert.X509Certificate

//@Transactional
class CertificateVSService {


    public Map getCertificateVSDataMap(CertificateVS certificate) {
        ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
        X509Certificate x509Cert = CertUtil.loadCertificateFromStream (bais)
        bais.close()
        //SerialNumber as String to avoid Javascript problem handling such big numbers
        def certMap = [serialNumber:"${x509Cert.getSerialNumber()}", isRoot:CertUtil.isSelfSigned(x509Cert),
               type:certificate.type.toString(), state:certificate.state.toString(),
               subjectDN:x509Cert.getSubjectDN().toString(),
               issuerDN: x509Cert.getIssuerDN().toString(), sigAlgName:x509Cert.getSigAlgName(),
               notBefore:DateUtils.getStringFromDate(x509Cert.getNotBefore()),
               notAfter:DateUtils.getStringFromDate(x509Cert.getNotAfter())]
        return certMap
    }

    public X509Certificate getX509Cert(CertificateVS certificate) {
        ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
        X509Certificate x509Cert = CertUtil.loadCertificateFromStream (bais)
        bais.close()
        return x509Cert
    }

}
