package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.vicket.util.MetaInfMsg
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import grails.transaction.Transactional

import java.nio.charset.Charset
import java.security.cert.X509Certificate

//@Transactional
class CertificateVSService {

    def userVSService
    def signatureVSService
    def messageSource
    def grailsLinkGenerator

    /*
     * Método para poder añadir certificados de confianza.
     * El procedimiento para añadir una autoridad certificadora consiste en
     * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
     */
    @Transactional
    public ResponseVS addCertificateAuthority(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        /*if(grails.util.Environment.PRODUCTION  ==  grails.util.Environment.current) {
            log.debug(" ### ADDING CERTS NOT ALLOWED IN PRODUCTION ENVIRONMENTS ###")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message: messageSource.getMessage('serviceDevelopmentModeMsg', null, locale))
        }*/
        ResponseVS responseVS = null;
        UserVS userSigner = messageSMIMEReq.getUserVS()
        String msg
        if(!userVSService.isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                         TypeVS.CERT_CA_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        if (!messageJSON.info || !messageJSON.certChainPEM ||
                (TypeVS.CERT_CA_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "params"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        Collection<X509Certificate> certX509CertCollection = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes())
        if(certX509CertCollection.isEmpty()) {
            msg = messageSource.getMessage('nullCertificateErrorMsg', null, locale)
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "nullCertificate"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        X509Certificate x509NewCACert = certX509CertCollection.iterator()?.next()
        CertificateVS certificateVS = CertificateVS.findBySerialNumber(x509NewCACert?.getSerialNumber()?.longValue())
        if(!certificateVS) {
            certificateVS = new CertificateVS(isRoot:CertUtil.isSelfSigned(x509NewCACert),
                    type:CertificateVS.Type.CERTIFICATE_AUTHORITY,
                    state:CertificateVS.State.OK,
                    description:messageJSON.info,
                    content:x509NewCACert.getEncoded(),
                    serialNumber:x509NewCACert.getSerialNumber()?.longValue(),
                    validFrom:x509NewCACert.getNotBefore(),
                    validTo:x509NewCACert.getNotAfter()).save()
            msg = messageSource.getMessage('cert.newCACertMsg', null, locale)
        } else {
            if(certificateVS.type != CertificateVS.Type.CERTIFICATE_AUTHORITY) {
                certificateVS.type = CertificateVS.Type.CERTIFICATE_AUTHORITY
                certificateVS.description = "${certificateVS.description} #### ${messageJSON.info}"
                certificateVS.save()
                msg = messageSource.getMessage('certUpdatedToCAMsg', [x509NewCACert.getSerialNumber().toString()].toArray(), locale)
            } else {
                msg = messageSource.getMessage('newCACertRepeatedErrorMsg',
                        [x509NewCACert.getSerialNumber().toString()].toArray(), locale)
                return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName,
                        "newCACertRepeated", "certificateVS_${certificateVS.id}"),
                        reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
            }
        }
        log.debug "addCertificateAuthority - new CA - id:'${certificateVS?.id}'"
        signatureVSService.loadCertAuthorities() //load changes
        String certURL = "${grailsLinkGenerator.link(controller:"certificateVS", action:"cert", absolute:true)}/${x509NewCACert.getSerialNumber().toString()}"
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CERT_CA_NEW,contentType: ContentTypeVS.JSON,
                data:[message:msg, URL:certURL, statusCode:ResponseVS.SC_OK],
                metaInf:MetaInfMsg.getOKMsg(methodName, "certificateVS_${certificateVS.id}"), message:msg)
    }

    @Transactional
    public ResponseVS editCert(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        UserVS userSigner = messageSMIMEReq.getUserVS()
        String msg
        if(userVSService.isUserAdmin()) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                         TypeVS.CERT_EDIT.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        CertificateVS.State changeCertToState = CertificateVS.State.valueOf(messageJSON.changeCertToState)
        if (!messageJSON.serialNumber ||(TypeVS.CERT_EDIT != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "params"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        CertificateVS certificateVS = CertificateVS.findWhere(state: CertificateVS.State.OK,
                serialNumber:Long.valueOf(messageJSON.serialNumber))
        if(!certificateVS) {
            msg = messageSource.getMessage('activeCertificateNotFoundErrorMsg', [messageJSON.serialNumber].toArray(), locale)
            log.error "${methodName} - ${msg}}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName,
                    "certificateVS_notFound_serialNumber_${messageJSON.serialNumber}"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        certificateVS.messageSMIME = messageSMIMEReq
        certificateVS.state = changeCertToState
        certificateVS.description = (certificateVS.description)? "${certificateVS.description}<br/>${messageJSON.reason}":messageJSON.reason
        certificateVS.save()
        signatureVSService.loadCertAuthorities() //load changes
        return new ResponseVS(type:TypeVS.CERT_EDIT, statusCode:ResponseVS.SC_OK,
                metaInf:MetaInfMsg.getOKMsg(methodName, "certificateVS_${certificateVS.id}"))
    }


    private void cancelCert(long serialNumberCert) {
        log.debug "cancelCert - serialNumberCert: ${serialNumberCert}"
        CertificateVS.withTransaction {
            CertificateVS certificate = CertificateVS.findWhere(serialNumber:serialNumberCert)
            if(certificate) {
                log.debug "cancelCert - certificateVS.id '${certificate?.id}'  --- "
                if(CertificateVS.State.OK == certificate.state) {
                    certificate.cancelDate = new Date(System.currentTimeMillis());
                    certificate.state = CertificateVS.State.CANCELLED;
                    certificate.save()
                    log.debug "cancelCert - certificateVS '${certificate?.id}' cancelled"
                } else log.debug "CertificateVS.id '${certificate?.id}' already cancelled"
            } else log.debug "CertificateVS with num. serie '${serialNumberCert}' not found"
        }
    }

    @Transactional
    public Map getCertificateVSDataMap(CertificateVS certificate) {
        X509Certificate x509Cert = CertUtil.loadCertificate (certificate.content)
        //SerialNumber as String to avoid Javascript problem handling such big numbers
        def certMap = [serialNumber:"${certificate.serialNumber}",
               isRoot:CertUtil.isSelfSigned(x509Cert),
               pemCert:new String(CertUtil.getPEMEncoded (x509Cert), "UTF-8"),
               type:certificate.type.toString(), state:certificate.state.toString(),
               subjectDN:x509Cert.getSubjectDN().toString(),
               issuerDN: x509Cert.getIssuerDN().toString(), sigAlgName:x509Cert.getSigAlgName(),
               notBefore:DateUtils.getStringFromDate(x509Cert.getNotBefore()),
               notAfter:DateUtils.getStringFromDate(x509Cert.getNotAfter())]
        if(certificate.getAuthorityCertificateVS()) certMap.issuerSerialNumber = "${certificate.getAuthorityCertificateVS()?.serialNumber}"
        return certMap
    }

}
