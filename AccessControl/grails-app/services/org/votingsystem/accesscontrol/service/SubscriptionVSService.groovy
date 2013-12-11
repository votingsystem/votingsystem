package org.votingsystem.accesscontrol.service

import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils
import org.votingsystem.model.ActorVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.DeviceVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil
import java.security.cert.X509Certificate
import grails.converters.JSON

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SubscriptionVSService {
		
	static transactional = true
    def grailsApplication
	def messageSource
	
	ResponseVS checkUser(UserVS userVS, Locale locale) {
		log.debug "checkUser - userVS.nif  '${userVS.getNif()}'"
		String msg
        CertificateVS certificate = null;
		if(!userVS.getNif()) {
			msg = messageSource.getMessage('userDataWithErrors', null, locale)
			log.error("- checkUser - ${msg}")
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
	
	ResponseVS checkDevice(String nif, String phone, String email,  String deviceId, Locale locale) {
		log.debug "checkDevice - nif:${nif} - phone:${phone} - email:${email} - deviceId:${deviceId}"
		if(!nif || !deviceId) {
			log.debug "Sin datos"
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('requestWithoutData', null, locale))
		}
		String validatedNIF = org.votingsystem.util.NifUtils.validate(nif)
		if(!validatedNIF) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('nifWithErrors', [nif].toArray(), locale))
		}
		UserVS userVS = UserVS.findWhere(nif:validatedNIF)
		if (!userVS) {
			userVS = new UserVS(nif:validatedNIF, email:email, phone:phone, type:UserVS.Type.USER).save()
		}
		DeviceVS dispositivo = DeviceVS.findWhere(deviceId:deviceId)
		if (!dispositivo || (dispositivo.userVS.id != userVS.id)) dispositivo =
                new DeviceVS(userVS:userVS, phone:phone, email:email, deviceId:deviceId).save()
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:userVS, data:dispositivo)
	}
    
	ResponseVS checkControlCenter(String serverURL, Locale locale) {
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
			ActorVS actorVS = ActorVS.populate(JSON.parse(responseVS.message))
			if (ActorVS.Type.CONTROL_CENTER != actorVS.getType()) {
				msg = messageSource.getMessage('actorNotControlCenterMsg', [actorVS.serverURL].toArray(), locale)
				log.error("checkControlCenter - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
			}
            if(!actorVS.getServerURL().equals(serverURL)) {
                msg = messageSource.getMessage('serverURLMismatch', [serverURL, actorVS.getServerURL()].toArray(), locale)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:msg)
            }
			if(!controlCenterDB) {
                controlCenterDB = new ControlCenterVS(actorVS)
                controlCenterDB.setState(ActorVS.State.RUNNING)
                ControlCenterVS.withTransaction {controlCenterDB.save()}
				log.debug("checkControlCenter - SAVED NEW CONTROL CENTER id: '${controlCenterDB.id}'")
			}
            // _ TODO _ validate control center cert
            controlCenterDB.certChainPEM = actorVS.certChainPEM
			X509Certificate x509controlCenterCert = CertUtil.fromPEMToX509CertCollection(
                    actorVS.certChainPEM.getBytes()).iterator().next()
            controlCenterCert = new CertificateVS(actorVS:controlCenterDB, certChainPEM:actorVS.certChainPEM.getBytes(),
                    content:x509controlCenterCert?.getEncoded(),state:CertificateVS.State.OK,
                    serialNumber:x509controlCenterCert?.getSerialNumber()?.longValue(),
                    validFrom:x509controlCenterCert?.getNotBefore(), type:CertificateVS.Type.ACTOR_VS,
                    validTo:x509controlCenterCert?.getNotAfter())
            controlCenterCert.setCertChainPEM(actorVS.certChainPEM.getBytes())
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


	public ResponseVS matchControlCenter(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("matchControlCenter")
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg = null;
		String serverURL = null;
		try {
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			if (!messageJSON.serverURL || !messageJSON.operation ||
				(TypeVS.CONTROL_CENTER_ASSOCIATION != TypeVS.valueOf(messageJSON.operation))) {
				msg = messageSource.getMessage('documentWithErrorsMsg', null, locale)
				log.error("matchControlCenter - ERROR -> ${smimeMessageReq.getSignedContent()}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                        type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR)
			} else {
				serverURL = StringUtils.checkURL(messageJSON.serverURL)
				ControlCenterVS controlCenter = ControlCenterVS.findWhere(serverURL:serverURL)
				if (controlCenter) {
					msg = messageSource.getMessage('controlCenterAlreadyAssociated',
                            [controlCenter.name].toArray(), locale)
					log.error("matchControlCenter - ERROR - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
						type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR)
				} else {
					ResponseVS responseVS = checkControlCenter(serverURL, locale)
					if (ResponseVS.SC_OK != responseVS.statusCode) {
						log.error("matchControlCenter- ERROR - ${responseVS.message}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
							type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR)
					} else {
						msg =  messageSource.getMessage('controlCenterAssociated',
                                [responseVS.data.controlCenterVS.name].toArray(), locale)
						log.debug(msg)
                        responseVS.type = TypeVS.CONTROL_CENTER_ASSOCIATION
						return responseVS
					}
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR,
				message: messageSource.getMessage('actorConnectionError',
					[serverURL, ex.getMessage()].toArray(), locale),)
		}
	}
}