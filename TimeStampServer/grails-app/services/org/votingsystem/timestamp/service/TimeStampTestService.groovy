package org.votingsystem.timestamp.service

import grails.transaction.Transactional
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil

import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate

@Transactional
class TimeStampTestService {

    static Set<X509Certificate> trustedCerts;
    private X509Certificate testCACert;
    def messageSource
    def timeStampService


    /*
     * Método para poder añadir certificados de confianza en las pruebas de carga.
     * El procedimiento para añadir una autoridad certificadora consiste en
     * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
     */
    public ResponseVS addCertificateAuthority (byte[] caPEM, Locale locale)  {
        if(!caPEM) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: "Missing cert")
        try {
            Collection<X509Certificate> certX509CertCollection = CertUtil.fromPEMToX509CertCollection(caPEM)
            for(X509Certificate cert: certX509CertCollection) {
                log.debug("addCertificateAuthority - adding cert: ${cert.getSubjectDN()}" +
                        " - serial number: ${cert.getSerialNumber()}");
            }
            getTrustedCerts().addAll(certX509CertCollection)
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Cert Authority added to TEST")
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
        }
    }

    private Set<X509Certificate> getTrustedCerts() {
        if(trustedCerts == null) trustedCerts = new HashSet<X509Certificate>()
        return trustedCerts;
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
                ResponseVS validationResponse = CertUtil.verifyCertificate(userVS.getCertificate(),
                        getTrustedCerts(), false)
                X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();

                log.debug("validateSignersCertificate - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());

            } catch (CertPathValidatorException ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                        messageSource.getMessage('unknownCAErrorMSg', null, locale))
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(message:ex.getMessage(), statusCode:ResponseVS.SC_ERROR)
            }
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:messageWrapper)
    }

}