package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.StringUtils
import static org.springframework.context.i18n.LocaleContextHolder.*
import java.security.cert.X509Certificate

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class SubscriptionVSService {

    static transactional = true
    def grailsApplication
    def messageSource
    def systemService

    ResponseVS checkUser(UserVS userVS) {
        log.debug "checkUser - userVS.nif  '${userVS.getNif()}'"
        String msg
        CertificateVS certificate = null;
        if(!userVS.getNif()) {
            msg = messageSource.getMessage('userDataWithErrors', null, locale)
            log.error("checkUser - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR)
        }
        X509Certificate x509Cert = userVS.getCertificate()
        if (!x509Cert) {
            log.debug("Missing certificate!!!")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:"Missing certificate!!!")
        }
        String validatedNIF = org.votingsystem.util.NifUtils.validate(userVS.getNif())
        if(!validatedNIF) {
            msg = messageSource.getMessage('NIFWithErrorsMsg', [userVS.getNif()].toArray(), locale)
            log.error("- checkUser - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR)
        }
        UserVS userVSDB = UserVS.findByNif(validatedNIF.toUpperCase())
        if (!userVSDB) {
            userVS.nif = validatedNIF.toUpperCase()
            userVS.type = UserVS.Type.USER
            userVS.save();
            userVSDB = userVS
            log.debug "- checkUser ### NEW USER ${userVSDB.nif} - id '${userVSDB.id}'"
            certificate = new CertificateVS(userVS:userVS, content:x509Cert.getEncoded(),
                    serialNumber:x509Cert.getSerialNumber()?.longValue(), state:CertificateVS.State.OK,
                    type:CertificateVS.Type.USER, authorityCertificate:userVS.getCertificateCA(),
                    validFrom:x509Cert.getNotBefore(), validTo:x509Cert.getNotAfter())
            certificate.save();
            log.debug "- checkUser ### NEW USER CertificateVS id '${certificate.id}'"
        } else {
            certificate = CertificateVS.findWhere(userVS:userVSDB, state:CertificateVS.State.OK)
            if (!certificate?.serialNumber == x509Cert.getSerialNumber()?.longValue()) {
                certificate.state = CertificateVS.State.CANCELLED
                certificate.save()
                log.debug "- checkUser - CANCELLED CertificateVS id '${certificate.id}'"
                certificate = new CertificateVS(userVS:userVSDB, content:x509Cert?.getEncoded(),
                        state:CertificateVS.State.OK, serialNumber:x509Cert?.getSerialNumber()?.longValue(),
                        authorityCertificate:userVS.getCertificateCA(), validFrom:x509Cert?.getNotBefore(),
                        validTo:x509Cert?.getNotAfter())
                certificate.save();
                log.debug "- checkUser - UPDATED CertificateVS id '${certificate.id}'"
            }
            userVSDB.setCertificateCA(userVS.getCertificateCA())
            userVSDB.setCertificate(userVS.getCertificate())
            userVSDB.setTimeStampToken(userVS.getTimeStampToken())
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVSDB, data:certificate)
    }

    ResponseVS checkDevice(String givenname, String surname, String nif, String phone, String email, String deviceId) {
        log.debug "checkDevice - givenname: ${givenname} - surname: ${surname} - nif:${nif} - phone:${phone} " +
                "- email:${email} - deviceId:${deviceId}"
        if(!nif || !deviceId) {
            log.debug "Missing params"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                    messageSource.getMessage('requestWithoutData', null, locale))
        }
        String validatedNIF = org.votingsystem.util.NifUtils.validate(nif)
        if(!validatedNIF) {
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                    messageSource.getMessage('nifWithErrors', [nif].toArray(), locale))
        }
        UserVS userVS = UserVS.findWhere(nif:validatedNIF)
        if (!userVS) userVS = new UserVS(nif:validatedNIF, email:email, phone:phone, type:UserVS.Type.USER,
                    name:givenname, firstName:givenname, lastName:surname).save()
        DeviceVS device = DeviceVS.findWhere(deviceId:deviceId)
        if (!device || (device.userVS.id != userVS.id)) device =
                new DeviceVS(userVS:userVS, phone:phone, email:email, deviceId:deviceId).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVS, data:device)
    }

    ResponseVS checkControlCenter(String serverURL) {
        log.debug "checkControlCenter - serverURL: ${serverURL}"
        String msg = null
        CertificateVS controlCenterCert = null
        X509Certificate x509controlCenterCertDB = null
        serverURL = StringUtils.checkURL(serverURL)
        ControlCenterVS controlCenterDB = ControlCenterVS.findWhere(serverURL:serverURL)
        if(controlCenterDB) {
            controlCenterCert = CertificateVS.findWhere(actorVS:controlCenterDB, state:CertificateVS.State.OK)
            if(controlCenterCert) return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg,
                    data:[controlCenterVS:controlCenterDB, certificateVS:controlCenterCert ])
        }
        ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL),ContentTypeVS.JSON)
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            ActorVS actorVS = ActorVS.parse(JSON.parse(responseVS.message))
            if (ActorVS.Type.CONTROL_CENTER != actorVS.getType()) {
                msg = messageSource.getMessage('actorNotControlCenterMsg', [actorVS.serverURL].toArray(), locale)
                log.error("checkControlCenter - ERROR - ${msg}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
            }
            if(!actorVS.getServerURL().equals(serverURL)) {
                msg = messageSource.getMessage('serverURLMismatch', [serverURL, actorVS.getServerURL()].toArray(), locale)
                log.debug("checkControlCenter - WARNING!!! - ${msg}")
                //return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
                actorVS.setServerURL(serverURL)
            }
            if(!controlCenterDB) {
                controlCenterDB = new ControlCenterVS(actorVS)
                controlCenterDB.setState(ActorVS.State.OK)
                ControlCenterVS.withTransaction {controlCenterDB.save()}
                log.debug("checkControlCenter - SAVED NEW CONTROL CENTER id: '${controlCenterDB.id}'")
            }
            // _ TODO _ validate control center cert
            controlCenterDB.certChainPEM = actorVS.certChainPEM
            X509Certificate x509controlCenterCert = CertUtils.fromPEMToX509CertCollection(
                    actorVS.certChainPEM.getBytes()).iterator().next()
            controlCenterCert = new CertificateVS(actorVS:controlCenterDB, certChainPEM:actorVS.certChainPEM.getBytes(),
                    content:x509controlCenterCert?.getEncoded(),state:CertificateVS.State.OK,
                    serialNumber:x509controlCenterCert?.getSerialNumber()?.longValue(),
                    validFrom:x509controlCenterCert?.getNotBefore(), type:CertificateVS.Type.ACTOR_VS,
                    validTo:x509controlCenterCert?.getNotAfter())
            controlCenterCert.save();
            log.debug("checkControlCenter - added CertificateVS  ${controlCenterCert.id}")
            controlCenterDB.x509Certificate = x509controlCenterCert
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg,
                    data:[controlCenterVS:controlCenterDB, certificateVS:controlCenterCert])
        } else {
            msg = messageSource.getMessage('controlCenterConnectionError', null, locale)
            log.error("checkControlCenter - ERROR CONECTANDO CONTROL CENTER - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
        }
    }

    public ResponseVS associateControlCenter (MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        String msg = null;
        if(!systemService.isUserAdmin(messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg',
                    [messageSMIMEReq.userVS.nif, methodName, locale].toArray())
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if (!messageJSON.serverURL || !messageJSON.operation ||
                (TypeVS.CONTROL_CENTER_ASSOCIATION != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('documentWithErrorsMsg', null, locale)
            log.error("$methodName - ERROR -> ${smimeMessageReq.getSignedContent()}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.ERROR, metaInf: MetaInfMsg.getErrorMsg(methodName, "documentWithErrors"))
        } else {
            String serverURL = StringUtils.checkURL(messageJSON.serverURL)
            ControlCenterVS controlCenter = ControlCenterVS.findWhere(serverURL:serverURL)
            if (controlCenter) {
                msg = messageSource.getMessage('controlCenterAlreadyAssociated',
                        [controlCenter.name].toArray(), locale)
                log.error("$methodName - ERROR - ${msg}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR,
                        metaInf: MetaInfMsg.getErrorMsg(methodName, "controlCenterAlreadyAssociated"))
            } else {
                ResponseVS responseVS = checkControlCenter(serverURL)
                if (ResponseVS.SC_OK != responseVS.statusCode) {
                    log.error("$methodName - ERROR - ${responseVS.message}")
                    return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
                            type:TypeVS.ERROR,  metaInf: MetaInfMsg.getErrorMsg(methodName, "checkControlCenter"))
                } else {
                    msg =  messageSource.getMessage('controlCenterAssociated',
                            [responseVS.data.controlCenterVS.name].toArray(), locale)
                    log.debug(msg)
                    responseVS.type = TypeVS.CONTROL_CENTER_ASSOCIATION
                    return responseVS
                }
            }
        }
    }

}