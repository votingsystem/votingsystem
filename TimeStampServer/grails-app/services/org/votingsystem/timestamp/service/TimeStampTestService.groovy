package org.votingsystem.timestamp.service

import grails.transaction.Transactional
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil

import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

@Transactional
class TimeStampTestService {

    private static Set<X509Certificate> trustedCerts;
    private static Set<TrustAnchor> trustAnchors;
    private X509Certificate testCACert;
    def messageSource
    def timeStampService

    private Set<X509Certificate> getTrustedCerts() {
        if(trustedCerts == null) trustedCerts = new HashSet<X509Certificate>()
        return trustedCerts;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors) {
            Set<X509Certificate> trustedCerts = getTrustedCerts()
            trustAnchors = new HashSet<TrustAnchor>();
            for(X509Certificate certificate: trustedCerts) {
                TrustAnchor anchor = new TrustAnchor(certificate, null);
                trustAnchors.add(anchor);
            }
        }
        return trustAnchors;
    }

    public ResponseVS validateMessage(byte[] messageContentBytes, Locale locale) {
        SMIMEMessageWrapper messageWrapper = new SMIMEMessageWrapper(new ByteArrayInputStream(messageContentBytes));
        Set<UserVS> signersVS = messageWrapper.getSigners();
        if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertExtensionCheckerVS extensionChecker
        String signerNIF = messageWrapper.getSigner().getNif()
        for(UserVS userVS: signersVS) {
            try {
                if(userVS.getTimeStampToken() != null) {
                    ResponseVS timestampValidationResp = timeStampService.validateToken(
                            userVS.getTimeStampToken(), locale)
                    log.debug("validateSignersCertificate - timestampValidationResp - " +
                            "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                    if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                        log.error("validateSignersCertificate - TIMESTAMP ERROR - ${timestampValidationResp.message}")
                        return timestampValidationResp
                    }
                } else {
                    String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                    log.error("ERROR - validateSignersCertificate - ${msg}")
                    return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST)
                }
                ResponseVS validationResponse = CertUtil.verifyCertificate(getTrustAnchors(), false, [userVS.getCertificate()])
                X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();

                log.debug("validateSignersCertificate - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());

            } catch (CertPathValidatorException ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                        messageSource.getMessage('unknownCAErrorMsg', null, locale))
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(message:ex.getMessage(), statusCode:ResponseVS.SC_ERROR)
            }
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:messageWrapper)
    }

}