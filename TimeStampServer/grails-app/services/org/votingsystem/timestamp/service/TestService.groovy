package org.votingsystem.timestamp.service

import grails.transaction.Transactional
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.throwable.ExceptionVS
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class TestService {

    private static Set<X509Certificate> trustedCerts;
    private static Set<TrustAnchor> trustAnchors;
    private X509Certificate testCACert;
    def messageSource
    def systemService

    public void addTrustedCert(X509Certificate trustedCert) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        getTrustedCerts().add(trustedCert)
        this.trustAnchors = null
        log.debug("$methodName added cert ${trustedCert.getSubjectDN()}");
    }

    private Set<X509Certificate> getTrustedCerts() {
        if(trustedCerts == null) trustedCerts = new HashSet<X509Certificate>()
        return trustedCerts;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors) {
            Set<X509Certificate> trustedCerts = getTrustedCerts()
            trustAnchors = new HashSet<TrustAnchor>(trustedCerts.size());
            for(X509Certificate certificate: trustedCerts) {
                trustAnchors.add(new TrustAnchor(certificate, null));
            }
        }
        return trustAnchors;
    }

    public ResponseVS validateMessage(InputStream inputStream) {
        SMIMEMessage smimeMessage = new SMIMEMessage(inputStream);
        smimeMessage.isValidSignature()
        Set<UserVS> signersVS = smimeMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertExtensionCheckerVS extensionChecker
        String signerNIF = smimeMessage.getSigner().getNif()
        for(UserVS userVS: signersVS) {
            try {
                systemService.validateToken(userVS.getTimeStampToken())
                CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                        getTrustAnchors(), false, [userVS.getCertificate()])
                X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
                log.debug("validateMessage - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
            } catch (CertPathValidatorException ex) {
                log.error(ex.getMessage(), ex)
                throw new ExceptionVS(messageSource.getMessage('unknownCAErrorMsg', null, locale))
            }
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:smimeMessage)
    }

}