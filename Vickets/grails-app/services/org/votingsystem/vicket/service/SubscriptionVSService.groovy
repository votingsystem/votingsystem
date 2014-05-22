package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.*
import org.votingsystem.model.vicket.MetaInfMsg

import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SubscriptionVSService {
		
	static transactional = true

    def grailsApplication
	def messageSource
    def userVSService
	
	ResponseVS checkUser(UserVS userVS, Locale locale) {
		log.debug "checkUser - userVS.nif  '${userVS.getNif()}'"
		String msg
        CertificateVS certificate = null;
		if(!userVS.getNif()) {
			msg = messageSource.getMessage('userDataWithErrors', null, locale)
			log.error("checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR,
                metaInf: MetaInfMsg.user_ERROR_missingNif)
		}
		X509Certificate x509Cert = userVS.getCertificate()
		if (!x509Cert) {
			log.debug("Missing certificate!!!")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:"Missing certificate!!!",
                    metaInf: MetaInfMsg.user_ERROR_missingCert)
		}
		String validatedNIF = org.votingsystem.util.NifUtils.validate(userVS.getNif())
		if(!validatedNIF) {
			msg = messageSource.getMessage('NIFWithErrorsMsg', [userVS.getNif()].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.USER_ERROR,
                    metaInf: MetaInfMsg.user_ERROR_nif)
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
			log.debug "- checkUser ### NEW UserVS CertificateVS id '${certificate.id}'"
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
	
	ResponseVS checkDevice(String givenname, String surname, String nif, String phone, String email,
               String deviceId, Locale locale) {
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
				messageSource.getMessage('NIFWithErrorsMsg', [nif].toArray(), locale))
		}
		UserVS userVS = UserVS.findWhere(nif:validatedNIF)
		if (!userVS) {
			userVS = new UserVS(nif:validatedNIF, email:email, phone:phone, type:UserVS.Type.USER,
                    name:givenname, firstName:givenname, lastName:surname).save()
		}
		DeviceVS device = DeviceVS.findWhere(deviceId:deviceId)
		if (!device || (device.userVS.id != userVS.id)) device =
                new DeviceVS(userVS:userVS, phone:phone, email:email, deviceId:deviceId).save()
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVS, data:device)
	}

    public ResponseVS deActivateUser(MessageSMIME messageSMIMEReq, Locale locale) {
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("deActivateUser - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        log.debug("deActivateUser - documentStr: ${documentStr}")
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvs.name || !messageJSON.groupvs.id ||
                !messageJSON.uservs.name || !messageJSON.uservs.NIF ||
                (TypeVS.VICKET_GROUP_USER_DEACTIVATE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "deActivateUser - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.deActivateVicketGroupUser_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.findWhere(name:messageJSON.groupvs.name.trim(), id:Long.valueOf(messageJSON.groupvs.id))
        if(!groupVS) {
            msg = messageSource.getMessage('itemNotFoundMsg', [messageJSON.groupvs.id].toArray(), locale)
            log.error "deActivateUser - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.deActivateVicketGroupUser_ERROR_groupNotFound, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        if(!groupVS.getGroupRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !userVSService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                 TypeVS.VICKET_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), locale)
            log.error "deActivateUser - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.activateVicketGroupUser_ERROR_userWithoutPrivilege, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        UserVS userToActivate = UserVS.findWhere(nif:messageJSON.uservs.NIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)

        if(SubscriptionVS.State.CANCELLED == subscription.state) {
            msg = messageSource.getMessage('groupUserAlreadyCencelledErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), locale)
            log.error "deActivateUser - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.deActivateVicketGroupUser_ERROR_groupUserAlreadyCancelled)
        }
        subscription.setReason(messageJSON.reason)
        subscription.setState(SubscriptionVS.State.CANCELLED)
        subscription.dateCancelled = Calendar.getInstance().getTime()
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("deActivateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")
        return new ResponseVS(type:TypeVS.VICKET_GROUP_USER_DEACTIVATE, message:msg, statusCode:ResponseVS.SC_OK,
                metaInf:MetaInfMsg.deActivateVicketGroupUser_OK + subscription.id, data:subscription)
        return responseVS
    }

    public ResponseVS activateUser(MessageSMIME messageSMIMEReq, Locale locale) {
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("activateUser - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvs.name || !messageJSON.groupvs.id ||
                !messageJSON.uservs.name || !messageJSON.uservs.NIF ||
                (TypeVS.VICKET_GROUP_USER_ACTIVATE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "activateUser - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.activateVicketGroupUser_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(Long.valueOf(messageJSON.groupvs.id))
        if(!groupVS || !messageJSON.groupvs.name.equals(groupVS.name)) {
            msg = messageSource.getMessage('itemNotFoundMsg', [messageJSON.groupvs.id].toArray(), locale)
            log.error "activateUser - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.activateVicketGroupUser_ERROR_groupNotFound, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(!groupVS.getGroupRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !userVSService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                     TypeVS.VICKET_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf:MetaInfMsg.activateVicketGroupUser_ERROR_userWithoutPrivilege, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        UserVS userToActivate = UserVS.findWhere(nif:messageJSON.uservs.NIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)
        if(!userToActivate || SubscriptionVS.State.ACTIVE == subscription.state) {
            msg = messageSource.getMessage('groupUserNotPendingErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.activateVicketGroupUser_ERROR_groupUserNotPending)
        }
        subscription.setState(SubscriptionVS.State.ACTIVE)
        subscription.dateActivated = Calendar.getInstance().getTime()
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("activateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")
        return new ResponseVS(type:TypeVS.VICKET_GROUP_USER_ACTIVATE, message:msg, statusCode:ResponseVS.SC_OK,
                metaInf:MetaInfMsg.activateVicketGroupUser_OK + subscription.id, data:subscription)
    }

}